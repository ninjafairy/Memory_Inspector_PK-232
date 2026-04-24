# PK-232 Host Mode Reference (Chapter 4 Extract)

Source basis: `AEA-PK-232-TechnicalReferenceManual.pdf`, Chapter 4 "Host Mode and Special Applications", with **Raw HDLC (4.6)** and **KISS (4.7.x)** intentionally excluded.

---

## Overview

Host Mode is the PK-232 machine-oriented protocol that replaces human/verbose command dialog with compact framed blocks over RS-232.

### Purpose of Host Mode
- Reduce human-readable parsing overhead for host software.
- Provide deterministic block framing for command/status/data.
- Support software-driven control flows (mailbox, BBS, automation, custom protocol stacks).

### Host Mode vs COMMAND/Verbose mode
- **COMMAND/Verbose mode**
  - Human-readable strings.
  - Mixed asynchronous status text and user interaction.
  - Harder for software to parse reliably.
- **Host Mode**
  - Binary-framed blocks: `SOH CTL DATA ETB`.
  - Explicit channel and block types via CTL byte.
  - Compact responses and numeric status/error codes.

### General protocol rules
- Framing:
  - `SOH = 0x01` (start of block)
  - `ETB = 0x17` (end of block)
  - `DLE = 0x10` (escape/pass byte)
- Escaping:
  - If payload contains `SOH`, `DLE`, or `ETB`, prefix that byte with `DLE`.
- Host command convention:
  - Two-letter mnemonic command tokens (ASCII), optional ASCII arguments.
- No carriage return inside Host-mode blocks; block terminates at `ETB`.
- In Packet multiconnect, channels are `0-9`; non-Packet modes effectively use channel `0`.

### Entering Host Mode
Set terminal and PK-232 link to 8-bit/no-parity before `HOST ON`:
- `AWLEN 8`
- `PARITY 0`
- `8BITCONV ON`
- `RESTART`
- `HOST ON`

Recommended robust entry sequence from host software:
- Send synchronization/command priming.
- Then verify by sending:
  - `01 4F 47 47 17` (`SOH 'O' 'G' 'G' ETB`)
- Expected success response:
  - `01 4F 47 47 00 17`

### Leaving Host Mode
- Send block equivalent of `HO N` / host-off request:
  - `01 4F 48 4F 4E 17`
- Or send serial BREAK (>=300 ms SPACE polarity).

### Recovery behavior
If host link state is uncertain, send a command block with **double SOH** to force parser resync, e.g.:
- `01 01 4F 47 47 17`

---

## Packet Structure

### Framing

| Field | Bytes | Value/Type | Notes |
|---|---:|---|---|
| Start | 1 | `SOH` (`0x01`) | Start of host block |
| Control | 1 | `CTL` | Defines block class + channel |
| Payload | 0..N | ASCII/byte data | Mnemonic + args, channel data, status text, etc. |
| End | 1 | `ETB` (`0x17`) | End of host block |

**Canonical form**

```text
[SOH][CTL][payload...][ETB]
```

### Escape handling

When payload contains any reserved byte (`SOH`, `DLE`, `ETB`), prefix that byte with `DLE`.

Example payload bytes:

```text
04 01 05 10 12
```

Framed on channel 0 data block:

```text
01 20 04 10 01 05 10 10 12 17
```

### CTL byte classes

| CTL | Direction | Meaning |
|---|---|---|
| `0x2x` | Host -> PK-232 | Data to channel `x` |
| `0x4x` | Host -> PK-232 | Command to channel `x` |
| `0x4F` | Host -> PK-232 | Global command/no channel switch |
| `0x2F` | PK-232 -> Host | Echoed data (Morse/Baudot/ASCII/AMTOR cases) |
| `0x3x` | PK-232 -> Host | Data from channel `x` |
| `0x3F` | PK-232 -> Host | Monitored frames/data |
| `0x4x` | PK-232 -> Host | Link status from channel `x` |
| `0x4F` | PK-232 -> Host | Command response |
| `0x5x` | PK-232 -> Host | Link messages from channel `x` |
| `0x5F` | PK-232 -> Host | Status/errors |

### Length and size notes (non-RawHDLC/KISS context)
- Host -> PK-232 block max: **330 chars** (excluding SOH/CTL/DLE/ETB).
- PK-232 -> Host max depends on mode:
  - Text modes may segment to <=256-byte chunks (payload basis).
  - Packet monitored worst case may be substantially larger due to stamp/options + escaping (documented up to 648 payload bytes in extreme case).

### Checksum
- Host Mode Chapter 4 framing itself does **not define a per-block checksum field** in `SOH/CTL/.../ETB`.
- Error signaling occurs by:
  - command response code byte (`c`)
  - `0x5F` error/status blocks
  - protocol-level behavior (polling/ack timing).

---

## Command Reference

This section covers Host Mode protocol/transport commands and Chapter-4-defined high-impact host operations.

### Command: Data Block to Channel (`CTL = 0x2x`)

| Field | Value |
|---|---|
| Command name / byte | Data to channel `x` / `0x2x` |
| Parameters | `x` channel nibble (`0-9`); payload bytes |
| Response format | Deferred; host polls and receives `0x5F ... 00` data-ack when accepted |
| Description | Sends user data to PK-232 channel. Non-Packet modes generally use channel `0`. |
| Notes / errors | Host should wait for data acknowledgment before next send to avoid overrun. |

Example:

```text
01 20 [data...] 17
```

---

### Command: Global Host Command (`CTL = 0x4F`)

| Field | Value |
|---|---|
| Command name / byte | Command block / `0x4F` |
| Parameters | Two-letter mnemonic + optional ASCII args |
| Response format | `01 4F a b c 17` |
| Description | Primary host command transport (no channel switch side effect). |
| Notes / errors | `c` is command completion/error code (table below). |

Command response `c` values:

| Code (`c`) | Meaning |
|---|---|
| `0x00` | Acknowledge, no error |
| `0x01` | bad |
| `0x02` | too many |
| `0x03` | not enough |
| `0x04` | too long |
| `0x05` | range |
| `0x06` | callsign |
| `0x07` | unknown command |
| `0x08` | VIA |
| `0x09` | not while connected |
| `0x0A` | need MYCALL |
| `0x0B` | need MYSELCAL |
| `0x0C` | already connected |
| `0x0D` | not while disconnected |
| `0x0E` | different connects |
| `0x0F` | too many packets outstanding |
| `0x10` | clock not set |
| `0x11` | need ALL/NONE/YES/NO |
| `0x15` | not in this mode |

Additional parser-level errors:
- `01 5F X X W 17` -> bad block
- `01 5F X X Y 17` -> bad CTL in block

---

### Command: Poll (`GG`)

| Field | Value |
|---|---|
| Command name / byte | Poll / mnemonic `GG` (`CTL 0x4F`) |
| Parameters | none |
| Response format | `01 4F 47 47 00 17` if nothing pending; otherwise one pending block per poll |
| Description | Fetch one queued event/data/status block from PK-232. |
| Notes / errors | Exactly one block returned per poll; keep polling loop active when `HPOLL ON`. |

Possible returned block classes:
- `0x2F` echoed data
- `0x3x` channel data
- `0x3F` monitored data
- `0x4x` link status
- `0x5x` link message
- `0x5F` status/error/data-ack

---

### Command: Host Poll Mode (`HPOLL`)

| Field | Value |
|---|---|
| Command name / byte | `HP` / Host Poll behavior |
| Parameters | `Y`/`N` (or equivalent command switch semantics) |
| Response format | Standard command response; query returns current state |
| Description | Controls whether host must poll for all outputs. |
| Notes / errors | `HPOLL ON`: host must poll (`GG`). `HPOLL OFF`: PK-232 pushes blocks when formed. |

---

### Command: Connect / Disconnect (`CO`, `DI`)

| Field | Value |
|---|---|
| Command name / byte | `CO` / `DI` sent with `CTL=0x4x` per channel |
| Parameters | Channel `x` and standard connect/disconnect args |
| Response format | Standard command response + link status/message blocks |
| Description | Per-channel link management in Packet multiconnect. |
| Notes / errors | Querying `CO` also returns structured link-status payload. |

---

### Command: Query Command Pattern

| Field | Value |
|---|---|
| Command name / byte | Any 2-letter mnemonic query (no args) |
| Parameters | none |
| Response format | `01 4F a b value... 17` |
| Description | Reads current command/parameter value. |
| Notes / errors | Boolean values returned as `Y`/`N`; `CONMODE` returns `C` or `T`. |

---

### Command: OPMODE Query (`OP`)

| Field | Value |
|---|---|
| Command name / byte | `OP` |
| Parameters | none |
| Response format | Mode-specific block signatures (`PA`, `MO`, `BA`, `AS`, `AM`, `AC`, `AL`, `FE`, `SE`, `FA` patterns) |
| Description | Returns current PK-232 operating mode and mode state indicators. |
| Notes / errors | Parse as tagged mode tuple; do not hardcode fixed length per mode. |

---

### Command: Link Status Query via CONNECT (`CO` query form)

| Field | Value |
|---|---|
| Command name / byte | `CO` query on channel `x` |
| Parameters | none (query form) |
| Response format | `01 4x 43 4F a b c d e path 17` |
| Description | Returns link state, protocol revision, outstanding packets, retry count, CONPERM, and path text. |
| Notes / errors | Fields `a..e` are nibble-like values represented as ASCII-coded hex (`ORed with 0x30`). |

---

### Command: ADDRESS / MEMORY / I-O (`AE`, `MM`, `IO`)

| Field | Value |
|---|---|
| Command name / byte | `AE`, `MM`, `IO` |
| Parameters | ADDRESS sets target; MEMORY/I-O read/write values are ASCII numeric/hex command args |
| Response format | Standard command response + value outputs on read |
| Description | Low-level memory and device-register access for diagnostics/development. |
| Notes / errors | `MEMORY` auto-increments ADDRESS after each operation; `I/O` does not increment ADDRESS. |

Documented address examples:
- `0xBF0D` parallel port A
- `0xBF0E` parallel port B
- `0xBF0F` parallel port C
- `0x7C08` terminal data
- `0x7E08` HDLC data
- `0x7E00` HDLC RR0 status

---

### Command: MHEARD line polling (`MH0..MH17`)

| Field | Value |
|---|---|
| Command name / byte | `MH` with line index ASCII suffix |
| Parameters | line index `0..17` |
| Response format | per-line result block; empty `... 00 ...` when exhausted |
| Description | Host-mode-safe MHEARD retrieval in line fragments due to buffer limits. |
| Notes / errors | Poll sequentially; list can change mid-read if frames arrive. |

---

### Host Mnemonics (as documented in Chapter 4)

The chapter documents a broad 2-letter mnemonic map for command transport. Representative sample:

| Mnemonic | Command |
|---|---|
| `CO` | CONNECT |
| `DI` | DISCONNECT |
| `TD` | TXDELAY |
| `SP` | SENDPAC |
| `HP` | HPOLL |
| `OP` | OPMODE |
| `AE` | ADDRESS |
| `MM` | MEMORY |
| `IO` | I/O |
| `MH` | MHEARD |
| `HO` | HOST |

The manual provides the full mnemonic index in section 4.2.2 (used by Host-mode command encoding).

---

## Flow Control & State Handling

### Poll-driven flow
- With `HPOLL ON`:
  - Host drives output retrieval using `GG` poll blocks.
  - One queued output block returned per poll.
- With `HPOLL OFF`:
  - PK-232 may push output blocks as they form.

### Data-send pacing
- Sending data block (`0x2x`) does not produce immediate command ACK.
- Host should wait for data acknowledgment block:
  - `01 5F X X 00 17`
- If PK-232 is busy, this acknowledgment is delayed.

### Channel handling
- Packet mode supports channels `0..9` (multiconnect).
- Other modes usually use channel `0`.

### Mode interactions
- `CONMODE` affects Host-mode data behavior:
  - `CONV`: `8BITCONV`, `ALFPACK`, `ALFDISP`, `LOCK`, `ESCAPE` active.
  - `TRANS`: pass-through behavior with minimal character modification.
- Do not issue human-mode `CONVERSE`/`TRANS` commands directly in Host-mode flow.

---

## Error Handling & Special Cases

### Command/parse errors
- Always parse `0x4F` response code `c`.
- Handle parser-level bad-block and bad-CTL (`0x5F ... W/Y`) separately from command errors.

### Host parser resynchronization
- On framing uncertainty, transmit double-SOH recovery command form.
- Clear partial block accumulators on timeout/framing fault.

### Block escaping
- Implement bidirectional escaping for `SOH`, `DLE`, `ETB` in payload.
- Never treat escaped control byte as framing delimiter.

### AMTOR data special case (documented in 4.4.3)
- In certain AMTOR modes, data block class differs (`0x30` vs `0x3F` behavior patterns).
- Keep demux logic mode-aware if implementing non-Packet host features.

### MHEARD polling caveat
- Mid-poll list mutation can produce inconsistent snapshots.
- If strict consistency is required, temporarily quiesce packet intake before full read.

---

## Recommended Java Implementation Notes

### Framing/deframing model
- Use a byte-oriented state machine:
  - `WAIT_SOH` -> `READ_CTL` -> `READ_PAYLOAD_UNTIL_ETB`
  - escape sub-state for `DLE`.
- Keep parser independent from Swing/UI threads.

### Suggested data types
- RX/TX buffers: `ByteArrayOutputStream` or reusable `byte[]` ring buffers.
- Block object:

```java
final class HostBlock {
    final int ctl;          // unsigned byte 0..255
    final byte[] payload;   // unescaped payload
}
```

### Unsigned byte handling

```java
int u = b & 0xFF;
```

### Encode helper (escape payload)

```java
private static final byte SOH = 0x01;
private static final byte DLE = 0x10;
private static final byte ETB = 0x17;

byte[] encodeBlock(int ctl, byte[] payload) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(SOH);
    out.write(ctl & 0xFF);
    for (byte b : payload) {
        if (b == SOH || b == DLE || b == ETB) out.write(DLE);
        out.write(b);
    }
    out.write(ETB);
    return out.toByteArray();
}
```

### Decode helper behavior
- Detect `SOH` block start.
- Consume CTL.
- Read bytes until unescaped `ETB`.
- If `DLE`, copy next byte verbatim into payload.
- On timeout/malformed stream, reset parser state and optionally send recovery command.

### Poll loop recommendation
- If using `HPOLL ON`, run scheduled poll task (e.g., every 20-100 ms, adaptive).
- Apply backpressure: do not flood `GG` if outstanding command/data ack waiters exist.

### Concurrency
- Serial IO thread:
  - parse blocks
  - dispatch events to protocol layer
- UI thread:
  - only render already-parsed events (`SwingUtilities.invokeLater`).

### Protocol safety checks
- Validate command response pattern (`0x4F`, matching mnemonic echo bytes when expected).
- Implement timeout and retry policy for:
  - command round-trip
  - data-ack wait after `0x2x` send
  - host-mode entry verification (`OGG` probe)

### Logging
- Log raw hex and decoded interpretation for each block:
  - timestamp
  - direction
  - CTL
  - payload text/hex
  - parse/command status code.

---

## Exclusion Note

Per request, this reference excludes detailed protocol extraction for:
- **4.6 Raw HDLC**
- **4.7 KISS TNC Asynchronous Packet Protocol (and 4.7.x)**

All other Chapter 4 host-mode material relevant to protocol operation, command transport, responses, polling, status, and special host interactions is included above in implementation-focused form.
