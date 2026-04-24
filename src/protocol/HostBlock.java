package protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable value object for a single PK-232 Host-Mode block per
 * {@code PK232_HostMode_Reference.md} §Packet Structure:
 * <pre>[SOH][CTL][payload...][ETB]</pre>
 *
 * <p>{@link #ctl()} is the raw CTL byte as an unsigned int (0..255); the
 * payload stored here is always <em>unescaped</em> — {@code DLE} escape
 * handling is the codec's concern, not this VO's. Payload bytes are defensively
 * copied on construction and on {@link #payloadCopy()} so the instance stays
 * immutable.
 */
public final class HostBlock {

    /** Host -> PK-232 global command (no channel switch side effect). */
    public static final int CTL_GLOBAL_COMMAND      = 0x4F;
    /** PK-232 -> Host command response (same encoding pattern as 0x4F TX). */
    public static final int CTL_GLOBAL_RESPONSE     = 0x4F;
    /** PK-232 -> Host status / errors / data-ack. */
    public static final int CTL_STATUS_ERROR        = 0x5F;
    /** PK-232 -> Host echoed data (Morse/Baudot/ASCII/AMTOR cases). */
    public static final int CTL_ECHOED_DATA         = 0x2F;
    /** PK-232 -> Host monitored frames/data. */
    public static final int CTL_MONITORED_DATA      = 0x3F;

    private final int ctl;
    private final byte[] payload;

    public HostBlock(int ctl, byte[] payload) {
        if ((ctl & ~0xFF) != 0) {
            throw new IllegalArgumentException("ctl must fit in an unsigned byte: 0x"
                    + Integer.toHexString(ctl));
        }
        Objects.requireNonNull(payload, "payload");
        this.ctl = ctl;
        this.payload = payload.clone();
    }

    /** Convenience factory for a zero-payload block (e.g. trivial mnemonic probes). */
    public static HostBlock of(int ctl) {
        return new HostBlock(ctl, new byte[0]);
    }

    /** Convenience factory taking a mnemonic-style ASCII string (e.g. {@code "GG"}, {@code "MM"}). */
    public static HostBlock ofAscii(int ctl, String asciiPayload) {
        Objects.requireNonNull(asciiPayload, "asciiPayload");
        byte[] bytes = new byte[asciiPayload.length()];
        for (int i = 0; i < bytes.length; i++) {
            char c = asciiPayload.charAt(i);
            if (c > 0x7F) {
                throw new IllegalArgumentException(
                        "non-ASCII char at index " + i + ": U+" + Integer.toHexString(c));
            }
            bytes[i] = (byte) c;
        }
        return new HostBlock(ctl, bytes);
    }

    public int ctl() {
        return ctl;
    }

    public int payloadLength() {
        return payload.length;
    }

    public byte payloadAt(int index) {
        return payload[index];
    }

    /** Defensive copy — callers cannot mutate the block. */
    public byte[] payloadCopy() {
        return payload.clone();
    }

    /**
     * Byte-for-byte compare against {@code other} without exposing the
     * internal array or forcing callers to construct their own copy.
     */
    public boolean payloadEquals(byte[] other) {
        return other != null && Arrays.equals(payload, other);
    }

    /** True for {@code CTL == 0x4F} — global command transport / response class. */
    public boolean isGlobalCommand() {
        return ctl == CTL_GLOBAL_COMMAND;
    }

    /** True for {@code CTL == 0x5F} — status / error / data-ack class. */
    public boolean isStatusOrError() {
        return ctl == CTL_STATUS_ERROR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostBlock other)) return false;
        return ctl == other.ctl && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        return 31 * ctl + Arrays.hashCode(payload);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16 + payload.length * 3);
        sb.append("HostBlock{ctl=0x");
        appendHexByte(sb, ctl);
        sb.append(", payload=[");
        for (int i = 0; i < payload.length; i++) {
            if (i > 0) sb.append(' ');
            appendHexByte(sb, payload[i] & 0xFF);
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void appendHexByte(StringBuilder sb, int unsignedByte) {
        String h = Integer.toHexString(unsignedByte).toUpperCase();
        if (h.length() == 1) sb.append('0');
        sb.append(h);
    }
}
