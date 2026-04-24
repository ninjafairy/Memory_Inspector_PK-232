package util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Rolling single-file packet logger per truenorth.md F1–F3.
 *
 * <p>Directory: {@code ./Logs/} relative to the working directory (created on
 * first write). File name: {@code memory_inspector.log}, rotated to
 * {@code .1} / {@code .2} suffixes. When the active file reaches the
 * {@value #DEFAULT_SIZE_CAP_BYTES}-byte cap, it rotates and a new file begins;
 * the oldest beyond {@code .2} is dropped. Only the three most recent files
 * are ever kept on disk (~30 KB total footprint).
 *
 * <p>Line format (F3): {@code <TX|RX> <CMD>  <HEX>  <ASCII>} — two-space
 * column separators, no timestamp. Non-printable bytes in the ASCII column are
 * rendered as {@code '.'} per C5.
 *
 * <p>Thread-safe: all write paths are guarded by a single intrinsic lock.
 */
public final class PacketLogger implements AutoCloseable {

    public enum Direction { TX, RX }

    public static final String DEFAULT_LOG_DIR_NAME = "Logs";
    public static final String DEFAULT_LOG_FILE_NAME = "memory_inspector.log";
    public static final long DEFAULT_SIZE_CAP_BYTES = 10L * 1024L;
    public static final int DEFAULT_KEEP_FILES = 3;

    private final Path logDir;
    private final Path activeFile;
    private final long sizeCapBytes;
    private final int keepFiles;
    private final byte[] lineSep;

    private OutputStream out;
    private long currentSize;
    private boolean closed;

    /**
     * Open a logger against {@code ./Logs/memory_inspector.log} with the
     * defaults from F1/F2.
     */
    public static PacketLogger defaultLogger() {
        return new PacketLogger(
                Path.of(DEFAULT_LOG_DIR_NAME),
                DEFAULT_LOG_FILE_NAME,
                DEFAULT_SIZE_CAP_BYTES,
                DEFAULT_KEEP_FILES);
    }

    public PacketLogger(Path logDir, String fileName, long sizeCapBytes, int keepFiles) {
        if (sizeCapBytes <= 0) {
            throw new IllegalArgumentException("sizeCapBytes must be > 0");
        }
        if (keepFiles < 1) {
            throw new IllegalArgumentException("keepFiles must be >= 1");
        }
        this.logDir = Objects.requireNonNull(logDir, "logDir");
        this.activeFile = logDir.resolve(Objects.requireNonNull(fileName, "fileName"));
        this.sizeCapBytes = sizeCapBytes;
        this.keepFiles = keepFiles;
        this.lineSep = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    }

    /** Log a TX (host -> PK-232) packet. */
    public void logTx(String command, byte[] framed) {
        log(Direction.TX, command, framed, 0, framed == null ? 0 : framed.length);
    }

    /** Log an RX (PK-232 -> host) packet. */
    public void logRx(String command, byte[] framed) {
        log(Direction.RX, command, framed, 0, framed == null ? 0 : framed.length);
    }

    /**
     * Log a framed packet. {@code command} is the short mnemonic class name
     * (e.g. {@code AE}, {@code MM}, {@code OGG}, {@code HON}, {@code ACK},
     * {@code ERR}). {@code framed} is the full on-the-wire byte sequence
     * including {@code SOH}, {@code CTL}, payload (DLE-escaped as sent), and
     * {@code ETB}.
     */
    public synchronized void log(Direction direction, String command,
                                 byte[] framed, int offset, int length) {
        if (closed) {
            return;
        }
        Objects.requireNonNull(direction, "direction");
        String cmd = command == null ? "" : command;
        String hex = framed == null ? "" : HexUtils.bytesToSpacedHex(framed, offset, length);
        String asc = framed == null ? "" : HexUtils.bytesToAsciiRender(framed, offset, length);
        String line = direction.name() + " " + cmd + "  " + hex + "  " + asc;
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);

        try {
            ensureOpen();
            if (currentSize > 0 && currentSize + bytes.length + lineSep.length > sizeCapBytes) {
                rotate();
            }
            out.write(bytes);
            out.write(lineSep);
            out.flush();
            currentSize += bytes.length + lineSep.length;
        } catch (IOException ioe) {
            // A logger that throws would take down the dump loop. Best-effort
            // only: report to stderr (captured by javaw's sink) and continue.
            System.err.println("[PacketLogger] write failed: " + ioe.getMessage());
        }
    }

    /**
     * Dump a free-form diagnostic line to the log stream (non-packet). Useful
     * for recording exit-path events, state transitions, etc. Counted against
     * the rotation cap the same as a normal entry.
     */
    public synchronized void logRaw(String message) {
        if (closed || message == null) {
            return;
        }
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        try {
            ensureOpen();
            if (currentSize > 0 && currentSize + bytes.length + lineSep.length > sizeCapBytes) {
                rotate();
            }
            out.write(bytes);
            out.write(lineSep);
            out.flush();
            currentSize += bytes.length + lineSep.length;
        } catch (IOException ioe) {
            System.err.println("[PacketLogger] write failed: " + ioe.getMessage());
        }
    }

    /**
     * Open a CR-framed RX line accumulator bound to this logger and the given
     * mnemonic. Command-mode RX streams (pre-host-mode verbose dialog, reset
     * banners, detection drain) arrive one byte per serial read at 9600 baud,
     * which — written naively — produces one log line per byte. Callers feed
     * raw RX chunks into the returned buffer and it emits a single
     * {@code RX <mnemonic>  <hex>  <ascii>} line per CR-terminated wire line.
     *
     * <p>CRLF handling: a CR is buffered for one byte. If the next byte fed
     * is LF (0x0A) it is appended to the same line before emit; if it is
     * anything else the CR-terminated line is emitted first and the new byte
     * starts a fresh line. This accommodates both {@code ALFD ON} (CRLF
     * terminators) and {@code ALFD OFF} (bare CR) equally well.
     *
     * <p>Callers should use try-with-resources so any trailing partial line
     * (common — e.g. a final {@code cmd:} prompt arrives without a CR) is
     * emitted at window end.
     */
    public RxLineBuffer streamRx(String mnemonic) {
        return new RxLineBuffer(this, mnemonic);
    }

    @Override
    public synchronized void close() {
        closed = true;
        closeQuiet(out);
        out = null;
    }

    private void ensureOpen() throws IOException {
        if (out != null) {
            return;
        }
        if (!Files.isDirectory(logDir)) {
            Files.createDirectories(logDir);
        }
        currentSize = Files.exists(activeFile) ? Files.size(activeFile) : 0L;
        out = Files.newOutputStream(activeFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    private void rotate() throws IOException {
        closeQuiet(out);
        out = null;

        // Drop the oldest retained slot so the rest can shift down.
        Files.deleteIfExists(siblingWithSuffix(keepFiles - 1));

        // Shift .(n-1) -> .n, ..., .1 -> .2.
        for (int i = keepFiles - 1; i >= 2; i--) {
            Path src = siblingWithSuffix(i - 1);
            if (Files.exists(src)) {
                Files.move(src, siblingWithSuffix(i), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Move active -> .1.
        if (Files.exists(activeFile)) {
            Files.move(activeFile, siblingWithSuffix(1), StandardCopyOption.REPLACE_EXISTING);
        }

        currentSize = 0L;
        out = Files.newOutputStream(activeFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Path siblingWithSuffix(int n) {
        return activeFile.resolveSibling(activeFile.getFileName().toString() + "." + n);
    }

    private static void closeQuiet(OutputStream s) {
        if (s == null) return;
        try {
            s.close();
        } catch (IOException ignore) {
            // Swallowed intentionally; logger shouldn't crash on close errors.
        }
    }

    // Package-private accessors for tests. Not part of the public contract.
    Path activeFileForTest() {
        return activeFile;
    }

    Path siblingForTest(int n) {
        return siblingWithSuffix(n);
    }

    /**
     * CR-framed accumulator for command-mode RX streams. See
     * {@link PacketLogger#streamRx(String)} for the factory and full contract.
     *
     * <p>Not thread-safe: each caller is expected to own its instance for the
     * lifetime of a single RX collection window. Serialization of the
     * underlying log writes is still guaranteed by {@link PacketLogger}'s
     * intrinsic lock.
     */
    public static final class RxLineBuffer implements AutoCloseable {
        private static final byte CR = 0x0D;
        private static final byte LF = 0x0A;

        private final PacketLogger logger;
        private final String mnemonic;
        private byte[] buf = new byte[256];
        private int size = 0;
        private boolean pendingCR = false;

        private RxLineBuffer(PacketLogger logger, String mnemonic) {
            this.logger = Objects.requireNonNull(logger, "logger");
            this.mnemonic = mnemonic == null ? "" : mnemonic;
        }

        /**
         * Feed a chunk of raw RX bytes. Each CR triggers a log-line emit
         * (with an optional trailing LF absorbed from the next byte if
         * present — see {@link PacketLogger#streamRx(String)}).
         */
        public void feed(byte[] src, int offset, int length) {
            if (src == null || length <= 0) {
                return;
            }
            if (offset < 0 || offset + length > src.length) {
                throw new IndexOutOfBoundsException(
                        "offset=" + offset + " length=" + length
                                + " src.length=" + src.length);
            }
            for (int i = 0; i < length; i++) {
                byte b = src[offset + i];
                if (pendingCR) {
                    if (b == LF) {
                        append(b);
                        emit();
                        pendingCR = false;
                        continue;
                    }
                    // ALFD OFF: the pending CR stood alone — emit it, then
                    // process this byte as the first of a fresh line.
                    emit();
                    pendingCR = false;
                }
                append(b);
                if (b == CR) {
                    pendingCR = true;
                }
            }
        }

        /** Emit any buffered-but-not-yet-terminated bytes as a final line. */
        public void flush() {
            if (size > 0) {
                emit();
            }
            pendingCR = false;
        }

        @Override
        public void close() {
            flush();
        }

        private void append(byte b) {
            if (size == buf.length) {
                byte[] grown = new byte[buf.length * 2];
                System.arraycopy(buf, 0, grown, 0, size);
                buf = grown;
            }
            buf[size++] = b;
        }

        private void emit() {
            logger.log(Direction.RX, mnemonic, buf, 0, size);
            size = 0;
        }
    }
}
