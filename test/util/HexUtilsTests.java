package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HexUtils}. Covers every conversion called out in
 * truenorth.md Step 6.2.1 plus the display/log formatters in C5 and F3.
 */
class HexUtilsTests {

    @Test
    @DisplayName("parse4Hex: BF0D -> 48909 (canonical AE vector, truenorth A2)")
    void parse4Hex_canonicalAEVector() {
        assertEquals(0xBF0D, HexUtils.parse4Hex("BF0D"));
        assertEquals(48909,  HexUtils.parse4Hex("BF0D"));
    }

    @Test
    @DisplayName("parse4Hex: case-insensitive and boundary values")
    void parse4Hex_caseAndBoundaries() {
        assertEquals(0x0000, HexUtils.parse4Hex("0000"));
        assertEquals(0xFFFF, HexUtils.parse4Hex("FFFF"));
        assertEquals(0xffff, HexUtils.parse4Hex("ffff"));
        assertEquals(0xABCD, HexUtils.parse4Hex("aBcD"));
    }

    @Test
    @DisplayName("parse4Hex: rejects wrong length and non-hex (truenorth C8)")
    void parse4Hex_rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex(null));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex(""));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex("123"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex("12345"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex("GHIJ"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex("$BF0D"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parse4Hex("0xBF"));
    }

    @Test
    @DisplayName("toHex4: formats 0x0000..0xFFFF as 4-char uppercase")
    void toHex4_roundTrip() {
        assertEquals("0000", HexUtils.toHex4(0));
        assertEquals("00FF", HexUtils.toHex4(0xFF));
        assertEquals("BF0D", HexUtils.toHex4(48909));
        assertEquals("FFFF", HexUtils.toHex4(0xFFFF));
    }

    @Test
    @DisplayName("toHex4: rejects out-of-range")
    void toHex4_rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.toHex4(-1));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.toHex4(0x10000));
    }

    @Test
    @DisplayName("toDecimalAscii: AE argument form for BF0D is \"48909\" (truenorth A2)")
    void toDecimalAscii_canonicalAEArg() {
        assertEquals("48909", HexUtils.toDecimalAscii(0xBF0D));
        assertEquals("0",     HexUtils.toDecimalAscii(0));
        assertEquals("65535", HexUtils.toDecimalAscii(0xFFFF));
    }

    @Test
    @DisplayName("toAsciiHexPair: 0x3F -> {'3','F'} (MM payload form, truenorth A1)")
    void toAsciiHexPair_basic() {
        assertArrayEquals(new byte[]{'3', 'F'}, HexUtils.toAsciiHexPair(0x3F));
        assertArrayEquals(new byte[]{'0', '0'}, HexUtils.toAsciiHexPair(0x00));
        assertArrayEquals(new byte[]{'F', 'F'}, HexUtils.toAsciiHexPair(0xFF));
        assertArrayEquals(new byte[]{'A', 'B'}, HexUtils.toAsciiHexPair(0xAB));
    }

    @Test
    @DisplayName("fromAsciiHexPair: inverse of toAsciiHexPair")
    void fromAsciiHexPair_roundTrip() {
        for (int v = 0; v < 256; v++) {
            byte[] pair = HexUtils.toAsciiHexPair(v);
            int decoded = HexUtils.fromAsciiHexPair(pair[0] & 0xFF, pair[1] & 0xFF);
            assertEquals(v, decoded, "round trip failed for byte " + v);
        }
    }

    @Test
    @DisplayName("fromAsciiHexPair: accepts lowercase and rejects malformed")
    void fromAsciiHexPair_caseAndErrors() {
        assertEquals(0x3F, HexUtils.fromAsciiHexPair('3', 'f'));
        assertEquals(0xAB, HexUtils.fromAsciiHexPair('a', 'b'));
        assertThrows(IllegalArgumentException.class,
                () -> HexUtils.fromAsciiHexPair('G', '0'));
        assertThrows(IllegalArgumentException.class,
                () -> HexUtils.fromAsciiHexPair('0', 'Z'));
    }

    @Test
    @DisplayName("printableOrDot: 0x20..0x7E printable, else '.' (truenorth C5)")
    void printableOrDot_boundary() {
        assertEquals('.', HexUtils.printableOrDot(0x00));
        assertEquals('.', HexUtils.printableOrDot(0x1F));
        assertEquals(' ', HexUtils.printableOrDot(0x20));
        assertEquals('A', HexUtils.printableOrDot(0x41));
        assertEquals('~', HexUtils.printableOrDot(0x7E));
        assertEquals('.', HexUtils.printableOrDot(0x7F));
        assertEquals('.', HexUtils.printableOrDot(0x80));
        assertEquals('.', HexUtils.printableOrDot(0xFF));
    }

    @Test
    @DisplayName("bytesToSpacedHex: matches the canonical OGG probe vector")
    void bytesToSpacedHex_oggProbe() {
        byte[] probe = new byte[] { 0x01, 0x4F, 0x47, 0x47, 0x17 };
        assertEquals("01 4F 47 47 17", HexUtils.bytesToSpacedHex(probe));
    }

    @Test
    @DisplayName("bytesToSpacedHex: empty input -> empty string")
    void bytesToSpacedHex_empty() {
        assertEquals("", HexUtils.bytesToSpacedHex(new byte[0]));
        assertEquals("", HexUtils.bytesToSpacedHex(null));
    }

    @Test
    @DisplayName("bytesToAsciiRender: SOH/ETB dotted, OMM visible (.OMM.)")
    void bytesToAsciiRender_mmEnvelope() {
        byte[] mm = new byte[] { 0x01, 0x4F, 0x4D, 0x4D, 0x17 };
        assertEquals(".OMM.", HexUtils.bytesToAsciiRender(mm));
    }

    @Test
    @DisplayName("bytesToAsciiRender: offset + length honored")
    void bytesToAsciiRender_offsetLength() {
        byte[] data = "xABCy".getBytes();
        assertEquals("ABC", HexUtils.bytesToAsciiRender(data, 1, 3));
    }
}
