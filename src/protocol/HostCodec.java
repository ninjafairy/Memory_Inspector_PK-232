package protocol;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stateless encoder + stateful streaming parser for PK-232 Host-Mode blocks
 * per {@code PK232_HostMode_Reference.md} §Framing / §Escape handling.
 *
 * <p>Frame layout (on the wire):
 * <pre>[SOH=0x01][CTL][payload, DLE-escaped][ETB=0x17]</pre>
 * Any {@code SOH}, {@code DLE}, or {@code ETB} byte inside the payload is
 * prefixed with a literal {@code DLE} byte by the sender and un-escaped by
 * the receiver. The escape applies only inside the payload window — the
 * framing {@code SOH} / {@code ETB} themselves are always raw.
 *
 * <h2>Parser resynchronization</h2>
 * A bare {@code SOH} encountered while the parser is mid-frame ({@code READ_CTL}
 * or {@code PAYLOAD}) aborts the current accumulator and re-enters
 * {@code READ_CTL} with this {@code SOH} treated as the start of a fresh
 * block. This implements the double-SOH recovery form described in §Recovery
 * behavior ({@code 01 01 4F 47 47 17}) without any special-case code. In
 * {@code PAYLOAD_ESCAPE} state a {@code SOH} byte is treated as literal
 * payload per §Escape handling (bidirectional escape semantics).
 */
public final class HostCodec {

    public static final byte SOH = 0x01;
    public static final byte DLE = 0x10;
    public static final byte ETB = 0x17;

    /**
     * Hard cap on in-flight payload bytes before the parser aborts the
     * current block. The documented worst-case payload is ~648 bytes
     * (monitored Packet with stamps + escaping, §Length and size notes);
     * 4096 is a generous ~6x safety margin that still bounds memory on a
     * totally desynced stream.
     */
    public static final int MAX_PAYLOAD_BYTES = 4096;

    private HostCodec() {}

    /**
     * Encode one block onto the wire. {@code payload} is copied verbatim;
     * {@code SOH}/{@code DLE}/{@code ETB} bytes are {@code DLE}-escaped in
     * the output per §Escape handling.
     */
    public static byte[] encode(int ctl, byte[] payload) {
        if ((ctl & ~0xFF) != 0) {
            throw new IllegalArgumentException("ctl must fit in an unsigned byte: 0x"
                    + Integer.toHexString(ctl));
        }
        Objects.requireNonNull(payload, "payload");

        ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length + 4);
        out.write(SOH);
        out.write(ctl & 0xFF);
        for (byte b : payload) {
            if (b == SOH || b == DLE || b == ETB) {
                out.write(DLE);
            }
            out.write(b);
        }
        out.write(ETB);
        return out.toByteArray();
    }

    /** Convenience: encode {@code block.ctl()} + {@code block.payloadCopy()}. */
    public static byte[] encode(HostBlock block) {
        Objects.requireNonNull(block, "block");
        return encode(block.ctl(), block.payloadCopy());
    }

    /**
     * One-shot decode of a complete byte stream into zero or more blocks.
     * Intended for tests / trace replay — real serial I/O should use
     * {@link #newParser()} so partial input across {@code read()} calls is
     * handled correctly.
     */
    public static List<HostBlock> decodeAll(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return newParser().feed(bytes, 0, bytes.length);
    }

    public static Parser newParser() {
        return new Parser();
    }

    /**
     * Byte-oriented state machine. Call {@link #feed(byte[], int, int)} as
     * bytes arrive from the serial port; the returned list holds every
     * block that completed inside that call (possibly empty, possibly
     * multiple). Instances are single-threaded — one parser per serial
     * reader thread.
     */
    public static final class Parser {

        public enum State { WAIT_SOH, READ_CTL, PAYLOAD, PAYLOAD_ESCAPE }

        private State state = State.WAIT_SOH;
        private int currentCtl;
        private final ByteArrayOutputStream payloadBuf = new ByteArrayOutputStream(64);

        private Parser() {}

        public State state() {
            return state;
        }

        /** Reset to {@link State#WAIT_SOH} and drop any partial accumulator. */
        public void reset() {
            state = State.WAIT_SOH;
            currentCtl = 0;
            payloadBuf.reset();
        }

        public List<HostBlock> feed(byte[] buf) {
            Objects.requireNonNull(buf, "buf");
            return feed(buf, 0, buf.length);
        }

        public List<HostBlock> feed(byte[] buf, int off, int len) {
            Objects.requireNonNull(buf, "buf");
            Objects.checkFromIndexSize(off, len, buf.length);
            List<HostBlock> out = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                step(buf[off + i], out);
            }
            return out;
        }

        private void step(byte b, List<HostBlock> out) {
            switch (state) {
                case WAIT_SOH -> {
                    if (b == SOH) {
                        beginFrame();
                    }
                    // anything else is pre-frame noise and gets dropped
                }
                case READ_CTL -> {
                    if (b == SOH) {
                        // Double-SOH recovery — restart the frame.
                        beginFrame();
                    } else if (b == ETB) {
                        // Malformed: ETB where a CTL was expected. Drop and resync.
                        reset();
                    } else {
                        currentCtl = b & 0xFF;
                        state = State.PAYLOAD;
                    }
                }
                case PAYLOAD -> {
                    if (b == ETB) {
                        out.add(new HostBlock(currentCtl, payloadBuf.toByteArray()));
                        reset();
                    } else if (b == DLE) {
                        state = State.PAYLOAD_ESCAPE;
                    } else if (b == SOH) {
                        // Bare SOH mid-payload means the sender (or noise)
                        // started a new frame. Drop partial, resync as if
                        // this SOH just arrived in WAIT_SOH.
                        beginFrame();
                    } else {
                        appendOrAbort(b);
                    }
                }
                case PAYLOAD_ESCAPE -> {
                    // DLE-escaped byte is always literal, even SOH/DLE/ETB.
                    appendOrAbort(b);
                    state = State.PAYLOAD;
                }
            }
        }

        private void beginFrame() {
            currentCtl = 0;
            payloadBuf.reset();
            state = State.READ_CTL;
        }

        private void appendOrAbort(byte b) {
            if (payloadBuf.size() >= MAX_PAYLOAD_BYTES) {
                // Safety valve: runaway accumulation is almost certainly a
                // missed ETB on a desynced stream. Drop the in-flight
                // frame and wait for the next SOH.
                reset();
                return;
            }
            payloadBuf.write(b);
        }
    }
}
