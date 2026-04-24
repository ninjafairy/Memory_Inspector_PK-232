package protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PK232Client}'s package-private payload validators
 * ({@code validateAEPayload} / {@code parseMMPayload}). These cover the
 * {@code setAddress} / {@code readOneByte} branches that matter — envelope
 * shape, status byte, and ASCII-hex-pair decode — without touching
 * {@link serial.SerialLink} or the client's reader/protocol threads.
 *
 * <p>Scope per truenorth §8 Change Log 2026-04-20 (M4): Option-3 relaxed
 * for M4, realized by exercising static helpers directly rather than
 * subclassing {@link PK232Client} with a scripted {@code sendAndAwait}.
 * {@link serial.SerialLink} + thread lifecycle stays hardware-gated.
 */
class PK232ClientHelperTests {

    // ------------------------------------------------------------
    // validateAEPayload — AE ack envelope contract
    // ------------------------------------------------------------

    @Test
    @DisplayName("validateAEPayload: canonical success 0x4F 'AE' 0x00 passes")
    void validateAE_success() throws IOException {
        HostBlock ok = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'A', 'E', 0x00 });
        PK232Client.validateAEPayload(ok); // no throw
    }

    @Test
    @DisplayName("validateAEPayload: 0x5F response (status/error class) is rejected")
    void validateAE_rejects_non_global_ctl() {
        HostBlock bad = new HostBlock(
                HostBlock.CTL_STATUS_ERROR,
                new byte[] { 'A', 'E', 0x00 });
        IOException ioe = assertThrows(IOException.class,
                () -> PK232Client.validateAEPayload(bad));
        assertTrue(ioe.getMessage().contains("0x4F"),
                "expected diagnostic to mention expected ctl, got: " + ioe.getMessage());
    }

    @Test
    @DisplayName("validateAEPayload: non-zero status byte throws (truenorth A6)")
    void validateAE_rejects_nonzero_status() {
        HostBlock bad = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'A', 'E', 0x05 });
        IOException ioe = assertThrows(IOException.class,
                () -> PK232Client.validateAEPayload(bad));
        assertTrue(ioe.getMessage().contains("05"),
                "expected diagnostic to mention the status byte, got: " + ioe.getMessage());
    }

    @Test
    @DisplayName("validateAEPayload: wrong mnemonic bytes or short payload → malformed")
    void validateAE_rejects_malformed_payload() {
        HostBlock wrongMnem = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'X', 'Y', 0x00 });
        assertThrows(IOException.class,
                () -> PK232Client.validateAEPayload(wrongMnem));

        HostBlock tooShort = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'A', 'E' });
        assertThrows(IOException.class,
                () -> PK232Client.validateAEPayload(tooShort));

        HostBlock tooLong = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'A', 'E', 0x00, 0x00 });
        assertThrows(IOException.class,
                () -> PK232Client.validateAEPayload(tooLong));
    }

    // ------------------------------------------------------------
    // parseMMPayload — MM response envelope + hex-pair decode
    // ------------------------------------------------------------

    @Test
    @DisplayName("parseMMPayload: canonical 0x4F 'MM$3F' → 0x3F (truenorth §5.9 example)")
    void parseMM_decodes_canonical_pair() throws IOException {
        HostBlock ok = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', '3', 'F' });
        assertEquals(0x3F, PK232Client.parseMMPayload(ok));
    }

    @Test
    @DisplayName("parseMMPayload: hardware trace 'MM$01' → 0x01 (truenorth §8 2026-04-21 MM fix)")
    void parseMM_decodes_hardware_observed_example() throws IOException {
        // Exact payload from the bug-report RX line:
        // RX MM  01 4F 4D 4D 24 30 31 17  .OMM$01.
        HostBlock ok = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 0x4D, 0x4D, 0x24, 0x30, 0x31 });
        assertEquals(0x01, PK232Client.parseMMPayload(ok));
    }

    @Test
    @DisplayName("parseMMPayload: 0x00 and 0xFF boundaries decode correctly")
    void parseMM_decodes_boundary_values() throws IOException {
        HostBlock zero = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', '0', '0' });
        HostBlock ff = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', 'F', 'F' });
        assertEquals(0x00, PK232Client.parseMMPayload(zero));
        assertEquals(0xFF, PK232Client.parseMMPayload(ff));
    }

    @Test
    @DisplayName("parseMMPayload: lowercase hex chars accepted (HexUtils tolerates both cases)")
    void parseMM_accepts_lowercase_hex() throws IOException {
        HostBlock mixed = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', 'a', 'b' });
        assertEquals(0xAB, PK232Client.parseMMPayload(mixed));
    }

    @Test
    @DisplayName("parseMMPayload: non-hex chars in payload throw with diagnostic")
    void parseMM_rejects_non_hex_chars() {
        HostBlock junk = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', 'G', 'H' });
        IOException ioe = assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(junk));
        assertTrue(ioe.getMessage().contains("non-hex")
                        || ioe.getCause() instanceof IllegalArgumentException,
                "expected non-hex diagnostic or wrapped IAE, got: " + ioe);
    }

    @Test
    @DisplayName("parseMMPayload: missing '$' separator is rejected as malformed")
    void parseMM_rejects_missing_separator() {
        // Pre-fix canonical shape (no '$') — guards against regression.
        HostBlock noSeparator = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '3', 'F' });
        IOException ioe = assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(noSeparator));
        assertTrue(ioe.getMessage().contains("malformed"),
                "expected 'malformed payload' diagnostic, got: " + ioe.getMessage());
    }

    @Test
    @DisplayName("parseMMPayload: wrong ctl / wrong mnemonic / wrong length all rejected")
    void parseMM_rejects_malformed_envelope() {
        HostBlock wrongCtl = new HostBlock(
                HostBlock.CTL_STATUS_ERROR,
                new byte[] { 'M', 'M', '$', '3', 'F' });
        assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(wrongCtl));

        HostBlock wrongMnem = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'X', 'X', '$', '3', 'F' });
        assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(wrongMnem));

        HostBlock tooShort = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', '3' });
        assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(tooShort));

        HostBlock tooLong = new HostBlock(
                HostBlock.CTL_GLOBAL_COMMAND,
                new byte[] { 'M', 'M', '$', '3', 'F', 0x00 });
        assertThrows(IOException.class,
                () -> PK232Client.parseMMPayload(tooLong));
    }
}
