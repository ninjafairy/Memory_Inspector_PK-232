package protocol;

import serial.SerialLink;
import util.PacketLogger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Drives the PK-232 from the {@code cmd:} prompt into Host Mode per
 * truenorth §5.5. Must be called immediately after
 * {@link StartupDetector#detect()} returns — the detector's post-condition
 * (locked Q6) guarantees the modem is parked at {@code cmd:} regardless of
 * which branch fired, so the {@code startingState} argument is informational
 * for logging only.
 *
 * <h2>Wire sequence</h2>
 * <ol>
 *   <li>Verbose-settings stage (at {@code cmd:}, each terminated with a bare
 *       {@code CR}, waiting for the next {@code cmd:} within
 *       {@value #VERBOSE_ACK_TIMEOUT_MS} ms, accumulator reset between steps):
 *       <pre>AWLEN 8&lt;CR&gt;
 *PARITY 0&lt;CR&gt;
 *8BITCONV ON&lt;CR&gt;</pre></li>
 *   <li>{@code RESTART<CR>} — required per {@code hCmd.md} (AWLEN / PARITY /
 *       8BITCONV do not take effect until RESTART is performed; see
 *       {@code hCmd.md} §AWLEN and §PARITY notes). The modem soft-reboots
 *       while retaining user settings. Drain RX for
 *       {@value #RESTART_SETTLE_MS} ms accumulating into a buffer, then
 *       verify {@code cmd:} appears as a substring in the captured stream.</li>
 *   <li>Resilient HOST-ON frame per {@code hCmd.md} §4.1.3:
 *       {@code 11 18 03 48 4F 53 54 20 59 0D} (XON + CAN + ^C + "HOST Y" + CR).
 *       No ack wait — modem is transitioning to host mode on this write.</li>
 *   <li>OGG probe {@code 01 4F 47 47 17}; parse RX via
 *       {@link HostCodec.Parser}; accept the first block with
 *       {@code ctl == 0x4F} and payload {@code [0x47 0x47 0x00]} within
 *       {@value #PROBE_TIMEOUT_MS} ms.</li>
 *   <li>On miss, one retry with the double-SOH form
 *       {@code 01 01 4F 47 47 17}. Fail on second miss with
 *       {@link HostModeEntryException}.</li>
 * </ol>
 *
 * <p><b>{@code RESET} is never sent</b> (truenorth E2 — destructive: wipes
 * MYCALL, MailDrop messages, monitor lists, stored baud rates to PROM
 * defaults). {@code RESTART}, which this class DOES send, is an entirely
 * different immediate command (non-destructive soft reboot). On entry
 * failure the caller ({@code Main}) surfaces a Retry/Cancel dialog per
 * truenorth §5.11.
 *
 * <h2>Shutdown</h2>
 * {@link #tryExit(SerialLink, PacketLogger)} is a best-effort helper called
 * from both the {@code MainFrame} close handler and the JVM shutdown hook.
 * It sends the double-SOH {@code HO N} frame (Q6), waits up to
 * {@value #EXIT_ACK_TIMEOUT_MS} ms for the ack, and falls back to a
 * {@value #EXIT_BREAK_MS} ms BREAK on write-error or ack-timeout. Total
 * wall-clock capped at {@value #EXIT_TOTAL_CAP_MS} ms so JVM exit never
 * stalls.
 */
public final class HostModeEntry {

    /** Thrown when Host Mode entry cannot be completed. */
    public static final class HostModeEntryException extends Exception {
        public HostModeEntryException(String message) { super(message); }
        public HostModeEntryException(String message, Throwable cause) { super(message, cause); }
    }

    /** Per-verbose-command {@code cmd:} substring window (Q2). */
    public static final int VERBOSE_ACK_TIMEOUT_MS = 500;
    /**
     * Post-{@code RESTART} RX drain window — user-specified 2000 ms per the
     * {@code hCmd.md §RESTART} "equivalent to power-cycle" contract. The
     * modem soft-reboots, emits a register dump / banner, and re-prints
     * {@code cmd:} within this window.
     */
    public static final int RESTART_SETTLE_MS = 2000;
    /** OGG probe response window (Q4). */
    public static final int PROBE_TIMEOUT_MS = 750;
    /** Short read block while polling — must divide cleanly into the match windows. */
    public static final int POLL_READ_TIMEOUT_MS = 100;
    /** Settle gap between {@code HOST Y<CR>} and the first OGG probe. */
    public static final int POST_HOST_ON_SETTLE_MS = 50;

    /** Ack window for the shutdown {@code HO N} frame (Q9). */
    public static final int EXIT_ACK_TIMEOUT_MS = 500;
    /** BREAK duration asserted when {@code HO N} fails (truenorth §5.11). */
    public static final int EXIT_BREAK_MS = 300;
    /** Hard wall-clock cap on the entire shutdown path (Q9). */
    public static final int EXIT_TOTAL_CAP_MS = 1500;

    // Verbose-command payloads (CR-terminated, Q1).
    private static final byte[] TX_AWLEN    = asciiCr("AWLEN 8");
    private static final byte[] TX_PARITY   = asciiCr("PARITY 0");
    private static final byte[] TX_8BITCONV = asciiCr("8BITCONV ON");
    /**
     * Soft-reboot command — required per {@code hCmd.md} for the three
     * verbose settings above to take effect. Do NOT confuse with the
     * destructive {@code RESET} command (truenorth E2).
     */
    private static final byte[] TX_RESTART  = asciiCr("RESTART");

    /**
     * Resilient HOST-ON frame per {@code hCmd.md} §4.1.3: XON (0x11), CAN
     * (0x18), ^C (0x03) to cancel any residual line state, then the literal
     * string {@code "HOST Y"} + {@code CR}. Chosen over plain {@code HOST ON<CR>}
     * per Q8 so we're belt-and-suspenders even if the previous verbose step
     * left noise on the wire.
     */
    private static final byte[] TX_HOST_Y_RESILIENT = {
            0x11, 0x18, 0x03,
            0x48, 0x4F, 0x53, 0x54, 0x20, 0x59,
            0x0D
    };

    /** Single-SOH OGG probe {@code 01 4F 47 47 17}. */
    private static final byte[] TX_OGG_PROBE =
            {0x01, 0x4F, 0x47, 0x47, 0x17};
    /** Double-SOH OGG probe retry {@code 01 01 4F 47 47 17}. */
    private static final byte[] TX_OGG_RETRY =
            {0x01, 0x01, 0x4F, 0x47, 0x47, 0x17};

    /** Double-SOH {@code HO N} frame used on shutdown (Q6). */
    private static final byte[] TX_HO_N_EXIT =
            {0x01, 0x01, 0x4F, 0x48, 0x4F, 0x4E, 0x17};

    /** Unescaped OGG ack payload bytes: {@code 47 47 00}. */
    private static final byte[] OGG_ACK_PAYLOAD =
            {0x47, 0x47, 0x00};
    /** Unescaped {@code HO N} ack payload bytes: {@code 48 4F 00}. */
    private static final byte[] HO_N_ACK_PAYLOAD =
            {0x48, 0x4F, 0x00};

    private final SerialLink link;
    private final PacketLogger log;
    private final StartupDetector.ModemState startingState;

    public HostModeEntry(SerialLink link, PacketLogger log,
                         StartupDetector.ModemState startingState) {
        this.link = Objects.requireNonNull(link, "link");
        this.log = Objects.requireNonNull(log, "log");
        this.startingState = Objects.requireNonNull(startingState, "startingState");
    }

    /**
     * Full Cmd→Hostmode transition. The caller's read-timeout on the serial
     * link is saved and restored around the entire call so this is safe to
     * sandwich between other protocol code.
     */
    public void enter() throws IOException, HostModeEntryException {
        if (!link.isOpen()) {
            throw new IOException("serial link is not open");
        }
        log.logRaw("host-mode entry: begin (starting state " + startingState + ")");

        int savedReadTimeout = link.getReadTimeoutMs();
        link.setReadTimeoutMs(POLL_READ_TIMEOUT_MS);
        try {
            sendVerbose("AWL", TX_AWLEN);
            sendVerbose("PAR", TX_PARITY);
            sendVerbose("8BC", TX_8BITCONV);

            restart();

            writeAndLog("HYR", TX_HOST_Y_RESILIENT);
            sleepQuietly(POST_HOST_ON_SETTLE_MS);

            if (!probeOnce("OGG", TX_OGG_PROBE)) {
                log.logRaw("host-mode entry: OGG probe miss, retrying double-SOH");
                if (!probeOnce("OGR", TX_OGG_RETRY)) {
                    throw new HostModeEntryException(
                            "OGG probe failed after double-SOH retry");
                }
            }
            log.logRaw("host-mode entry: resolved (HOSTMODE ready)");
        } finally {
            link.setReadTimeoutMs(savedReadTimeout);
        }
    }

    /** Send one verbose command and wait for {@code cmd:}; throw on miss. */
    private void sendVerbose(String mnemonic, byte[] frame)
            throws IOException, HostModeEntryException {
        writeAndLog(mnemonic, frame);
        if (!awaitSubstring("cmd:", VERBOSE_ACK_TIMEOUT_MS)) {
            throw new HostModeEntryException(
                    "no `cmd:` ack after " + mnemonic + " within "
                            + VERBOSE_ACK_TIMEOUT_MS + " ms");
        }
    }

    /**
     * Send {@code RESTART<CR>} and wait {@link #RESTART_SETTLE_MS} for the
     * modem to finish its soft reboot. All bytes received during the settle
     * window are logged as {@code RX RST} (the reset banner / register dump)
     * and accumulated into a buffer so we can confirm the modem returned to
     * the {@code cmd:} prompt. Throws {@link HostModeEntryException} if
     * {@code cmd:} is not observed.
     *
     * <p>Critically: this is {@code RESTART}, NOT {@code RESET}. The latter
     * is destructive and forbidden per E2.
     */
    private void restart() throws IOException, HostModeEntryException {
        writeAndLog("RST", TX_RESTART);
        StringBuilder sb = new StringBuilder(256);
        byte[] buf = new byte[256];
        long deadline = System.nanoTime() + msToNanos(RESTART_SETTLE_MS);
        try (PacketLogger.RxLineBuffer rx = log.streamRx("RST")) {
            while (System.nanoTime() < deadline) {
                int n = link.read(buf, 0, buf.length);
                if (n > 0) {
                    rx.feed(buf, 0, n);
                    for (int i = 0; i < n; i++) {
                        sb.append((char) (buf[i] & 0xFF));
                    }
                }
            }
        }
        if (sb.indexOf("cmd:") < 0) {
            throw new HostModeEntryException(
                    "no `cmd:` observed in " + RESTART_SETTLE_MS
                            + " ms post-RESTART window");
        }
    }

    /**
     * Send {@code frame}, then feed RX bytes into a fresh
     * {@link HostCodec.Parser} for up to {@link #PROBE_TIMEOUT_MS} until a
     * block matching the OGG ack shape arrives. Non-matching blocks are
     * logged and discarded — hardware occasionally emits a leading status
     * frame on host-mode entry even though the spec suggests silence.
     */
    private boolean probeOnce(String mnemonic, byte[] frame) throws IOException {
        writeAndLog(mnemonic, frame);
        HostCodec.Parser parser = HostCodec.newParser();
        byte[] buf = new byte[256];
        long deadline = System.nanoTime() + msToNanos(PROBE_TIMEOUT_MS);

        while (System.nanoTime() < deadline) {
            int n = link.read(buf, 0, buf.length);
            if (n <= 0) {
                continue;
            }
            List<HostBlock> blocks = parser.feed(buf, 0, n);
            for (HostBlock block : blocks) {
                byte[] framed = HostCodec.encode(block.ctl(), block.payloadCopy());
                log.log(PacketLogger.Direction.RX, "OGG", framed, 0, framed.length);
                if (block.isGlobalCommand()
                        && block.payloadEquals(OGG_ACK_PAYLOAD)) {
                    return true;
                }
                log.logRaw("host-mode entry: ignored pre-ack block " + block);
            }
        }
        return false;
    }

    /**
     * Accumulates RX bytes into a String buffer (reset per call — stage
     * isolation, same discipline as {@link StartupDetector}) and returns
     * {@code true} as soon as {@code needle} appears, or {@code false} after
     * {@code timeoutMs}.
     */
    private boolean awaitSubstring(String needle, int timeoutMs) throws IOException {
        long deadline = System.nanoTime() + msToNanos(timeoutMs);
        StringBuilder sb = new StringBuilder(64);
        byte[] buf = new byte[256];
        try (PacketLogger.RxLineBuffer rx = log.streamRx("CMD")) {
            while (System.nanoTime() < deadline) {
                int n = link.read(buf, 0, buf.length);
                if (n > 0) {
                    rx.feed(buf, 0, n);
                    for (int i = 0; i < n; i++) {
                        sb.append((char) (buf[i] & 0xFF));
                    }
                    if (sb.indexOf(needle) >= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void writeAndLog(String mnemonic, byte[] bytes) throws IOException {
        log.logTx(mnemonic, bytes);
        link.write(bytes);
    }

    /**
     * Best-effort exit from Host Mode. Safe to call from any thread, from
     * both the MainFrame close handler and the JVM shutdown hook; idempotency
     * at the application level is provided by {@code Main.runShutdown}'s
     * {@link java.util.concurrent.atomic.AtomicBoolean} guard.
     *
     * <p>Strategy (truenorth §5.11, Q6/Q9):
     * <ol>
     *   <li>Write double-SOH {@code HO N} frame {@code 01 01 4F 48 4F 4E 17}.</li>
     *   <li>Wait up to {@value #EXIT_ACK_TIMEOUT_MS} ms for the ack block
     *       {@code 01 4F 48 4F 00 17}. Success is logged as {@code RX HOA}.</li>
     *   <li>On write failure OR ack timeout, assert BREAK for
     *       {@value #EXIT_BREAK_MS} ms (logged via {@code logRaw}).</li>
     *   <li>The whole path is capped at {@value #EXIT_TOTAL_CAP_MS} ms so
     *       the JVM never hangs on exit even if the modem is unplugged.</li>
     * </ol>
     */
    public static void tryExit(SerialLink link, PacketLogger log) {
        Objects.requireNonNull(link, "link");
        Objects.requireNonNull(log, "log");
        if (!link.isOpen()) {
            log.logRaw("shutdown: serial link already closed, skipping HO N");
            return;
        }

        long startNanos = System.nanoTime();
        long wallCapNanos = msToNanos(EXIT_TOTAL_CAP_MS);

        int savedReadTimeout;
        try {
            savedReadTimeout = link.getReadTimeoutMs();
        } catch (RuntimeException re) {
            log.logRaw("shutdown: could not read read-timeout (" + re + "), best-effort BREAK only");
            tryBreak(link, log);
            return;
        }

        try {
            link.setReadTimeoutMs(POLL_READ_TIMEOUT_MS);
        } catch (RuntimeException re) {
            log.logRaw("shutdown: could not set poll timeout (" + re + "), best-effort BREAK only");
            tryBreak(link, log);
            return;
        }

        boolean ackSeen = false;
        try {
            log.logTx("HOX", TX_HO_N_EXIT);
            link.write(TX_HO_N_EXIT);
            ackSeen = awaitExitAck(link, log, startNanos, wallCapNanos);
        } catch (IOException ioe) {
            log.logRaw("shutdown: HO N write failed (" + ioe.getMessage() + ")");
        } catch (RuntimeException re) {
            log.logRaw("shutdown: HO N write crashed (" + re + ")");
        } finally {
            try {
                link.setReadTimeoutMs(savedReadTimeout);
            } catch (RuntimeException ignore) {
                // We're on the shutdown path; nothing useful to do here.
            }
        }

        if (!ackSeen) {
            long remaining = wallCapNanos - (System.nanoTime() - startNanos);
            if (remaining <= 0) {
                log.logRaw("shutdown: HO N timed out and wall-cap hit before BREAK fallback");
                return;
            }
            tryBreak(link, log);
        }
    }

    private static boolean awaitExitAck(SerialLink link, PacketLogger log,
                                        long startNanos, long wallCapNanos) throws IOException {
        HostCodec.Parser parser = HostCodec.newParser();
        byte[] buf = new byte[128];
        long ackDeadline = startNanos + msToNanos(EXIT_ACK_TIMEOUT_MS);
        long wallDeadline = startNanos + wallCapNanos;
        long deadline = Math.min(ackDeadline, wallDeadline);

        while (System.nanoTime() < deadline) {
            int n = link.read(buf, 0, buf.length);
            if (n <= 0) {
                continue;
            }
            List<HostBlock> blocks = parser.feed(buf, 0, n);
            for (HostBlock block : blocks) {
                byte[] framed = HostCodec.encode(block.ctl(), block.payloadCopy());
                log.log(PacketLogger.Direction.RX, "HOA", framed, 0, framed.length);
                if (block.isGlobalCommand()
                        && block.payloadEquals(HO_N_ACK_PAYLOAD)) {
                    return true;
                }
            }
        }
        log.logRaw("shutdown: HO N ack not seen within " + EXIT_ACK_TIMEOUT_MS + " ms");
        return false;
    }

    private static void tryBreak(SerialLink link, PacketLogger log) {
        log.logRaw("shutdown: asserting " + EXIT_BREAK_MS + " ms BREAK as HO N fallback");
        try {
            link.sendBreak(EXIT_BREAK_MS);
        } catch (IOException ioe) {
            log.logRaw("shutdown: BREAK failed (" + ioe.getMessage() + ")");
        } catch (RuntimeException re) {
            log.logRaw("shutdown: BREAK crashed (" + re + ")");
        }
    }

    private static byte[] asciiCr(String s) {
        byte[] out = new byte[s.length() + 1];
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0x7F) {
                throw new IllegalArgumentException("non-ASCII char in verbose command: "
                        + s);
            }
            out[i] = (byte) c;
        }
        out[out.length - 1] = 0x0D;
        return out;
    }

    private static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static long msToNanos(int ms) {
        return ms * 1_000_000L;
    }
}
