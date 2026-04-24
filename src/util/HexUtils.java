package util;

/**
 * Hex / ASCII conversion helpers for the Memory Inspector protocol layer
 * and display layer. All methods treat bytes as unsigned (0..255).
 *
 * <p>Scope: just the conversions called out in truenorth.md Step 6.2.1 —
 * 4-hex to int (ADDRESS input), int to decimal ASCII (AE argument, per A2),
 * byte to ASCII-hex-pair and back (MM payload, per A1), and byte to
 * printable-or-dot for display and logging (C5, F3).
 */
public final class HexUtils {

    private HexUtils() {
    }

    /**
     * Parse exactly four hex characters ({@code 0-9 a-f A-F}) into an unsigned
     * 16-bit value 0x0000..0xFFFF. Case-insensitive. No {@code $} or {@code 0x}
     * prefix is accepted (per truenorth C8).
     */
    public static int parse4Hex(String s) {
        if (s == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (s.length() != 4) {
            throw new IllegalArgumentException("address must be exactly 4 hex chars, got " + s.length());
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int d = hexDigitValue(s.charAt(i));
            if (d < 0) {
                throw new IllegalArgumentException("non-hex character at index " + i + ": '" + s.charAt(i) + "'");
            }
            value = (value << 4) | d;
        }
        return value;
    }

    /**
     * Format an int in [0, 0xFFFF] as a 4-character uppercase hex string.
     */
    public static String toHex4(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("value out of 16-bit range: " + value);
        }
        char[] out = new char[4];
        for (int i = 3; i >= 0; i--) {
            out[i] = HEX_CHARS[value & 0xF];
            value >>>= 4;
        }
        return new String(out);
    }

    /**
     * Convert an unsigned int to its decimal ASCII representation, suitable
     * as the argument to the {@code AE} Host-Mode command (truenorth A2).
     */
    public static String toDecimalAscii(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative: " + value);
        }
        return Integer.toString(value);
    }

    /**
     * Encode one byte (interpreted as unsigned, low 8 bits) as a 2-character
     * uppercase ASCII hex pair. Returns a fresh {@code byte[2]} of ASCII codes,
     * e.g. {@code 0x3F} yields {@code {'3','F'} == {0x33, 0x46}} — the exact
     * wire form used in {@code MM} responses (truenorth A1).
     */
    public static byte[] toAsciiHexPair(int b) {
        int u = b & 0xFF;
        return new byte[] {
                (byte) HEX_CHARS[(u >>> 4) & 0xF],
                (byte) HEX_CHARS[u & 0xF]
        };
    }

    /**
     * Decode a 2-character ASCII hex pair (taken from a {@code MM} payload)
     * into an unsigned byte 0..255. Both upper- and lower-case are accepted.
     *
     * @param hi ASCII code of the high nibble character (e.g. {@code '3'})
     * @param lo ASCII code of the low  nibble character (e.g. {@code 'F'})
     */
    public static int fromAsciiHexPair(int hi, int lo) {
        int h = hexDigitValue((char) (hi & 0xFF));
        int l = hexDigitValue((char) (lo & 0xFF));
        if (h < 0 || l < 0) {
            throw new IllegalArgumentException(
                    "malformed ASCII-hex pair: 0x" + Integer.toHexString(hi & 0xFF)
                    + " 0x" + Integer.toHexString(lo & 0xFF));
        }
        return (h << 4) | l;
    }

    /**
     * Return the printable ASCII character for an unsigned byte, or {@code '.'}
     * for anything outside the printable range 0x20..0x7E (truenorth C5).
     */
    public static char printableOrDot(int b) {
        int u = b & 0xFF;
        return (u >= 0x20 && u <= 0x7E) ? (char) u : '.';
    }

    /**
     * Format a byte range as space-separated uppercase hex pairs, e.g.
     * {@code "01 4F 4D 4D 17"}. Used by {@link util.PacketLogger}.
     */
    public static String bytesToSpacedHex(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException(
                    "offset=" + offset + " length=" + length + " data.length=" + data.length);
        }
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length * 3 - 1);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int u = data[offset + i] & 0xFF;
            sb.append(HEX_CHARS[(u >>> 4) & 0xF]);
            sb.append(HEX_CHARS[u & 0xF]);
        }
        return sb.toString();
    }

    public static String bytesToSpacedHex(byte[] data) {
        return data == null ? "" : bytesToSpacedHex(data, 0, data.length);
    }

    /**
     * Render a byte range as a printable-or-dot ASCII string (truenorth C5/F3).
     */
    public static String bytesToAsciiRender(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException(
                    "offset=" + offset + " length=" + length + " data.length=" + data.length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(printableOrDot(data[offset + i]));
        }
        return sb.toString();
    }

    public static String bytesToAsciiRender(byte[] data) {
        return data == null ? "" : bytesToAsciiRender(data, 0, data.length);
    }

    private static int hexDigitValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
}
