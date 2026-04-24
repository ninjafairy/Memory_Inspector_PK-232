package protocol;

import serial.SerialLink;
import util.HexUtils;
import util.PacketLogger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 2-thread framed client for the PK-232 in Host Mode, per truenorth §5.2 +
 * §5.3 + §5.6 and the M3c Step-C lock (§8 Change Log 2026-04-20).
 *
 * <h2>Threading model</h2>
 * <ul>
 *   <li>A <b>Serial Read Thread</b> ({@code PK232-Reader}) does blocking
 *       {@link SerialLink#read(byte[], int, int)} with a short poll timeout,
 *       feeds raw bytes into a single {@link HostCodec.Parser}, and enqueues
 *       emitted {@link HostBlock}s into a {@link LinkedBlockingQueue}.</li>
 *   <li>A <b>Protocol Thread</b> ({@code PK232-Protocol}) drains the queue.
 *       Each block is either correlated to the single pending
 *       {@link #sendAndAwait(HostBlock, long)} request (if it is a
 *       {@code 0x4F} block and a request is pending) or routed to the
 *       user-supplied {@link Consumer} unsolicited handler.</li>
 *   <li>Callers live on application threads — usually the EDT proxy in
 *       {@code DumpController} (arrives in M4). {@code sendAndAwait} blocks
 *       those callers; never call it from the EDT.</li>
 * </ul>
 *
 * <h2>Correlation contract</h2>
 * A fair {@link ReentrantLock} serializes {@code sendAndAwait} — at most one
 * outstanding command at a time (truenorth A3, depth = 1). Pending state is
 * stored in a single volatile slot and handed off via a
 * {@link CountDownLatch}. Timeout throws {@link ProtocolTimeoutException}
 * (an {@link IOException} subtype). Per §5.6 a timeout is terminal for the
 * current operation; the caller is expected to abort whatever it was doing
 * (no automatic retry inside the client).
 *
 * <h2>Lifecycle</h2>
 * Construct → {@link #start()} after {@code HostModeEntry.enter()} succeeds
 * → use → {@link #close()} BEFORE {@code HostModeEntry.tryExit(link, log)}
 * in the shutdown path (§5.11 ordering — the bare
 * {@link SerialLink#read(byte[], int, int)} inside {@code tryExit} must not
 * race this client's reader).
 *
 * <h2>Logging</h2>
 * TX (via {@code sendAndAwait}) is logged as {@code TX <mnem>} with the
 * mnemonic derived from the first two ASCII-alpha payload bytes when
 * available, else {@code CMD}. Correlated RX responses are logged as
 * {@code RX <mnem>} by the Protocol Thread. Non-correlated blocks log as
 * {@code RX UNS} (generic unsolicited) or {@code RX ERR}
 * ({@code 0x5F} status/error). Client lifecycle events go through
 * {@link PacketLogger#logRaw(String)}.
 *
 * <h2>Testing policy</h2>
 * Option-3 strict (truenorth §10 + §8 2026-04-20): this class is not
 * unit-tested. Hardware smoke during M3c confirms threads launch/stop
 * cleanly; M4's first real {@code AE} + {@code MM} round-trip exercises
 * correlation + timeout + unsolicited routing on real hardware.
 */
public final class PK232Client implements AutoCloseable {

    /** Thrown when {@code sendAndAwait} does not see a matching response in time. */
    public static final class ProtocolTimeoutException extends IOException {
        public ProtocolTimeoutException(String message) { super(message); }
    }

    /** Read-poll timeout used by the reader thread — drives close() responsiveness. */
    public static final int READ_POLL_MS = 100;
    /** Default {@code sendAndAwait} timeout per truenorth §5.6. */
    public static final int DEFAULT_TIMEOUT_MS = 1500;
    /** Max wait on each thread join during {@code close()} — keeps shutdown under §5.11's 1500 ms cap. */
    public static final int SHUTDOWN_JOIN_MS = 500;

    private final SerialLink link;
    private final PacketLogger log;

    private final LinkedBlockingQueue<HostBlock> rxQueue = new LinkedBlockingQueue<>();
    // Fair lock so queued callers are serviced in FIFO order — matters if
    // M4's UI thread and a background prefetcher ever both issue commands.
    private final ReentrantLock sendLock = new ReentrantLock(true);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed  = new AtomicBoolean(false);

    private volatile Consumer<HostBlock> unsolicitedHandler = b -> { /* no-op */ };
    private volatile PendingRequest pending;
    private volatile IOException readerError;
    private volatile int savedReadTimeoutMs;

    private Thread readerThread;
    private Thread protocolThread;

    public PK232Client(SerialLink link, PacketLogger log) {
        this.link = Objects.requireNonNull(link, "link");
        this.log  = Objects.requireNonNull(log, "log");
    }

    /**
     * Replace the unsolicited-block handler. Safe to call before or after
     * {@link #start()} and from any thread — the handler is volatile and
     * invoked only on the Protocol Thread. Null installs a no-op.
     */
    public void setUnsolicitedHandler(Consumer<HostBlock> handler) {
        this.unsolicitedHandler = handler == null ? b -> { /* no-op */ } : handler;
    }

    /**
     * Spawn the reader + protocol threads. Must be called exactly once,
     * after the link is open and the modem is in host mode. The caller's
     * read-timeout on the {@link SerialLink} is saved and restored in
     * {@link #close()} so this is safe to sandwich between other protocol
     * code that has its own timing expectations.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("PK232Client already started");
        }
        if (!link.isOpen()) {
            started.set(false);
            throw new IllegalStateException("serial link is not open");
        }
        savedReadTimeoutMs = link.getReadTimeoutMs();
        link.setReadTimeoutMs(READ_POLL_MS);

        readerThread = new Thread(this::readerLoop, "PK232-Reader");
        readerThread.setDaemon(false);
        protocolThread = new Thread(this::protocolLoop, "PK232-Protocol");
        protocolThread.setDaemon(false);
        readerThread.start();
        protocolThread.start();
        log.logRaw("PK232Client: started (reader + protocol threads)");
    }

    public boolean isRunning() {
        return started.get() && !closed.get();
    }

    /**
     * Send a framed request and wait up to {@code timeoutMs} for the next
     * {@code 0x4F} response block. Enforced depth = 1 — callers serialize
     * through a fair {@link ReentrantLock}. Non-correlated blocks that
     * arrive during the wait are routed to the unsolicited handler and do
     * not satisfy this call.
     *
     * @throws ProtocolTimeoutException if no matching response arrives in time
     * @throws IOException               if the reader thread has died or the
     *                                   client is not running
     * @throws InterruptedException      if the calling thread is interrupted
     *                                   while awaiting the response
     */
    public HostBlock sendAndAwait(HostBlock request, long timeoutMs)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be > 0: " + timeoutMs);
        }
        if (!isRunning()) {
            throw new IOException("PK232Client is not running");
        }
        IOException priorReaderFailure = readerError;
        if (priorReaderFailure != null) {
            throw new IOException("reader thread failed earlier: "
                    + priorReaderFailure.getMessage(), priorReaderFailure);
        }

        String mnem = deriveMnemonic(request, "CMD");
        sendLock.lockInterruptibly();
        try {
            PendingRequest p = new PendingRequest();
            pending = p;
            byte[] framed = HostCodec.encode(request.ctl(), request.payloadCopy());
            log.log(PacketLogger.Direction.TX, mnem, framed, 0, framed.length);
            link.write(framed);

            HostBlock response = p.await(timeoutMs);
            if (response == null) {
                throw new ProtocolTimeoutException(
                        "no response to TX " + mnem + " within " + timeoutMs + " ms");
            }
            return response;
        } finally {
            // Clear pending BEFORE releasing the send lock. A stale response
            // arriving after this point will see pending == null on the
            // Protocol Thread and be routed to the unsolicited handler
            // rather than falsely correlating to the next command.
            pending = null;
            sendLock.unlock();
        }
    }

    /**
     * Set the modem's ADDRESS register via the {@code AE <decimal>} host
     * command (truenorth A2 + A6). {@code addr} must fit in 16 bits. The
     * caller is expected to have already parsed the 4-hex UI value via
     * {@link HexUtils#parse4Hex(String)}.
     *
     * <p>Expected response: {@code 01 4F 41 45 00 17} (mnemonic echo +
     * status byte {@code c == 0x00}). A non-zero status byte or any
     * malformed envelope throws {@link IOException} (per §5.6, the caller
     * aborts the wider operation on such a failure).
     */
    public void setAddress(int addr) throws IOException, InterruptedException {
        if (addr < 0 || addr > 0xFFFF) {
            throw new IllegalArgumentException(
                    "address out of 16-bit range: 0x" + Integer.toHexString(addr));
        }
        HostBlock req = HostBlock.ofAscii(
                HostBlock.CTL_GLOBAL_COMMAND,
                "AE" + Integer.toString(addr));
        HostBlock resp = sendAndAwait(req, DEFAULT_TIMEOUT_MS);
        validateAEPayload(resp);
    }

    /**
     * Read one byte from the current ADDRESS, which the PK-232
     * auto-increments on each {@code MM} (truenorth A3, {@code hCmd.md}
     * §MEmory). Returns the byte as an unsigned {@code int} 0..255.
     *
     * <p>Expected response: {@code 01 4F 4D 4D 24 <hi> <lo> 17} — i.e.
     * payload {@code "MM$<hi><lo>"} where {@code <hi><lo>} is an
     * ASCII-hex pair (truenorth A1 + §8 Change Log 2026-04-21 MM payload
     * fix). The literal {@code '$'} separator is the hardware-observed
     * format; it is drained before decoding via
     * {@link HexUtils#fromAsciiHexPair(int, int)}. Any envelope or
     * decode failure throws {@link IOException}; timeouts throw
     * {@link ProtocolTimeoutException}. Caller is expected to abort the
     * dump on any exception per §5.6.
     */
    public int readOneByte() throws IOException, InterruptedException {
        HostBlock req = HostBlock.ofAscii(HostBlock.CTL_GLOBAL_COMMAND, "MM");
        HostBlock resp = sendAndAwait(req, DEFAULT_TIMEOUT_MS);
        return parseMMPayload(resp);
    }

    /**
     * Validate the {@code AE} response envelope. Package-private so tests
     * can exercise the validation branches directly without mocking a
     * {@link SerialLink} — the static-helper split was a deliberate M4
     * design decision (Option-3 relaxed for M4 per §8 Change Log
     * 2026-04-20). Visible for {@code test/protocol/PK232ClientHelperTests.java}.
     */
    static void validateAEPayload(HostBlock resp) throws IOException {
        if (!resp.isGlobalCommand()) {
            throw new IOException("AE: expected 0x4F response, got " + resp);
        }
        if (resp.payloadLength() != 3
                || resp.payloadAt(0) != 'A'
                || resp.payloadAt(1) != 'E') {
            throw new IOException("AE: malformed payload, got " + resp);
        }
        int status = resp.payloadAt(2) & 0xFF;
        if (status != 0x00) {
            throw new IOException(
                    "AE: modem rejected with status 0x"
                            + String.format("%02X", status));
        }
    }

    /**
     * Validate and decode the {@code MM} response envelope. Returns the
     * byte value 0..255 that the modem reported at the current ADDRESS.
     * Package-private so tests can exercise it directly (see
     * {@link #validateAEPayload(HostBlock)} for the extraction rationale).
     */
    static int parseMMPayload(HostBlock resp) throws IOException {
        if (!resp.isGlobalCommand()) {
            throw new IOException("MM: expected 0x4F response, got " + resp);
        }
        // Hardware-observed MM response payload is "MM$<hi><lo>" (5 bytes) —
        // the literal '$' separator between the mnemonic echo and the
        // ASCII-hex pair is part of the on-wire format, not a parser
        // artifact (truenorth A1 + §8 Change Log 2026-04-21).
        if (resp.payloadLength() != 5
                || resp.payloadAt(0) != 'M'
                || resp.payloadAt(1) != 'M'
                || resp.payloadAt(2) != '$') {
            throw new IOException("MM: malformed payload, got " + resp);
        }
        int hi = resp.payloadAt(3) & 0xFF;
        int lo = resp.payloadAt(4) & 0xFF;
        try {
            return HexUtils.fromAsciiHexPair(hi, lo);
        } catch (IllegalArgumentException iae) {
            throw new IOException(
                    "MM: non-hex ASCII pair in payload: " + iae.getMessage(), iae);
        }
    }

    /**
     * Stop the client. Idempotent. Interrupts both threads, joins each
     * with a {@link #SHUTDOWN_JOIN_MS} cap, and restores the caller's
     * original {@link SerialLink} read-timeout so the bare-link exit path
     * ({@code HostModeEntry.tryExit}) sees the same configuration it set.
     * Does NOT close the {@link SerialLink} itself — that's the caller's
     * responsibility (ownership stays with {@code Main}).
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Thread r = readerThread;
        Thread p = protocolThread;
        if (r != null) r.interrupt();
        if (p != null) p.interrupt();
        joinQuietly(r);
        joinQuietly(p);

        // Restore only if the link is still open; if it was closed out from
        // under us (e.g. a hardware yank) setReadTimeoutMs is a no-op per
        // SerialLink's contract but we still skip it to avoid masking the
        // real cause with a secondary IllegalStateException.
        if (link.isOpen()) {
            try {
                link.setReadTimeoutMs(savedReadTimeoutMs);
            } catch (RuntimeException re) {
                log.logRaw("PK232Client: could not restore read-timeout (" + re + ")");
            }
        }
        log.logRaw("PK232Client: stopped");
    }

    private void readerLoop() {
        byte[] buf = new byte[256];
        HostCodec.Parser parser = HostCodec.newParser();
        while (!closed.get()) {
            int n;
            try {
                n = link.read(buf, 0, buf.length);
            } catch (IOException ioe) {
                readerError = ioe;
                log.logRaw("PK232Client reader: IOException, thread exiting: "
                        + ioe.getMessage());
                // Signal protocol thread via interrupt so it bails promptly
                // instead of waiting on poll.
                Thread pt = protocolThread;
                if (pt != null) pt.interrupt();
                return;
            } catch (RuntimeException re) {
                log.logRaw("PK232Client reader: crashed, thread exiting: " + re);
                readerError = new IOException(re);
                Thread pt = protocolThread;
                if (pt != null) pt.interrupt();
                return;
            }
            if (n <= 0) {
                continue;
            }
            List<HostBlock> blocks;
            try {
                blocks = parser.feed(buf, 0, n);
            } catch (RuntimeException re) {
                log.logRaw("PK232Client reader: parser crashed (" + re + "), resyncing");
                parser = HostCodec.newParser();
                continue;
            }
            for (HostBlock block : blocks) {
                rxQueue.add(block);
            }
        }
    }

    private void protocolLoop() {
        while (!closed.get()) {
            HostBlock block;
            try {
                block = rxQueue.poll(READ_POLL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                if (closed.get()) {
                    return;
                }
                // Spurious interrupt during normal operation — re-assert and
                // continue; the only legitimate interrupt sources are
                // close() and the reader's error path, both of which set
                // closed or readerError first.
                continue;
            }
            if (block == null) {
                continue;
            }
            try {
                dispatch(block);
            } catch (RuntimeException re) {
                log.logRaw("PK232Client protocol: dispatch crashed (" + re + ")");
            }
        }
    }

    private void dispatch(HostBlock block) {
        PendingRequest p = pending;
        if (p != null && block.isGlobalCommand()) {
            logRx(block, deriveMnemonic(block, "UNS"));
            p.complete(block);
            return;
        }
        String mnem = block.isStatusOrError() ? "ERR" : "UNS";
        logRx(block, mnem);
        Consumer<HostBlock> handler = unsolicitedHandler;
        try {
            handler.accept(block);
        } catch (RuntimeException re) {
            log.logRaw("PK232Client: unsolicited handler threw (" + re + ")");
        }
    }

    private void logRx(HostBlock block, String mnemonic) {
        byte[] framed = HostCodec.encode(block.ctl(), block.payloadCopy());
        log.log(PacketLogger.Direction.RX, mnemonic, framed, 0, framed.length);
    }

    /**
     * Derive a short mnemonic from a block's payload — the first two bytes
     * if both are ASCII-alpha, else {@code fallback}. Matches the
     * convention for {@code AE}, {@code MM}, {@code HO}, {@code HP}-class
     * commands and their echoed responses.
     */
    private static String deriveMnemonic(HostBlock block, String fallback) {
        if (block.payloadLength() < 2) {
            return fallback;
        }
        int b0 = block.payloadAt(0) & 0xFF;
        int b1 = block.payloadAt(1) & 0xFF;
        if (isAsciiAlpha(b0) && isAsciiAlpha(b1)) {
            return "" + (char) b0 + (char) b1;
        }
        return fallback;
    }

    private static boolean isAsciiAlpha(int u) {
        return (u >= 'A' && u <= 'Z') || (u >= 'a' && u <= 'z');
    }

    private void joinQuietly(Thread t) {
        if (t == null) return;
        try {
            t.join(SHUTDOWN_JOIN_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Single-shot handoff slot for the currently-outstanding
     * {@code sendAndAwait}. {@link #complete(HostBlock)} may be called at
     * most once; subsequent calls are benign (CountDownLatch is idempotent
     * past zero).
     */
    private static final class PendingRequest {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile HostBlock response;

        void complete(HostBlock block) {
            response = block;
            latch.countDown();
        }

        HostBlock await(long timeoutMs) throws InterruptedException {
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                return response;
            }
            return null;
        }
    }
}
