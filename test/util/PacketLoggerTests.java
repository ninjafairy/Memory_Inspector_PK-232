package util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PacketLogger}. Verifies the wire-format line shape
 * (truenorth F3), rotation semantics (F1/F2), and thread-safe append.
 */
class PacketLoggerTests {

    @TempDir
    Path tmp;

    private Path logDir;

    @BeforeEach
    void setUp() {
        logDir = tmp.resolve("Logs");
    }

    @Test
    @DisplayName("F3: TX MM frame produces the canonical log line")
    void txMM_writesCanonicalLine() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            byte[] mmFrame = { 0x01, 0x4F, 0x4D, 0x4D, 0x17 };
            log.logTx("MM", mmFrame);
        }
        List<String> lines = Files.readAllLines(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8);
        assertEquals(1, lines.size(), "expected exactly one line");
        assertEquals("TX MM  01 4F 4D 4D 17  .OMM.", lines.get(0));
    }

    @Test
    @DisplayName("F3: RX MM read response renders hi/lo ASCII hex payload")
    void rxMM_renderAsciiHexPayload() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            byte[] rxMM = { 0x01, 0x4F, 0x4D, 0x4D, 0x33, 0x46, 0x17 };
            log.logRx("MM", rxMM);
        }
        List<String> lines = Files.readAllLines(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("RX MM  01 4F 4D 4D 33 46 17  .OMM3F.", lines.get(0));
    }

    @Test
    @DisplayName("F3: non-printables in payload collapse to '.' in the ASCII column")
    void nonPrintables_asDot() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            byte[] oggProbe = { 0x01, 0x4F, 0x47, 0x47, 0x17 };
            log.logTx("OGG", oggProbe);
        }
        String content = Files.readString(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8).trim();
        assertEquals("TX OGG  01 4F 47 47 17  .OGG.", content);
    }

    @Test
    @DisplayName("F1/F2: rotates at cap and retains at most three files")
    void rotates_atCap_keepsThree() throws IOException {
        long cap = 200L; // Small cap for deterministic rotation.
        int keep = 3;
        byte[] frame = makeFrame(40); // ~120-byte line after hex+ascii expansion.

        try (PacketLogger log = newLogger(cap, keep)) {
            // Write enough entries that we force several rotations.
            for (int i = 0; i < 20; i++) {
                log.logTx("MM", frame);
            }
        }

        Path active = logDir.resolve("memory_inspector.log");
        Path dot1   = logDir.resolve("memory_inspector.log.1");
        Path dot2   = logDir.resolve("memory_inspector.log.2");
        Path dot3   = logDir.resolve("memory_inspector.log.3");

        assertTrue(Files.exists(active), "active file missing after rotation");
        assertTrue(Files.exists(dot1),   ".1 missing after rotation");
        assertTrue(Files.exists(dot2),   ".2 missing after rotation");
        assertFalse(Files.exists(dot3),  ".3 must not exist (keepFiles=3)");
    }

    @Test
    @DisplayName("F2: a single oversized line does not trigger pointless rotation")
    void oversizedLine_goesInFreshFile() throws IOException {
        long cap = 50L;
        byte[] big = makeFrame(200); // line much larger than cap
        try (PacketLogger log = newLogger(cap, 3)) {
            log.logTx("MM", big);
        }
        // No pre-existing active content, so no rotation should have occurred
        // and the oversized line should sit alone in the active file.
        Path active = logDir.resolve("memory_inspector.log");
        Path dot1   = logDir.resolve("memory_inspector.log.1");
        assertTrue(Files.exists(active));
        assertFalse(Files.exists(dot1), ".1 must not exist after a single oversized write into an empty active file");
        assertTrue(Files.size(active) > cap, "oversized line should be present");
    }

    @Test
    @DisplayName("logRaw: writes free-form diagnostics and honors rotation")
    void logRaw_writesAndRotates() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            log.logRaw("hello world");
        }
        List<String> lines = Files.readAllLines(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("hello world", lines.get(0));
    }

    @Test
    @DisplayName("Concurrent writers produce the exact expected line count with no corruption")
    void concurrentWrites_noCorruption() throws Exception {
        final int threads = 8;
        final int perThread = 250;
        try (PacketLogger log = newLogger(1024L * 1024L, 3)) {
            Thread[] workers = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                workers[t] = new Thread(() -> {
                    byte[] frame = { 0x01, 0x4F, 0x4D, 0x4D, 0x17 };
                    for (int i = 0; i < perThread; i++) {
                        log.logTx("MM", frame);
                    }
                });
                workers[t].start();
            }
            for (Thread w : workers) w.join();
        }

        List<String> lines = Files.readAllLines(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8);
        assertEquals(threads * perThread, lines.size(), "unexpected line count");
        for (String line : lines) {
            assertEquals("TX MM  01 4F 4D 4D 17  .OMM.", line, "corrupted line detected");
        }
    }

    @Test
    @DisplayName("RxLineBuffer: CRLF-terminated line emits as a single log entry including CR and LF")
    void rxLineBuffer_crlf_emitsSingleLine() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("RST")) {
                // "AB" + CR + LF arriving one byte at a time, as at 9600 baud.
                feedBytes(rx, 0x41, 0x42, 0x0D, 0x0A);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(1, lines.size(), "expected exactly one emitted line, got: " + lines);
        assertEquals("RX RST  41 42 0D 0A  AB..", lines.get(0));
    }

    @Test
    @DisplayName("RxLineBuffer: bare CR (ALFD OFF) emits a line at CR without waiting for LF")
    void rxLineBuffer_bareCR_emitsAtCR() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("RST")) {
                // ALFD OFF pattern: two bare-CR lines back-to-back, no LFs.
                feedBytes(rx, 0x41, 0x42, 0x0D, 0x43, 0x44, 0x0D);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(2, lines.size(), "expected two emitted lines, got: " + lines);
        assertEquals("RX RST  41 42 0D  AB.", lines.get(0));
        assertEquals("RX RST  43 44 0D  CD.", lines.get(1));
    }

    @Test
    @DisplayName("RxLineBuffer: mixed ALFD ON/OFF lines in the same stream all resolve correctly")
    void rxLineBuffer_mixedCRandCRLF() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("RST")) {
                // "A" + CRLF, then "B" + CR (bare, ALFD toggled off), then "C" + CRLF.
                feedBytes(rx, 0x41, 0x0D, 0x0A, 0x42, 0x0D, 0x43, 0x0D, 0x0A);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(3, lines.size(), "expected three emitted lines, got: " + lines);
        assertEquals("RX RST  41 0D 0A  A..", lines.get(0));
        assertEquals("RX RST  42 0D  B.",    lines.get(1));
        assertEquals("RX RST  43 0D 0A  C..", lines.get(2));
    }

    @Test
    @DisplayName("RxLineBuffer: partial line without terminator is flushed on close")
    void rxLineBuffer_partialLineFlushedOnClose() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("CMD")) {
                // "cmd:" without any terminator — common at end of verbose-command window.
                feedBytes(rx, 0x63, 0x6D, 0x64, 0x3A);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(1, lines.size(), "expected the partial line to be flushed on close");
        assertEquals("RX CMD  63 6D 64 3A  cmd:", lines.get(0));
    }

    @Test
    @DisplayName("RxLineBuffer: CR buffered at end of feed is emitted with paired LF arriving in next feed")
    void rxLineBuffer_crAcrossFeedBoundary_absorbsLFFromNextFeed() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("BAN")) {
                // Serial read returns "A" + CR; next read returns LF + "B".
                feedBytes(rx, 0x41, 0x0D);
                feedBytes(rx, 0x0A, 0x42);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(2, lines.size(), "expected two lines (CRLF-terminated + partial), got: " + lines);
        assertEquals("RX BAN  41 0D 0A  A..", lines.get(0));
        assertEquals("RX BAN  42  B",        lines.get(1));
    }

    @Test
    @DisplayName("RxLineBuffer: CR buffered at end of feed is emitted on flush when no more bytes arrive")
    void rxLineBuffer_pendingCRFlushedOnClose() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("DRN")) {
                feedBytes(rx, 0x41, 0x42, 0x0D);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(1, lines.size());
        assertEquals("RX DRN  41 42 0D  AB.", lines.get(0));
    }

    @Test
    @DisplayName("RxLineBuffer: empty feed is a no-op and does not emit a blank line")
    void rxLineBuffer_emptyFeedIsNoop() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            try (PacketLogger.RxLineBuffer rx = log.streamRx("DET")) {
                rx.feed(new byte[0], 0, 0);
                rx.feed(new byte[]{0x41}, 0, 0);
                rx.feed(null, 0, 1);
            }
        }
        // The logger creates the file lazily on first write; no writes means
        // no file, which is the strongest evidence that nothing was emitted.
        Path active = logDir.resolve("memory_inspector.log");
        assertFalse(Files.exists(active),
                "no bytes fed should produce no log file at all");
    }

    @Test
    @DisplayName("RxLineBuffer: flush() is idempotent and leaves the buffer empty")
    void rxLineBuffer_flushIsIdempotent() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            PacketLogger.RxLineBuffer rx = log.streamRx("CMD");
            feedBytes(rx, 0x41, 0x42);
            rx.flush();
            rx.flush();
            rx.close();
        }
        List<String> lines = readLogLines();
        assertEquals(1, lines.size(), "only one flush should produce output, got: " + lines);
        assertEquals("RX CMD  41 42  AB", lines.get(0));
    }

    @Test
    @DisplayName("RxLineBuffer: large chunk spanning multiple CRLF lines splits correctly in one feed call")
    void rxLineBuffer_multipleLinesInSingleFeed() throws IOException {
        try (PacketLogger log = newLogger(10 * 1024, 3)) {
            byte[] chunk = {
                    0x41, 0x0D, 0x0A,        // "A" + CRLF
                    0x42, 0x43, 0x0D, 0x0A,  // "BC" + CRLF
                    0x63, 0x6D, 0x64, 0x3A   // "cmd:" (no terminator)
            };
            try (PacketLogger.RxLineBuffer rx = log.streamRx("RST")) {
                rx.feed(chunk, 0, chunk.length);
            }
        }
        List<String> lines = readLogLines();
        assertEquals(3, lines.size(), "expected two CRLF lines + one flushed partial, got: " + lines);
        assertEquals("RX RST  41 0D 0A  A..",     lines.get(0));
        assertEquals("RX RST  42 43 0D 0A  BC..", lines.get(1));
        assertEquals("RX RST  63 6D 64 3A  cmd:", lines.get(2));
    }

    private static void feedBytes(PacketLogger.RxLineBuffer rx, int... bytes) {
        byte[] chunk = new byte[1];
        for (int b : bytes) {
            chunk[0] = (byte) b;
            rx.feed(chunk, 0, 1);
        }
    }

    private List<String> readLogLines() throws IOException {
        return Files.readAllLines(logDir.resolve("memory_inspector.log"),
                StandardCharsets.UTF_8);
    }

    private PacketLogger newLogger(long cap, int keep) {
        return new PacketLogger(logDir, "memory_inspector.log", cap, keep);
    }

    private static byte[] makeFrame(int payloadSize) {
        byte[] f = new byte[payloadSize + 4];
        f[0] = 0x01;
        f[1] = 0x4F;
        f[2] = 0x4D;
        f[3] = 0x4D;
        for (int i = 4; i < f.length - 1; i++) {
            f[i] = (byte) ('A' + (i % 26));
        }
        f[f.length - 1] = 0x17;
        return f;
    }
}
