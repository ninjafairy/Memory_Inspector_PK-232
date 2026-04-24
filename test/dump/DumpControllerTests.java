package dump;

import dump.DumpController.ViewMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DumpController}'s pure {@code renderBuffer}
 * helper — the only part of the class that isn't worker-thread /
 * {@link protocol.PK232Client}-coupled. Matches the Option-3 relaxed
 * pattern established in M4 for {@code parseMMPayload} /
 * {@code validateAEPayload} (truenorth §10 + §8 Change Log 2026-04-20
 * M4 design-lock entry).
 */
class DumpControllerTests {

    // ------------------------------------------------------------
    // Null / empty / range handling
    // ------------------------------------------------------------

    @Test
    @DisplayName("renderBuffer: null data or length=0 returns empty string")
    void render_empty_is_empty() {
        assertEquals("", DumpController.renderBuffer(null, 0, 0x0000, ViewMode.HEX));
        assertEquals("", DumpController.renderBuffer(
                new byte[] { 0x00, 0x01 }, 0, 0x0000, ViewMode.HEX));
        assertEquals("", DumpController.renderBuffer(
                new byte[0], 0, 0x1234, ViewMode.ASCII));
    }

    @Test
    @DisplayName("renderBuffer: length > data.length throws IllegalArgumentException")
    void render_overlong_length_rejected() {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> DumpController.renderBuffer(
                        new byte[4], 5, 0x0000, ViewMode.HEX));
        assertTrue(iae.getMessage().contains("exceeds"),
                "expected 'exceeds' diagnostic, got: " + iae.getMessage());
    }

    @Test
    @DisplayName("renderBuffer: null mode throws NullPointerException")
    void render_null_mode_rejected() {
        assertThrows(NullPointerException.class,
                () -> DumpController.renderBuffer(new byte[1], 1, 0, null));
    }

    // ------------------------------------------------------------
    // HEX mode — one full 16-byte row
    // ------------------------------------------------------------

    @Test
    @DisplayName("renderBuffer: HEX mode shows hex only (no trailing ASCII column)")
    void render_hex_mode_no_ascii_tail() {
        byte[] data = {
                'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o',
                'r', 'l', 'd', '!', 0x00, 0x01, 0x02, 0x03
        };
        String out = DumpController.renderBuffer(data, 16, 0x0000, ViewMode.HEX);
        assertEquals(1, countLines(out));
        assertTrue(out.startsWith("$0000: "),
                "expected address prefix, got: " + out);
        assertTrue(out.contains("48 65 6C 6C 6F 20 57 6F 72 6C 64 21 00 01 02 03"),
                "expected 16-byte hex row, got: " + out);
        assertFalse(out.contains("Hello World"),
                "HEX mode must not include ASCII tail, got: " + out);
        assertTrue(out.endsWith("\n"), "expected trailing newline, got: " + out);
    }

    // ------------------------------------------------------------
    // 128-byte row width
    // ------------------------------------------------------------

    @Test
    @DisplayName("renderBuffer: HEX row width is 128 bytes; 257 bytes span 3 rows with address increments of 0x80")
    void render_hex_128_bytes_per_row() {
        byte[] data = new byte[257];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        String out = DumpController.renderBuffer(data, 257, 0xBF00, ViewMode.HEX);
        assertEquals(3, countLines(out));
        String[] lines = out.split("\n");
        assertTrue(lines[0].startsWith("$BF00: "), "row 0 got: " + lines[0]);
        assertTrue(lines[1].startsWith("$BF80: "), "row 1 got: " + lines[1]);
        assertTrue(lines[2].startsWith("$C000: "), "row 2 got: " + lines[2]);
        // Row 2 has only 1 byte.
        assertTrue(lines[2].endsWith(" 00") || lines[2].equals("$C000: 00"),
                "row 2 partial byte got: " + lines[2]);
    }

    // ------------------------------------------------------------
    // ASCII mode
    // ------------------------------------------------------------

    @Test
    @DisplayName("renderBuffer: ASCII mode omits hex columns, shows printable-or-dot only")
    void render_ascii_mode_printable_and_nonprintable() {
        byte[] data = { 'O', 'M', 'M', 0x01, 0x17, 'X' };
        String out = DumpController.renderBuffer(data, 6, 0x0100, ViewMode.ASCII);
        assertEquals(1, countLines(out));
        assertTrue(out.startsWith("$0100: "),
                "expected address prefix, got: " + out);
        // Non-printable 0x01 and 0x17 collapse to '.' per HexUtils.printableOrDot.
        assertTrue(out.contains("OMM..X"),
                "expected 'OMM..X' ASCII rendering, got: " + out);
        // No hex bytes should be present in ASCII mode.
        assertFalse(out.contains("4F 4D"),
                "ASCII mode should not include hex columns, got: " + out);
    }

    @Test
    @DisplayName("renderBuffer: ASCII mode handles full printable 16-byte row")
    void render_ascii_full_row() {
        byte[] data = "ABCDEFGHIJKLMNOP".getBytes();
        String out = DumpController.renderBuffer(data, 16, 0x0000, ViewMode.ASCII);
        assertEquals(1, countLines(out));
        assertTrue(out.startsWith("$0000: ABCDEFGHIJKLMNOP"),
                "expected '$0000: ABCDEFGHIJKLMNOP', got: " + out);
    }

    // ------------------------------------------------------------
    // Subset rendering (length < data.length)
    // ------------------------------------------------------------

    @Test
    @DisplayName("renderBuffer: length < data.length only renders the first `length` bytes")
    void render_subset_length() {
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ('A' + i);
        }
        // Only render the first 5 bytes even though the buffer is 32.
        String out = DumpController.renderBuffer(data, 5, 0x2000, ViewMode.HEX);
        assertEquals(1, countLines(out));
        assertTrue(out.startsWith("$2000: 41 42 43 44 45"),
                "expected first 5 bytes only, got: " + out);
        // 6th byte onward must NOT appear in the rendering.
        assertFalse(out.contains("46"),
                "length=5 should not render byte index 5 (0x46), got: " + out);
    }

    private static int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') n++;
        }
        return n;
    }
}
