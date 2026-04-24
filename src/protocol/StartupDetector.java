package protocol;

import serial.SerialLink;
import util.PacketLogger;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Authoritative startup-state detector per the {@code logic} file (truenorth
 * E4). Runs a four-stage ladder at whatever baud the caller has already
 * opened the serial link at:
 *
 * <ol>
 *   <li><b>COMMAND</b> — send {@code CR}, wait 250 ms, send {@code CR}; if
 *       {@code "cmd:"} appears within the per-step window, state is COMMAND.</li>
 *   <li><b>AUTOBAUD</b> — send {@code "*"}; if {@code "PK-232"} appears
 *       anywhere in the response within the per-step window, state is
 *       AUTOBAUD (the PK-232 banner terminates at {@code "cmd:"}).</li>
 *   <li><b>HOSTMODE</b> — send the double-SOH {@code HO N} frame
 *       {@code 01 01 4F 48 4F 4E 17} followed by {@code CR}; if {@code "cmd:"}
 *       appears (the probe landed on a host-mode parser, which then left host
 *       mode), state is HOSTMODE.</li>
 *   <li><b>OFF</b> — prompt the user to power-cycle the PK-232 (via the
 *       injected {@link BooleanSupplier}), give the unit up to 5000 ms to
 *       come up, then re-run the first three stages exactly once. If that
 *       re-run also falls through, throw {@link DetectionException}.</li>
 * </ol>
 *
 * <p>The detector is intentionally UI-free — {@link BooleanSupplier}
 * decouples the power-cycle prompt from Swing so this class stays usable
 * from any thread and from headless test harnesses.
 *
 * <p><b>Post-condition:</b> on a successful return, the modem is parked at
 * the {@code cmd:} prompt regardless of which branch fired (locked Q6). Any
 * residual banner bytes are drained before {@link #detect()} returns so the
 * next caller on the serial link starts with a clean RX buffer.
 */
public final class StartupDetector {

    public enum ModemState { COMMAND, AUTOBAUD, HOSTMODE }

    /** Thrown when detection cannot bring the modem to a known state. */
    public static final class DetectionException extends Exception {
        public DetectionException(String message) { super(message); }
    }

    /** Per-step match window (locked Q1/Q5). */
    public static final int PER_STEP_TIMEOUT_MS = 750;
    /** Pause between the two returns in the COMMAND stage. */
    public static final int INTER_CR_PAUSE_MS = 250;
    /** Short read block used while polling — must divide cleanly into the per-step window. */
    public static final int POLL_READ_TIMEOUT_MS = 100;
    /** Total window given to the user to power the unit on (locked Q4). */
    public static final int POWER_ON_WINDOW_MS = 5000;
    /** After bytes have started arriving, idle this long to declare the banner done. */
    public static final int POWER_ON_QUIET_GAP_MS = 300;
    /** Post-detection drain window — clears any banner tail for the next caller. */
    public static final int POST_DETECT_DRAIN_MS = 150;
    /**
     * AUTOBAUD phase-2: once {@code PK-232} has been seen, keep reading
     * until the banner's terminal {@code cmd:} appears or this window
     * elapses. Without this, the next stage would send {@code AWLEN} while
     * banner bytes — including the banner's own {@code cmd:} — were still
     * streaming in, causing the verbose-ack window to false-match on
     * banner residue. Sized for a ~200-byte banner at 9600 baud plus
     * generous modem-processing headroom (see §8 Change Log 2026-04-20).
     */
    public static final int AUTOBAUD_BANNER_SETTLE_MS = 3000;

    private static final byte[] CR        = {0x0D};
    private static final byte[] STAR      = {0x2A};
    /** Double-SOH {@code HO N} frame per {@code logic} line 14 / §Recovery behavior. */
    private static final byte[] HOSTMODE_PROBE =
            {0x01, 0x01, 0x4F, 0x48, 0x4F, 0x4E, 0x17};

    private final SerialLink         link;
    private final PacketLogger       log;
    private final BooleanSupplier    powerCyclePrompt;

    public StartupDetector(SerialLink link, PacketLogger log, BooleanSupplier powerCyclePrompt) {
        this.link             = Objects.requireNonNull(link, "link");
        this.log              = Objects.requireNonNull(log, "log");
        this.powerCyclePrompt = Objects.requireNonNull(powerCyclePrompt, "powerCyclePrompt");
    }

    /**
     * Runs the full detection ladder. Returns the detected state on success
     * (modem guaranteed parked at {@code cmd:}). The caller's read-timeout
     * on the serial link is saved and restored around the call so it is
     * safe to sandwich this between other protocol code.
     */
    public ModemState detect() throws IOException, DetectionException {
        if (!link.isOpen()) {
            throw new IOException("serial link is not open");
        }

        int savedReadTimeout = link.getReadTimeoutMs();
        link.setReadTimeoutMs(POLL_READ_TIMEOUT_MS);
        try {
            log.logRaw("startup detect: begin (first pass)");
            ModemState s = runLadder();
            if (s != null) {
                drainResidual();
                log.logRaw("startup detect: resolved as " + s);
                return s;
            }

            log.logRaw("startup detect: OFF branch (prompting for power-cycle)");
            if (!powerCyclePrompt.getAsBoolean()) {
                throw new DetectionException("user cancelled power-cycle prompt");
            }
            waitForPowerOnActivity();
            log.logRaw("startup detect: begin (post power-cycle re-run)");

            s = runLadder();
            if (s != null) {
                drainResidual();
                log.logRaw("startup detect: resolved as " + s + " (after power-cycle)");
                return s;
            }
            throw new DetectionException("modem unresponsive after power-cycle");
        } finally {
            link.setReadTimeoutMs(savedReadTimeout);
        }
    }

    /** One pass through COMMAND -> AUTOBAUD -> HOSTMODE. Returns null on fall-through. */
    private ModemState runLadder() throws IOException {
        if (tryCommand())  return ModemState.COMMAND;
        if (tryAutobaud()) return ModemState.AUTOBAUD;
        if (tryHostmode()) return ModemState.HOSTMODE;
        return null;
    }

    private boolean tryCommand() throws IOException {
        writeAndLog("CR", CR);
        sleepQuietly(INTER_CR_PAUSE_MS);
        writeAndLog("CR", CR);
        return awaitSubstring("cmd:", PER_STEP_TIMEOUT_MS);
    }

    private boolean tryAutobaud() throws IOException {
        writeAndLog("ST", STAR);
        // Two-phase match (see §8 Change Log 2026-04-20):
        //  Phase 1 — wait PER_STEP_TIMEOUT_MS for "PK-232" substring (Q3:
        //            case-sensitive, substring, "PK-232M" still satisfies).
        //  Phase 2 — same accumulator, same RX DET log stream, keep reading
        //            until "cmd:" appears so the whole banner is drained
        //            before we hand off to the next stage.
        //
        // Dual deadlines keep the two windows independent: phase-1 starts
        // from right-after-ST-send; phase-2 starts from the moment PK-232
        // is first seen. On phase-2 timeout we fall through to the next
        // ladder stage rather than reporting AUTOBAUD with banner residue
        // still in flight — safer for HostModeEntry downstream.
        long phase1Deadline = System.nanoTime() + msToNanos(PER_STEP_TIMEOUT_MS);
        long phase2Deadline = 0L;
        StringBuilder sb = new StringBuilder(256);
        byte[] buf = new byte[256];
        boolean bannerStarted = false;
        try (PacketLogger.RxLineBuffer rx = log.streamRx("DET")) {
            while (true) {
                long deadline = bannerStarted ? phase2Deadline : phase1Deadline;
                if (System.nanoTime() >= deadline) {
                    if (bannerStarted) {
                        log.logRaw("autobaud: PK-232 seen but no `cmd:` within "
                                + AUTOBAUD_BANNER_SETTLE_MS + " ms — falling through");
                    }
                    return false;
                }
                int n = link.read(buf, 0, buf.length);
                if (n > 0) {
                    rx.feed(buf, 0, n);
                    for (int i = 0; i < n; i++) {
                        sb.append((char) (buf[i] & 0xFF));
                    }
                    if (!bannerStarted && sb.indexOf("PK-232") >= 0) {
                        bannerStarted = true;
                        phase2Deadline = System.nanoTime() + msToNanos(AUTOBAUD_BANNER_SETTLE_MS);
                    }
                    if (bannerStarted && sb.indexOf("cmd:") >= 0) {
                        return true;
                    }
                }
            }
        }
    }

    private boolean tryHostmode() throws IOException {
        writeAndLog("HPB", HOSTMODE_PROBE);
        writeAndLog("CR",  CR);
        return awaitSubstring("cmd:", PER_STEP_TIMEOUT_MS);
    }

    /**
     * Waits up to {@link #POWER_ON_WINDOW_MS} for bytes to arrive, then
     * (if they did) until a {@link #POWER_ON_QUIET_GAP_MS} idle gap. The
     * quiet-gap path exits early as soon as the banner settles; the no-data
     * path holds for the full window in case the user was slow to hit the
     * switch.
     */
    private void waitForPowerOnActivity() throws IOException {
        long deadline     = System.nanoTime() + msToNanos(POWER_ON_WINDOW_MS);
        long lastByteTime = 0L;
        byte[] buf = new byte[256];

        try (PacketLogger.RxLineBuffer rx = log.streamRx("BAN")) {
            while (System.nanoTime() < deadline) {
                int n = link.read(buf, 0, buf.length);
                if (n > 0) {
                    rx.feed(buf, 0, n);
                    lastByteTime = System.nanoTime();
                } else if (lastByteTime != 0L
                        && System.nanoTime() - lastByteTime >= msToNanos(POWER_ON_QUIET_GAP_MS)) {
                    return;
                }
            }
        }
    }

    /**
     * Accumulates RX bytes into a String buffer and returns true as soon as
     * {@code needle} appears, or false after {@code timeoutMs}. The buffer
     * is always fresh per call — stage isolation (see class doc).
     */
    private boolean awaitSubstring(String needle, int timeoutMs) throws IOException {
        long deadline = System.nanoTime() + msToNanos(timeoutMs);
        StringBuilder sb = new StringBuilder(128);
        byte[] buf = new byte[256];
        try (PacketLogger.RxLineBuffer rx = log.streamRx("DET")) {
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

    /**
     * After a successful match, the modem often has a few more bytes queued
     * (e.g. CR/LF following {@code cmd:}). Pull them off the wire so the
     * next protocol step doesn't start with stale data. Best-effort; any
     * bytes still in flight after the drain window stay in the OS buffer.
     */
    private void drainResidual() throws IOException {
        long deadline = System.nanoTime() + msToNanos(POST_DETECT_DRAIN_MS);
        byte[] buf = new byte[256];
        try (PacketLogger.RxLineBuffer rx = log.streamRx("DRN")) {
            while (System.nanoTime() < deadline) {
                int n = link.read(buf, 0, buf.length);
                if (n > 0) {
                    rx.feed(buf, 0, n);
                }
            }
        }
    }

    private void writeAndLog(String mnemonic, byte[] bytes) throws IOException {
        log.logTx(mnemonic, bytes);
        link.write(bytes);
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
