# PK-232 Startup State Detection Specification

## 1. Overview

This document defines the startup detection logic that runs immediately after the serial port is opened (inside the initialization progress dialog) in `PactorRATT_Alpha`.

Goal: reliably determine the modem's current runtime state before normal application flow continues.

The detector must classify the PK-232 into exactly one of these states:

1. Powered OFF
2. AUTOBAUD prompt
3. Cmd: prompt
4. HOSTMODE

Detection output is used to decide the next startup action, user messaging, and recovery path.

---

## 2. Detection Strategy

Use an active probing strategy with short read windows and deterministic retries:

1. Flush stale RX data after opening the serial port.
2. Send a neutral wakeup sequence to trigger a recognizable response.
3. Read incoming bytes in small polling windows, normalize text, and match state signatures.
4. If no clear state appears, send targeted probes in a fixed order:
   - `Ctrl-C` to break to command mode
   - `*` for AUTOBAUD handshake test
5. Use bounded retries and explicit timeouts to avoid hanging startup.

Key principles:

- Always allow for noise/partial lines in serial input.
- Treat detection as successful only on strong signatures.
- Keep HOSTMODE detection conservative (avoid false positives).
- If conflicting evidence appears, continue probing until timeout or stable result.

---

## 3. Detailed Detection for Each State

## 3.1 Powered OFF

- Expected modem response / behavior
  - No serial data at all.
  - No response to `Ctrl-C` or `*`.
  - DCD/CTS lines may be static, but data channel remains silent.

- Command(s) to send
  - Send `Ctrl-C` (`0x03`) up to 3 times (250 ms gap).
  - Send `\r` after each `Ctrl-C` to provoke prompt redraw.
  - Final probe: send `*` (`0x2A`) and wait.

- How to reliably detect it
  - After all probes, receive buffer remains empty or only transport noise without recognizable tokens.
  - No `cmd:` and no AUTOBAUD handshake text detected.

- Timeout value
  - Total powered-off confirmation window: 4000 ms from first probe.

- What to do on success (next action)
  - Mark state as `Powered OFF`.
  - Keep port open, show startup failure status: "No modem response detected."
  - Offer user actions: Retry detection, check power/cable, close connection.

- What to do on failure / fallback
  - If any valid signature appears late, reclassify to matching state.
  - If data is present but unparseable, classify as unknown/noise and continue detection cycle once more.

## 3.2 AUTOBAUD prompt

- Expected modem response / behavior
  - Modem requests autobaud sync after reset/cold start.
  - Typical behavior: requires repeated `*` characters to lock terminal baud.
  - May output prompt or text indicating autobaud/listening for `*`.

- Command(s) to send
  - Send burst of `*` characters: `"***"` then `\r`.
  - If needed, send second burst after 500 ms.

- How to reliably detect it
  - Detect explicit autobaud text (case-insensitive tokens like `AUTOBAUD`).
  - Or detect transition behavior where `*` probing leads to eventual `cmd:` prompt.
  - If `cmd:` appears immediately after star burst, treat as successful AUTOBAUD resolution and classify as AUTOBAUD encountered -> Cmd ready.

- Timeout value
  - Initial autobaud probe window: 2000 ms.
  - Secondary probe window: additional 1500 ms.

- What to do on success (next action)
  - Mark state as `AUTOBAUD prompt` (detected).
  - Continue by completing star sync until `cmd:` prompt is obtained.
  - Move startup flow to command-ready initialization step.

- What to do on failure / fallback
  - If no autobaud indicators appear, continue to Cmd/HOSTMODE detection probes.
  - If repeated star bursts produce only noise, reset parser buffer and continue with `Ctrl-C` strategy.

## 3.3 Cmd: prompt

- Expected modem response / behavior
  - Command mode prompt visible, usually `cmd:` (case-insensitive comparison recommended).
  - Prompt may appear as `cmd:` alone or after status text/line breaks.

- Command(s) to send
  - Primary: `Ctrl-C` then `\r` to force prompt.
  - Optional: send bare `\r` once to redraw prompt.

- How to reliably detect it
  - Match normalized stream against regex equivalent of `\bcmd:\b` (case-insensitive).
  - Accept prompt with leading/trailing whitespace or CR/LF artifacts.
  - Require at least one full token occurrence in current detection window.

- Timeout value
  - Prompt acquisition timeout: 2500 ms after first `Ctrl-C`.

- What to do on success (next action)
  - Mark state as `Cmd: prompt`.
  - Proceed with normal startup init commands (PACTOR setup path).
  - Update progress dialog: "Modem command mode ready."

- What to do on failure / fallback
  - If no prompt appears, try one additional `Ctrl-C` cycle.
  - If still no prompt but continuous binary-like data appears, evaluate HOSTMODE.
  - If no data appears, evaluate Powered OFF.

## 3.4 HOSTMODE

- Expected modem response / behavior
  - Device is in host mode and does not present `cmd:` prompt.
  - Data stream may be framed/binary or host-formatted and may ignore plain text probes.

- Command(s) to send
  - Send `Ctrl-C` and `\r` first; if ignored and non-text traffic continues, suspect HOSTMODE.
  - Optional safe text probe: send `\r` only (avoid destructive commands during detection).

- How to reliably detect it
  - No `cmd:` prompt after full command-mode probe sequence.
  - Incoming stream is non-printable-heavy or repetitive framed structure.
  - Behavior remains stable across 2 consecutive read windows.
  - Classification must be conservative; avoid labeling random noise as HOSTMODE.

- Timeout value
  - HOSTMODE evaluation window: 3000 ms after failed Cmd detection.

- What to do on success (next action)
  - Mark state as `HOSTMODE`.
  - Show status: "Modem appears to be in HOSTMODE."
  - Offer controlled recovery option (user-confirmed sequence to return to command mode) or continue with host-compatible path if later supported.

- What to do on failure / fallback
  - If evidence is weak/inconsistent, classify as unknown startup communication error.
  - Allow user to retry detection with current serial settings.

---

## 4. Sequence / Decision Flow

Recommended startup algorithm:

1. Open serial port with configured parameters.
2. Start detection timer (`MAX_TOTAL_DETECT_MS`).
3. Flush input buffer and wait 150 ms quiet period.
4. Passive read for up to 500 ms:
   - If `cmd:` found -> state = Cmd: prompt (done).
   - If AUTOBAUD indicators found -> state = AUTOBAUD prompt (go to step 7).
5. Active command-mode probe:
   - Send `Ctrl-C`, wait 250 ms, send `\r`, read 800 ms.
   - Repeat up to 3 attempts.
   - If `cmd:` found -> state = Cmd: prompt (done).
6. AUTOBAUD probe:
   - Send `"***\r"`, read 1000 ms.
   - If AUTOBAUD indicator found -> state = AUTOBAUD prompt.
   - Continue star sync and read up to 1500 ms for `cmd:`.
   - If `cmd:` found -> done (AUTOBAUD resolved to command mode).
7. HOSTMODE evaluation:
   - Read window 1500 ms and compute printable ratio + pattern stability.
   - If binary/framed stable stream and no `cmd:` -> state = HOSTMODE (done).
8. Powered OFF evaluation:
   - If no meaningful data seen through full sequence -> state = Powered OFF.
9. If none matched by `MAX_TOTAL_DETECT_MS`, return detection failure (`UNKNOWN_STARTUP_STATE`) and show retry option.

Pseudo-logic sketch:

```text
if seeCmdPrompt() => CMD
else if seeAutobaudText() => AUTOBAUD
else tryCtrlCBreak()
     if seeCmdPrompt() => CMD
     else tryStarAutobaud()
          if seeCmdPrompt() or seeAutobaudText() => AUTOBAUD
          else if looksLikeHostMode() => HOSTMODE
          else if sawNoMeaningfulData() => POWERED_OFF
          else => UNKNOWN
```

---

## 5. Edge Cases & Error Handling

- No response at all
  - Treat as Powered OFF only after full probe sequence and timeout.

- Garbage or RF noise in stream
  - Normalize and strip control noise before matching.
  - Require strong prompt tokens (`cmd:`) before classification.

- Partial prompt fragments (`cm`, `md:` split across reads)
  - Use rolling buffer with bounded size (e.g., last 4 KB).
  - Match on concatenated normalized stream, not per-chunk only.

- Conflicting evidence (e.g., one `cmd:` followed by binary stream)
  - Prefer first strong valid prompt if seen during active probe window.
  - Log raw windows for diagnosis.

- Wrong serial settings (baud/parity mismatch)
  - Likely appears as gibberish.
  - Report "Communication appears invalid; verify serial settings."
  - Offer quick return to Serial Port Setup dialog.

- Stale buffered data from previous session
  - Flush input before first classification attempt.

- User cancellation during progress dialog
  - Abort detection loop gracefully and close/disconnect safely.

---

## 6. Recommended Constants

Use centralized constants class (example names):

- Text tokens:
  - `TOKEN_CMD_PROMPT = "cmd:"`
  - `TOKEN_AUTOBAUD = "autobaud"`

- Probe bytes/commands:
  - `CTRL_C = 0x03`
  - `CR = 0x0D`
  - `STAR = 0x2A`
  - `CMD_BREAK_SEQUENCE = [CTRL_C, CR]`
  - `AUTOBAUD_SEQUENCE = "***\r"`

- Timing:
  - `MAX_TOTAL_DETECT_MS = 8000`
  - `QUIET_AFTER_OPEN_MS = 150`
  - `CTRL_C_ATTEMPTS = 3`
  - `CTRL_C_GAP_MS = 250`
  - `PROMPT_READ_WINDOW_MS = 800`
  - `AUTOBAUD_READ_WINDOW_MS = 1000`
  - `AUTOBAUD_SECOND_WINDOW_MS = 1500`
  - `HOSTMODE_WINDOW_MS = 3000`

- Buffering/parsing:
  - `MAX_ROLLING_BUFFER_BYTES = 4096`
  - `PRINTABLE_RATIO_HOSTMODE_THRESHOLD = 0.35`
  - `MIN_STABLE_WINDOWS_FOR_HOSTMODE = 2`

- Suggested startup baud presets (UI-level fallback list):
  - `1200, 2400, 4800, 9600, 19200`

Implementation note:
keep all detection constants and signatures in one class so field tuning can be done without changing control flow logic.
