package dump;

import protocol.HostBlock;
import protocol.PK232Client;
import util.HexUtils;
import util.PacketLogger;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * M5 dump controller per truenorth §5.3 / §5.6 / §5.11 and the
 * 2026-04-21 design-lock Change Log entry (seven Step-B answers).
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Own the {@code MemoryInspector-dump} non-daemon worker thread
 *       for the lifetime of a dump. One dump at a time — enforced via
 *       an {@link AtomicBoolean} on {@link #start(int, int, Listener)}.</li>
 *   <li>Always issue {@code AE <addr>} first, then a serialized
 *       N-iteration {@code MM} loop (truenorth C7, re-locked 2026-04-21).
 *       The PK-232 auto-increments ADDRESS on each {@code MM}
 *       (truenorth A3).</li>
 *   <li>Observe a {@code volatile boolean cancelled} between every
 *       {@link PK232Client#readOneByte()} call (Q3 answer 2026-04-21);
 *       the worst-case unblock latency is the client's 1500 ms
 *       {@code sendAndAwait} timeout on the in-flight byte.</li>
 *   <li>Swap the {@link PK232Client} unsolicited handler at dump start
 *       and restore it in {@code finally} (Q4 answer 2026-04-21). A
 *       {@code 0x5F} status/error block flips a
 *       {@code volatile HostBlock abortBlock}; the loop checks it on
 *       the next iteration and exits with {@link Outcome#ABORTED}.</li>
 *   <li>Publish progress on the EDT every
 *       {@value #BATCH_BYTES} bytes (or at the final byte) with Bps
 *       and ETA computed against the wall-clock timer that starts
 *       right after the {@code AE} ack — so the one-shot AE round-trip
 *       does not depress the throughput estimate.</li>
 * </ul>
 *
 * <h2>Buffer ownership</h2>
 * The controller owns the {@code byte[]} buffer for the duration of a
 * dump; each batch callback hands the caller a fresh copy of the NEW
 * bytes ({@code newChunk}, ≤ {@value #BATCH_BYTES} bytes) so the UI can
 * either append-render it or accumulate it in a mirror for a HEX↔ASCII
 * toggle (Q5 / C6). On completion the full buffer (trimmed to actually-
 * read bytes) is delivered once via {@link Listener#onCompleted}.
 *
 * <h2>Testing policy</h2>
 * Worker thread / cancel plumbing / listener dispatch are hardware-gated
 * (Option-3 strict, same as {@code PK232Client} threading). The pure
 * rendering helper {@link #renderBuffer(byte[], int, int, ViewMode)} is
 * public (consumed by {@code app.Main} for HEX↔ASCII toggle re-render
 * as well as by {@code test/dump/DumpControllerTests}) and still
 * exercised directly — Option-3 relaxed, same pattern as M4's
 * {@code parseMMPayload}.
 */
public final class DumpController {

    public enum ViewMode { HEX, ASCII }

    public enum Outcome {
        /** All {@code bytes} bytes read successfully. */
        COMPLETED,
        /** User hit Cancel; partial buffer in the completion callback. */
        CANCELLED,
        /** Unsolicited {@code 0x5F} status/error block during the dump. */
        ABORTED,
        /** {@link IOException} or unexpected runtime error in the loop. */
        FAILED
    }

    /**
     * Progress + completion sink. All callbacks fire on the EDT.
     */
    public interface Listener {
        /**
         * Per-batch progress. {@code newChunk} holds the freshly read
         * bytes for THIS batch (length ≤ {@value #BATCH_BYTES}) and is a
         * defensive copy — callers may retain or mutate it freely.
         */
        void onBatchReady(int bytesSoFar,
                          int totalBytes,
                          double bytesPerSecond,
                          long etaMillis,
                          byte[] newChunk);

        /**
         * Terminal callback. {@code fullBuffer} has exactly
         * {@code bytesRead} bytes — equal to {@code totalBytes} on
         * {@link Outcome#COMPLETED}, smaller on
         * {@link Outcome#CANCELLED} / {@link Outcome#ABORTED} /
         * {@link Outcome#FAILED}. {@code failureMessage} is the
         * user-visible reason ({@code null} on COMPLETED).
         */
        void onCompleted(byte[] fullBuffer,
                         int startAddr,
                         Outcome outcome,
                         String failureMessage);
    }

    /** EDT-append cadence per §5.6 + Q6 2026-04-21 (same as M4). */
    public static final int BATCH_BYTES = 32;

    private final PK232Client client;
    private final PacketLogger log;
    private final Consumer<HostBlock> idleUnsolicitedHandler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean cancelled;
    private volatile HostBlock abortBlock;
    private volatile Thread worker;

    /**
     * @param client the started {@link PK232Client} this controller
     *               issues commands through. Must be running.
     * @param log the application logger for free-form dump events.
     * @param idleUnsolicitedHandler handler to restore on
     *                               {@link PK232Client#setUnsolicitedHandler}
     *                               after a dump ends. {@code null}
     *                               installs a no-op.
     */
    public DumpController(PK232Client client,
                          PacketLogger log,
                          Consumer<HostBlock> idleUnsolicitedHandler) {
        this.client = Objects.requireNonNull(client, "client");
        this.log    = Objects.requireNonNull(log, "log");
        this.idleUnsolicitedHandler = idleUnsolicitedHandler == null
                ? b -> { /* no-op */ }
                : idleUnsolicitedHandler;
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Kick off a dump on the worker thread. Arguments are validated
     * synchronously (range checks per truenorth A6 / C4 / C8); the
     * modem-facing traffic happens on the worker so callers on the EDT
     * never block here beyond the validation and thread-spawn cost.
     *
     * @throws IllegalArgumentException addr / bytes / addr+bytes out of range
     * @throws IllegalStateException    another dump is already running
     *                                  (EDT must gate the Dump button per C2)
     */
    public void start(int addr, int bytes, Listener listener) {
        Objects.requireNonNull(listener, "listener");
        if (addr < 0 || addr > 0xFFFF) {
            throw new IllegalArgumentException(
                    "addr out of 16-bit range: 0x" + Integer.toHexString(addr));
        }
        if (bytes < 1) {
            throw new IllegalArgumentException("bytes must be >= 1: " + bytes);
        }
        if (addr + bytes > 0x10000) {
            // C4 says the caller (DumpPromptDialog) clamps; we still
            // enforce defensively so a misbehaving UI can't walk off
            // the end of memory into undefined modem behavior.
            throw new IllegalArgumentException(
                    "addr + bytes > 0x10000 (expected clamp per C4): addr=0x"
                            + Integer.toHexString(addr) + " bytes=" + bytes);
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("dump already running");
        }

        cancelled = false;
        abortBlock = null;

        byte[] buffer = new byte[bytes];

        Thread w = new Thread(
                () -> dumpLoop(addr, bytes, buffer, listener),
                "MemoryInspector-dump");
        w.setDaemon(false);
        worker = w;
        w.start();
    }

    /**
     * Request cancellation. Flag-only per Q3 2026-04-21 — the worker
     * observes this between {@link PK232Client#readOneByte()} calls and
     * exits with {@link Outcome#CANCELLED}. Idempotent; safe to call
     * from any thread, including the EDT.
     */
    public void cancel() {
        cancelled = true;
    }

    private void dumpLoop(int addr, int bytes, byte[] buffer, Listener listener) {
        // Handler swap — captureUnsolicited flips abortBlock on 0x5F.
        client.setUnsolicitedHandler(this::captureUnsolicited);

        int bytesDone = 0;
        Outcome outcome = null;
        String failureMessage = null;

        try {
            client.setAddress(addr);
            // Start the throughput timer AFTER the AE ack so the one-shot
            // register-set round-trip doesn't skew the MM-loop Bps estimate.
            long startNanos = System.nanoTime();
            int lastBatchStart = 0;

            for (int i = 0; i < bytes; i++) {
                if (cancelled) {
                    outcome = Outcome.CANCELLED;
                    failureMessage = "cancelled by user at byte " + bytesDone;
                    break;
                }
                HostBlock abort = abortBlock;
                if (abort != null) {
                    outcome = Outcome.ABORTED;
                    failureMessage = "aborted by unsolicited " + abort;
                    break;
                }

                int b = client.readOneByte();
                buffer[i] = (byte) b;
                bytesDone = i + 1;

                if ((bytesDone % BATCH_BYTES) == 0 || bytesDone == bytes) {
                    final int done = bytesDone;
                    final int chunkStart = lastBatchStart;
                    final int chunkLen = done - chunkStart;
                    lastBatchStart = done;

                    long elapsedNanos = System.nanoTime() - startNanos;
                    final double bps = elapsedNanos > 0
                            ? (done * 1_000_000_000.0) / elapsedNanos
                            : 0.0;
                    final long etaMs = bps > 0
                            ? (long) (((bytes - done) * 1000.0) / bps)
                            : -1L;

                    final byte[] chunk = Arrays.copyOfRange(
                            buffer, chunkStart, chunkStart + chunkLen);
                    SwingUtilities.invokeLater(() ->
                            listener.onBatchReady(done, bytes, bps, etaMs, chunk));
                }
            }

            if (outcome == null) {
                outcome = Outcome.COMPLETED;
                log.logRaw("dump completed: " + bytes + " bytes from $"
                        + HexUtils.toHex4(addr));
            } else if (outcome == Outcome.CANCELLED) {
                log.logRaw("dump cancelled at byte " + bytesDone);
            } else if (outcome == Outcome.ABORTED) {
                log.logRaw("dump aborted at byte " + bytesDone
                        + ": " + failureMessage);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            outcome = Outcome.CANCELLED;
            failureMessage = "interrupted at byte " + bytesDone;
            log.logRaw("dump interrupted at byte " + bytesDone);
        } catch (IOException ioe) {
            outcome = Outcome.FAILED;
            failureMessage = ioe.getMessage();
            log.logRaw("dump failed at byte " + bytesDone + ": " + ioe.getMessage());
        } catch (RuntimeException re) {
            outcome = Outcome.FAILED;
            failureMessage = re.toString();
            log.logRaw("dump crashed at byte " + bytesDone + ": " + re);
        } finally {
            client.setUnsolicitedHandler(idleUnsolicitedHandler);
            worker = null;
            running.set(false);

            final byte[] snapshot = (bytesDone == bytes)
                    ? buffer
                    : Arrays.copyOf(buffer, bytesDone);
            final int      finalAddr    = addr;
            final Outcome  finalOutcome = (outcome != null ? outcome : Outcome.FAILED);
            final String   finalMsg     = failureMessage;
            SwingUtilities.invokeLater(() ->
                    listener.onCompleted(snapshot, finalAddr, finalOutcome, finalMsg));
        }
    }

    /**
     * Installed as the unsolicited handler while a dump is running.
     * A {@code 0x5F} (status/error class) block flips {@code abortBlock};
     * the worker observes it on the next loop iteration and exits with
     * {@link Outcome#ABORTED}. Everything else is forwarded to the
     * idle handler so hardware-gate traces still record the block.
     */
    private void captureUnsolicited(HostBlock block) {
        if (block != null && block.isStatusOrError() && abortBlock == null) {
            abortBlock = block;
        }
        try {
            idleUnsolicitedHandler.accept(block);
        } catch (RuntimeException re) {
            log.logRaw("DumpController: idle handler threw during dump (" + re + ")");
        }
    }

    /**
     * Render {@code length} bytes of {@code data} for display. Pure,
     * stateless, and unit-tested (package-private). Rows are 16 bytes
     * wide; the address column wraps at 0x10000 (modulo 16 bits) which
     * is a no-op in practice because {@link #start(int, int, Listener)}
     * rejects {@code addr + bytes > 0x10000} per C4.
     *
     * <p>HEX mode: {@code "$AAAA: XX XX ... XX  ASCII"} (hex bytes
     * left-justified and padded so the ASCII column always starts at
     * the same offset even on a partial final row).
     *
     * <p>ASCII mode: {@code "$AAAA: <16 printable-or-dot chars>"}.
     * Trailing row is as long as the actual data for that row (no pad).
     *
     * @param data     buffer (may be longer than {@code length})
     * @param length   number of bytes to render, 0..{@code data.length}
     * @param startAddr address of {@code data[0]} in the PK-232's memory
     * @param mode      HEX or ASCII
     */
    public static String renderBuffer(byte[] data, int length, int startAddr, ViewMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (data == null || length <= 0) {
            return "";
        }
        if (length > data.length) {
            throw new IllegalArgumentException(
                    "length " + length + " exceeds data.length " + data.length);
        }
        final int rowBytes = 128;
        StringBuilder sb = new StringBuilder(length * 4);
        for (int rowStart = 0; rowStart < length; rowStart += rowBytes) {
            int rowLen = Math.min(rowBytes, length - rowStart);
            int rowAddr = (startAddr + rowStart) & 0xFFFF;
            sb.append('$').append(HexUtils.toHex4(rowAddr)).append(": ");
            if (mode == ViewMode.HEX) {
                sb.append(HexUtils.bytesToSpacedHex(data, rowStart, rowLen));
            } else {
                sb.append(HexUtils.bytesToAsciiRender(data, rowStart, rowLen));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
