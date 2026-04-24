package protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HostBlock}. Covers defensive copying, unsigned-byte
 * ctl-range enforcement, value-based equality, and the convenience predicates
 * / factories used by higher layers.
 */
class HostBlockTests {

    @Test
    @DisplayName("constructor: defensive-copies payload on the way in")
    void defensiveCopyOnConstruction() {
        byte[] payload = {0x47, 0x47};
        HostBlock block = new HostBlock(HostBlock.CTL_GLOBAL_COMMAND, payload);

        payload[0] = 0x00;

        assertEquals(0x47, block.payloadAt(0));
        assertEquals(0x47, block.payloadAt(1));
    }

    @Test
    @DisplayName("payloadCopy: defensive-copies payload on the way out")
    void defensiveCopyOnAccess() {
        HostBlock block = new HostBlock(HostBlock.CTL_GLOBAL_COMMAND, new byte[]{0x41, 0x45});

        byte[] copy = block.payloadCopy();
        copy[0] = 0x00;

        assertEquals(0x41, block.payloadAt(0));
    }

    @Test
    @DisplayName("ctl: accepts 0x00..0xFF and rejects outside range")
    void ctlRange() {
        assertDoesNotThrow(() -> new HostBlock(0x00, new byte[0]));
        assertDoesNotThrow(() -> new HostBlock(0xFF, new byte[0]));

        assertThrows(IllegalArgumentException.class, () -> new HostBlock(-1,    new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new HostBlock(0x100, new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new HostBlock(0xDEAD, new byte[0]));
    }

    @Test
    @DisplayName("constructor: rejects null payload")
    void rejectsNullPayload() {
        assertThrows(NullPointerException.class,
                () -> new HostBlock(HostBlock.CTL_GLOBAL_COMMAND, null));
    }

    @Test
    @DisplayName("ofAscii: rejects non-ASCII input")
    void ofAsciiRejectsNonAscii() {
        assertThrows(IllegalArgumentException.class,
                () -> HostBlock.ofAscii(HostBlock.CTL_GLOBAL_COMMAND, "héllo"));
    }

    @Test
    @DisplayName("ofAscii: GG produces the canonical probe payload bytes")
    void ofAsciiCanonical() {
        HostBlock gg = HostBlock.ofAscii(HostBlock.CTL_GLOBAL_COMMAND, "GG");
        assertEquals(0x4F, gg.ctl());
        assertTrue(gg.payloadEquals(new byte[]{0x47, 0x47}));
    }

    @Test
    @DisplayName("equals/hashCode: value-based and consistent")
    void equalityIsValueBased() {
        HostBlock a = new HostBlock(0x4F, new byte[]{0x47, 0x47, 0x00});
        HostBlock b = new HostBlock(0x4F, new byte[]{0x47, 0x47, 0x00});
        HostBlock c = new HostBlock(0x4F, new byte[]{0x47, 0x47});       // different payload
        HostBlock d = new HostBlock(0x5F, new byte[]{0x47, 0x47, 0x00}); // different ctl

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(null, a);
        assertNotEquals("not a HostBlock", a);
    }

    @Test
    @DisplayName("predicates: isGlobalCommand / isStatusOrError")
    void classPredicates() {
        assertTrue (new HostBlock(0x4F, new byte[0]).isGlobalCommand());
        assertFalse(new HostBlock(0x4F, new byte[0]).isStatusOrError());

        assertTrue (new HostBlock(0x5F, new byte[0]).isStatusOrError());
        assertFalse(new HostBlock(0x5F, new byte[0]).isGlobalCommand());

        assertFalse(new HostBlock(0x3F, new byte[0]).isGlobalCommand());
        assertFalse(new HostBlock(0x3F, new byte[0]).isStatusOrError());
    }

    @Test
    @DisplayName("toString: shows uppercase hex CTL + spaced payload bytes")
    void toStringFormat() {
        HostBlock b = new HostBlock(0x4F, new byte[]{0x47, 0x47, 0x00});
        assertEquals("HostBlock{ctl=0x4F, payload=[47 47 00]}", b.toString());
    }
}
