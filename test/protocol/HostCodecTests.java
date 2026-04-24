package protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HostCodec}. Covers the locked TX/RX test vectors
 * from truenorth §7 Pre-Code Actions, the escape-handling example from
 * {@code PK232_HostMode_Reference.md} §Escape handling, and the streaming
 * parser's resync / chunking / runaway-safety properties.
 */
class HostCodecTests {

    // --- canonical TX vectors (truenorth §7) ---

    @Test
    @DisplayName("encode: OGG probe -> 01 4F 47 47 17")
    void encode_oggProbe() {
        assertArrayEquals(
                bytes(0x01, 0x4F, 0x47, 0x47, 0x17),
                HostCodec.encode(0x4F, bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("encode: HON -> 01 4F 48 4F 4E 17")
    void encode_hon() {
        assertArrayEquals(
                bytes(0x01, 0x4F, 0x48, 0x4F, 0x4E, 0x17),
                HostCodec.encode(0x4F, bytes(0x48, 0x4F, 0x4E)));
    }

    @Test
    @DisplayName("encode: AE 48909 (BF0D decimal) -> 01 4F 41 45 34 38 39 30 39 17")
    void encode_aeCanonical() {
        assertArrayEquals(
                bytes(0x01, 0x4F, 0x41, 0x45, 0x34, 0x38, 0x39, 0x30, 0x39, 0x17),
                HostCodec.encode(0x4F, "AE48909".getBytes()));
    }

    @Test
    @DisplayName("encode: MM (no args) -> 01 4F 4D 4D 17")
    void encode_mm() {
        assertArrayEquals(
                bytes(0x01, 0x4F, 0x4D, 0x4D, 0x17),
                HostCodec.encode(0x4F, bytes(0x4D, 0x4D)));
    }

    @Test
    @DisplayName("encode(HostBlock): forwards ctl + payload")
    void encode_fromHostBlock() {
        HostBlock b = HostBlock.ofAscii(0x4F, "GG");
        assertArrayEquals(bytes(0x01, 0x4F, 0x47, 0x47, 0x17), HostCodec.encode(b));
    }

    // --- escape semantics (reference §Escape handling) ---

    @Test
    @DisplayName("encode: spec example 04 01 05 10 12 on CTL 0x20 matches the reference exactly")
    void encode_escapeReferenceExample() {
        byte[] payload = bytes(0x04, 0x01, 0x05, 0x10, 0x12);
        byte[] expected = bytes(0x01, 0x20, 0x04, 0x10, 0x01, 0x05, 0x10, 0x10, 0x12, 0x17);
        assertArrayEquals(expected, HostCodec.encode(0x20, payload));
    }

    @Test
    @DisplayName("encode: SOH/DLE/ETB together -> all three DLE-escaped")
    void encode_allThreeEscaped() {
        byte[] payload = bytes(0x01, 0x10, 0x17);
        byte[] expected = bytes(0x01, 0x4F, 0x10, 0x01, 0x10, 0x10, 0x10, 0x17, 0x17);
        assertArrayEquals(expected, HostCodec.encode(0x4F, payload));
    }

    @Test
    @DisplayName("encode: rejects null payload and out-of-range ctl")
    void encode_errorPaths() {
        assertThrows(NullPointerException.class,     () -> HostCodec.encode(0x4F, null));
        assertThrows(IllegalArgumentException.class, () -> HostCodec.encode(-1,    new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> HostCodec.encode(0x100, new byte[0]));
    }

    // --- canonical RX vectors (truenorth §7) ---

    @Test
    @DisplayName("decodeAll: OGG ack 01 4F 47 47 00 17 -> ctl=0x4F, payload=47 47 00")
    void decode_oggAck() {
        List<HostBlock> blocks = HostCodec.decodeAll(bytes(0x01, 0x4F, 0x47, 0x47, 0x00, 0x17));
        assertEquals(1, blocks.size());
        assertEquals(0x4F, blocks.get(0).ctl());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x47, 0x47, 0x00)));
    }

    @Test
    @DisplayName("decodeAll: AE ack 01 4F 41 45 00 17 -> ctl=0x4F, payload=41 45 00 (truenorth A6)")
    void decode_aeAck() {
        List<HostBlock> blocks = HostCodec.decodeAll(bytes(0x01, 0x4F, 0x41, 0x45, 0x00, 0x17));
        assertEquals(1, blocks.size());
        assertEquals(0x4F, blocks.get(0).ctl());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x41, 0x45, 0x00)));
    }

    @Test
    @DisplayName("decodeAll: MM read 01 4F 4D 4D 33 46 17 -> payload 4D 4D 33 46 (0x3F byte)")
    void decode_mmRead3F() {
        List<HostBlock> blocks = HostCodec.decodeAll(bytes(0x01, 0x4F, 0x4D, 0x4D, 0x33, 0x46, 0x17));
        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x4D, 0x4D, 0x33, 0x46)));
    }

    @Test
    @DisplayName("decodeAll: 0x5F status block preserved intact")
    void decode_statusErrorBlock() {
        List<HostBlock> blocks = HostCodec.decodeAll(bytes(0x01, 0x5F, 0x42, 0x42, 0x00, 0x17));
        assertEquals(1, blocks.size());
        assertEquals(0x5F, blocks.get(0).ctl());
        assertTrue(blocks.get(0).isStatusOrError());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x42, 0x42, 0x00)));
    }

    // --- encode/decode round-trip ---

    @Test
    @DisplayName("round-trip: bare SOH in payload survives encode+decode")
    void roundTrip_bareSoh() {
        assertRoundTrip(0x4F, bytes(0x01));
    }

    @Test
    @DisplayName("round-trip: bare DLE in payload survives encode+decode")
    void roundTrip_bareDle() {
        assertRoundTrip(0x4F, bytes(0x10));
    }

    @Test
    @DisplayName("round-trip: bare ETB in payload survives encode+decode")
    void roundTrip_bareEtb() {
        assertRoundTrip(0x4F, bytes(0x17));
    }

    @Test
    @DisplayName("round-trip: SOH + DLE + ETB sandwich survives encode+decode")
    void roundTrip_allThree() {
        assertRoundTrip(0x4F, bytes(0x01, 0x10, 0x17));
    }

    @Test
    @DisplayName("round-trip: every byte 0x00..0xFF in sequence survives encode+decode")
    void roundTrip_everyByte() {
        byte[] payload = new byte[256];
        for (int i = 0; i < 256; i++) {
            payload[i] = (byte) i;
        }
        assertRoundTrip(0x4F, payload);
    }

    // --- streaming / chunking / multiple blocks ---

    @Test
    @DisplayName("parser: feeding one byte at a time yields the same block as a single feed")
    void parser_byteAtATime() {
        byte[] wire = bytes(0x01, 0x4F, 0x47, 0x47, 0x00, 0x17);
        HostCodec.Parser p = HostCodec.newParser();

        HostBlock completed = null;
        int emittedCount = 0;
        for (byte b : wire) {
            List<HostBlock> out = p.feed(new byte[]{b});
            if (!out.isEmpty()) {
                emittedCount += out.size();
                completed = out.get(0);
            }
        }
        assertEquals(1, emittedCount);
        assertNotNull(completed);
        assertEquals(0x4F, completed.ctl());
        assertTrue(completed.payloadEquals(bytes(0x47, 0x47, 0x00)));
    }

    @Test
    @DisplayName("parser: two concatenated frames emerge as two blocks in order")
    void parser_twoConcatenatedFrames() {
        byte[] wire = bytes(
                0x01, 0x4F, 0x4D, 0x4D, 0x17,
                0x01, 0x4F, 0x48, 0x4F, 0x4E, 0x17);
        List<HostBlock> blocks = HostCodec.decodeAll(wire);

        assertEquals(2, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x4D, 0x4D)));
        assertTrue(blocks.get(1).payloadEquals(bytes(0x48, 0x4F, 0x4E)));
    }

    @Test
    @DisplayName("parser: feed honors off/len and ignores bytes outside the window")
    void parser_offsetLength() {
        byte[] padded = bytes(0xFF, 0xFF, 0x01, 0x4F, 0x47, 0x47, 0x17, 0xAA);
        HostCodec.Parser p = HostCodec.newParser();
        List<HostBlock> out = p.feed(padded, 2, 5);

        assertEquals(1, out.size());
        assertTrue(out.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("parser: empty input yields an empty list, parser stays in WAIT_SOH")
    void parser_emptyInput() {
        HostCodec.Parser p = HostCodec.newParser();
        assertTrue(p.feed(new byte[0]).isEmpty());
        assertEquals(HostCodec.Parser.State.WAIT_SOH, p.state());
    }

    @Test
    @DisplayName("parser: reset() drops a mid-frame accumulator")
    void parser_resetDropsPartial() {
        HostCodec.Parser p = HostCodec.newParser();
        p.feed(bytes(0x01, 0x4F, 0x41, 0x42));
        assertEquals(HostCodec.Parser.State.PAYLOAD, p.state());

        p.reset();
        assertEquals(HostCodec.Parser.State.WAIT_SOH, p.state());

        List<HostBlock> out = p.feed(bytes(0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, out.size());
        assertTrue(out.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    // --- resync behaviors (reference §Recovery behavior + §Host parser resynchronization) ---

    @Test
    @DisplayName("resync: double-SOH 01 01 4F 47 47 17 yields one OGG block")
    void resync_doubleSoh() {
        List<HostBlock> blocks = HostCodec.decodeAll(
                bytes(0x01, 0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, blocks.size());
        assertEquals(0x4F, blocks.get(0).ctl());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("resync: garbage before SOH is silently dropped")
    void resync_preFrameNoise() {
        List<HostBlock> blocks = HostCodec.decodeAll(
                bytes(0xFF, 0xFF, 0x99, 0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("resync: bare SOH mid-payload aborts partial frame and starts a new one")
    void resync_soHMidPayload() {
        List<HostBlock> blocks = HostCodec.decodeAll(
                bytes(0x01, 0x4F, 0x41, 0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("resync: bare ETB where a CTL was expected drops the frame and resyncs")
    void resync_etbWhereCtlExpected() {
        List<HostBlock> blocks = HostCodec.decodeAll(
                bytes(0x01, 0x17, 0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    @Test
    @DisplayName("parser: DLE-escaped SOH in PAYLOAD_ESCAPE is literal, not a frame restart")
    void escape_dleSohIsLiteral() {
        List<HostBlock> blocks = HostCodec.decodeAll(
                bytes(0x01, 0x4F, 0x41, 0x10, 0x01, 0x42, 0x17));
        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).payloadEquals(bytes(0x41, 0x01, 0x42)));
    }

    // --- safety cap ---

    @Test
    @DisplayName("parser: runaway accumulation beyond MAX_PAYLOAD_BYTES resets, then recovers")
    void parser_runawayThenRecover() {
        HostCodec.Parser p = HostCodec.newParser();

        p.feed(bytes(0x01, 0x4F));  // open a frame but never close it
        byte[] flood = new byte[HostCodec.MAX_PAYLOAD_BYTES + 1];
        for (int i = 0; i < flood.length; i++) flood[i] = 0x41;
        assertTrue(p.feed(flood).isEmpty(), "overflow must not emit a block");

        // The trailing ETB belongs to the aborted frame and is ignored.
        assertTrue(p.feed(bytes(0x17)).isEmpty());
        assertEquals(HostCodec.Parser.State.WAIT_SOH, p.state());

        List<HostBlock> recovery = p.feed(bytes(0x01, 0x4F, 0x47, 0x47, 0x17));
        assertEquals(1, recovery.size());
        assertTrue(recovery.get(0).payloadEquals(bytes(0x47, 0x47)));
    }

    // --- helpers ---

    private static byte[] bytes(int... ints) {
        byte[] out = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            out[i] = (byte) ints[i];
        }
        return out;
    }

    private static void assertRoundTrip(int ctl, byte[] payload) {
        byte[] wire = HostCodec.encode(ctl, payload);
        List<HostBlock> blocks = HostCodec.decodeAll(wire);
        assertEquals(1, blocks.size(), "exactly one block expected from a single encode");
        assertEquals(ctl, blocks.get(0).ctl());
        assertArrayEquals(payload, blocks.get(0).payloadCopy());
    }
}
