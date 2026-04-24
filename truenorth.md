# True North — Memory Inspector

> **Single Source of Truth** for the PK-232 RAM Memory Inspector project.
> All design, implementation, and review decisions must be reconciled against this document.
> When reality and this document disagree, update this document first, then the code.

---

## 1. Project Overview

**Memory Inspector** is a standalone Java 17 Swing desktop application (compiled with plain `javac`, no Maven/Gradle) that connects to an **AEA PK-232 multi-mode data controller** over a serial COM port, drives it into **Host Mode**, and performs a **RAM memory dump** of a user-specified address range.

The app:

1. Prompts for COM port + baud rate at launch (with a "don't show again" preference).
2. Auto-detects the modem's current state (CMD / AUTOBAUD / HOSTMODE / OFF) and transitions it into HOSTMODE.
3. Accepts a 4-hex-digit **ADDRESS** and a 5-digit decimal **BYTES** count, and issues a live RAM dump.
4. Uses the Host-Mode `AE` (set address) and `MM` (read byte w/ auto-increment) commands to walk memory.
5. Renders the output **live**, 128 bytes per line, switchable between ASCII (default) and HEX views, with progress, throughput, and ETA indicators.
6. Leaves the modem in a clean state on exit (`HO N`).

Primary platform: **Windows** (cross-platform is a bonus via jSerialComm).

### Authoritative Source Documents

| Document | Role |
|---|---|
| `truenorth.md` *(this file)* | Single source of truth — decisions, architecture, risks |
| `PK232_HostMode_Reference.md` | Authoritative Host-Mode framing, CTL byte classes, probe sequence, response codes |
| `hCmd.md` | Full verbose/host command reference — authoritative for `AE` / `MM` / `IO` / etc. semantics |
| `logic` | Authoritative startup-state detection spec (per **E4**) |
| `startupDetect.md` | Superseded by `logic`; retained for reference only |

---

## 2. Goals & Success Criteria

### Goals

- Provide a reliable, operator-friendly tool to read arbitrary RAM from a PK-232 without external scripting.
- Robust startup detection: the tool must recover the modem into HOSTMODE from any realistic starting state except "powered off".
- Live, scrolling display during long dumps (up to 99,999 bytes) with cancel and progress.
- Zero external build-system dependency — `javac` + a single jar (`jSerialComm`) only.
- Operator-friendly error behavior: clear dialogs, a rotating diagnostic log, and a clean shutdown path.

### Success Criteria (Definition of Done)

- [ ] Launches on Windows with a bundled `.bat` (dev) and a fat/uber `.jar` (release).
- [ ] Connects to a real PK-232 on a configured COM port at 1200/2400/4800/9600/19200 baud.
- [ ] Correctly detects starting state and forces HOSTMODE without sending `RESET`.
- [ ] Dumps 99,999 bytes without dropped/corrupted bytes or parser resync failures.
- [ ] Renders results live at 128 bytes/line, updated every 32 bytes, with working HEX↔ASCII re-render.
- [ ] Shows live Bytes/sec, ETA, and a working Cancel button during a dump.
- [ ] Main window opens at 2/3 screen size; closing it exits the process.
- [ ] On app exit, sends `HO N`; falls back to serial BREAK if that fails.
- [ ] Writes a rolling log in `./Logs/` next to the jar (10 KB cap × 3 files, one packet per line, direction + command + hex + ASCII).
- [ ] Preferences persist across launches (port, baud, detection timeout, skip-dialog, view mode, last address, last bytes).

---

## 3. Scope & Out of Scope

### In Scope

- Java 17 LTS, Swing UI only.
- Serial I/O via **jSerialComm** (single jar, pure-Java wrapper).
- Host-Mode framing `[SOH][CTL][payload][ETB]` with `DLE` escape of `SOH`/`DLE`/`ETB` in payload (per `PK232_HostMode_Reference.md`).
- Commands used: `AE` (ADDRESS set), `MM` (MEMORY read w/ auto-increment), `OGG` probe, `HO N` (exit host mode).
- `GG` poll and `IO` are **not used** (see §3 Out of Scope for rationale).
- Host-Mode entry sequence: `AWLEN 8`, `PARITY 0`, `8BITCONV ON`, **`RESTART`** (soft reboot, required per `hCmd.md` for AWLEN/PARITY changes to take effect), then the resilient HOST-ON frame `XON CAN ^C HOST Y<CR>` (`11 18 03 48 4F 53 54 20 59 0D`, per `hCmd.md` §4.1.3). **`RESET` is never sent** (it wipes all user settings to PROM defaults per `hCmd.md`; full recovery uses a user-prompted power cycle instead).
- Single-address-space dumping: the full 64 KB range `0x0000–0xFFFF` is treated as one flat space; ROM/RAM regions are not distinguished.
- Persistence via `java.util.prefs.Preferences` at `/BFSoft/MemoryInspector`.
- Rolling diagnostic log in `./Logs/` directory next to the jar.
- Windows as primary target; Linux/macOS a bonus.

### Out of Scope

- Memory **writing** (no write/patch UI; `MM <value>` form is not issued).
- `IO` command (register-level I/O) — not needed for RAM dumping; `MM` auto-increments, `IO` does not.
- `GG` poll loop — not required under `HPOLL OFF`.
- TNC / packet-radio features of the PK-232 beyond what host mode exposes.
- Bulk-read optimizations (no such command exists on the PK-232).
- Maven/Gradle/Ant build tooling.
- Any GUI framework other than Swing (no JavaFX/SWT).
- User-facing protocol-error code display (log-only for v1).
- Automatic firmware/ROM identification.
- Per-region memory-map warnings (ROM vs RAM vs I/O ranges).

---

## 4. Key Decisions & Answers

All answers below are authoritative and supersede any earlier spec text.

### A. Protocol Semantics

| ID | Question | Decision |
|---|---|---|
| **A1** | `MM` response shape? | Host-Mode framed block; on-wire format is `01 4F 4D 4D 24 <hi> <lo> 17`, i.e. payload `"MM$<hi><lo>"` — the mnemonic echo is followed by a **literal `$` separator** and then the **ASCII-hex pair** (2 ASCII chars per byte read). The `$` is hardware-observed (confirmed against real PK-232 traces 2026-04-21 — see §8 Change Log "MM payload `$` separator fix"); `parseMMPayload` drains `MM$` before decoding the hex pair via `HexUtils.fromAsciiHexPair`. Exact framing envelope is defined in `PK232_HostMode_Reference.md` (§Framing, §CTL byte classes, §Command: Global Host Command). `hCmd.md` §MEmory defines command semantics. Both must be consulted before writing the parser. |
| **A2** | `AE` argument format? | **ASCII decimal.** The user's 4-hex ADDRESS (e.g. `BF0D`) is converted to its decimal equivalent (e.g. `48909`) before being sent in the `AE` command. |
| **A3** | Is `MM` pipelinable? | **No.** Pipeline depth = 1. Only one outstanding command at a time. "Overlap" applies only to parsing vs. serial I/O, not multiple in-flight `MM`s. |
| **A4** | `HPOLL` setting? | **`HPOLL OFF`** (push mode). Since every `MM` produces exactly one response block, the push stream stays 1:1 with requests under depth = 1. `GG` poll is not used. |
| **A5** | Bulk-read command? | **None exists.** One byte per round-trip is the hard ceiling. User must be informed of ETA. |
| **A6** | `AE` response envelope? | Standard host ack per `PK232_HostMode_Reference.md` §Command: Global Host Command: `01 4F 41 45 00 17` (mnemonic echo + `c=0x00`). Any non-zero `c` aborts the dump with an error dialog. Verify against `hCmd.md` §ADDress for any additional payload. |

### B. Performance

| ID | Question | Decision |
|---|---|---|
| **B1** | Throughput display? | **Yes.** Show live **Bytes/sec**, **ETA**, a **progress bar**, and a **Cancel** button during a dump. |
| **B2** | Cancel UX? | **Separate Cancel button.** The Dump button is **disabled** while a dump is in progress; Cancel is enabled only during a dump. On the next Dump press, `AE` is always re-issued (per **C7**). |

### C. UI

| ID | Question | Decision |
|---|---|---|
| **C1** | 128 bytes per line? | **Confirmed.** Primary view is **ASCII**, no spacing, **128 characters wide** for the data portion. |
| **C2** | Address prefix wrap? | Lines increment by `0x80`. **No mid-line wraparound** beyond plain modulo. Fixed 128-byte-wide rows, **horizontal scrollbar** when necessary. |
| **C3** | BYTES field format? | **Decimal only**, 1–99999. **Leading zeros are stripped** and accepted. Input of `0` triggers a popup with the exact text: **`OK ive done nothing are you happy?`** |
| **C4** | Address + BYTES > 0xFFFF? | **Stop at `0xFFFF`** and display an "end of memory" warning. |
| **C5** | Non-printable ASCII? | Render as `.` for `0x00–0x1F` and `0x7F–0xFF`. Printable range is `0x20–0x7E`. |
| **C6** | HEX/ASCII toggle? | **Live re-render of the entire dumped buffer.** Does not only affect future lines. |
| **C7** | Post-Cancel restart | On every Dump press, **always** send `AE` before starting the `MM` loop. Never trust the modem's last increment state. |
| **C8** | ADDRESS input format | Exactly **4 hex chars** (`0-9`, `A-F`, case-insensitive). **No `$` or `0x` prefix accepted.** Input is normalized to uppercase on submit. |
| **C9** | MainFrame window | Opens at **2/3 of the current screen size**, centered. It is **always open** while the program runs. **Closing `MainFrame` exits the application** (triggers the `HO N` shutdown path). |
| **C10** | Pre-dump ADDRESS bounds | Treat ROM and RAM as **one flat 64 KB address space**. No warnings, no refusals, no region-based special handling. The device answers whatever it answers. |

### D. Connection Dialog

| ID | Question | Decision |
|---|---|---|
| **D1** | Baud list? | `1200, 2400, 4800, 9600, 19200`. Default **9600**. |
| **D2** | Data bits / parity / stop / flow? | **Hard-coded 8-N-1, no flow control.** Not user-exposed. |
| **D3** | Port list refresh? | **Yes**, a refresh button for hot-plug. |
| **D4** | "Don't show on launch" fallback? | **Yes.** If auto-connect fails, the dialog still appears regardless of the preference. |
| **D5** | Detection timeout | **User-configurable** in the COM port / Settings window. Default **8000 ms**. Persisted via `AppSettings`. |

### E. State Management

| ID | Question | Decision |
|---|---|---|
| **E1** | Already in HOSTMODE on launch? | **Do not trust it.** Always `HO N` → re-detect `cmd:` → re-enter HOSTMODE. |
| **E2** | `RESET` vs `RESTART`? | **Never send `RESET`.** Per `hCmd.md`, `RESET` is destructive — it wipes all user parameters (MYCALL, MailDrop messages, monitor lists, stored baud) back to PROM defaults. **`RESTART` IS sent** and is required as part of the Host-Mode entry sequence (§5.5): it is a non-destructive soft reboot that applies queued `AWLEN` / `PARITY` / `8BITCONV` changes while retaining all user settings. If full recovery is needed (e.g. modem fully wedged), prompt the user to power-cycle — which also preserves user settings when the battery jumper is connected. |
| **E3** | App exit behavior? | **Exit host mode** (`HO N`) on application exit. Fallback to serial BREAK if `HO N` fails. Triggered by `MainFrame` close (per **C9**) and by the OS shutdown hook. |
| **E4** | Authoritative detection spec? | **`logic` takes precedence** over `startupDetect.md` — it is known and tested. |
| **E5** | `OGG` vs `GG`? | `GG` = poll mnemonic (not used in this app). `OGG` probe = the **formed/wrapped command packet** `01 4F 47 47 17`, success response `01 4F 47 47 00 17`. Both defined in `PK232_HostMode_Reference.md` §Entering Host Mode. |

### F. Logging / Diagnostics

| ID | Question | Decision |
|---|---|---|
| **F1** | Log location? | **`./Logs/`** directory, created next to the jar if missing. File name `memory_inspector.log` plus rotated `.1` / `.2` suffixes. |
| **F2** | Log rotation? | **10 KB cap per file**, new file when full, **keep only the 3 most recent**. |
| **F3** | Log line format? | One packet per line. Prefix: **direction (`TX`/`RX`) + command/block type (`AE`, `MM`, `OGG`, `HON`, `ACK`, `ERR`, etc.)**. Then the full framed payload in **HEX**, then the same payload rendered in **ASCII** (non-printables as `.`). Example: `TX MM  01 4F 4D 4D 17  .OMM.` |
| **F4** | Protocol error codes in UI? | **Log only** for v1. May be surfaced in the UI later. |

### G. Packaging

| ID | Question | Decision |
|---|---|---|
| **G1** | Java target? | **Java 17 LTS.** |
| **G2** | Distribution? | **`.bat` launcher** for development; **fat/uber `.jar`** for releases. |
| **G3** | Platform? | **Windows required**, cross-platform a bonus (jSerialComm supports it). |

### H. Build / Dependencies / Launch

| ID | Question | Decision |
|---|---|---|
| **H1** | jSerialComm version? | **`jSerialComm-2.11.4.jar`** — the only shipped third-party runtime dependency. Vendored into `lib/`. *(As of the decision date the jar is staged at the project root; it will be moved into `lib/` when that directory is scaffolded in Step 6.1.)* |
| **H2** | Test harness? | **JUnit 5 console launcher** — `junit-platform-console-standalone-<ver>.jar` vendored into `lib/` alongside `jSerialComm`. Invoked by `test.bat`. Test-only classpath; **never** merged into the release fat jar. |
| **H3** | Dev/release launcher? | **`javaw.exe`** (no console window) for both `run.bat` and the release shortcut. All diagnostic output routes through `PacketLogger` (**F1–F3**); `System.err` may additionally be tee'd to the rolling log from `Main` if useful. |
| **H4** | Default monospaced font? | Resolved at `HexDumpView` construction time from `GraphicsEnvironment.getAvailableFontFamilyNames()` using the fallback chain `Consolas → Cascadia Mono → Menlo → Monospaced` at **12 pt**. **Not persisted** in `AppSettings` for v1 (see §5.8). Making the font configurable is deferred. |

---

## 5. Architecture / Technical Approach

### 5.1 Layered View

```text
+-------------------+        +---------------------+       +----------------------+
| Swing UI (EDT)    | <----> | Dump Controller     | <---> | Host-Mode Protocol   |
|  - Startup dialog |        |  - serialized loop  |       |  - block encode/dec  |
|  - MainFrame      |        |  - progress/ETA     |       |  - AE / MM / OGG     |
|  - Settings menu  |        |  - cancel           |       |  - escape handling   |
+-------------------+        +---------------------+       +----------------------+
          ^                           ^                               ^
          | invokeLater batches       | blocking queue                | byte stream
          +---------------------------+-------------------------------+
                                      |
                              +-------------------+
                              | Serial IO Thread  |
                              | (jSerialComm)     |
                              +-------------------+
```

### 5.2 Threading Model

- **Serial Read Thread** — blocking reads, feeds raw bytes to the Host-Mode parser (`WAIT_SOH → READ_CTL → PAYLOAD (DLE escape) → ETB`); emits `HostBlock` objects to a `LinkedBlockingQueue<HostBlock>`.
- **Protocol Thread** — drains the queue, correlates blocks to pending commands, invokes callbacks.
- **EDT (UI Thread)** — only receives pre-batched UI updates via `SwingUtilities.invokeLater`.
- All shared state: thread-confined or `java.util.concurrent` primitives.

### 5.3 Module / Class Breakdown

| Package | Class | Responsibility |
|---|---|---|
| `ui` | `StartupConnectDialog` | Port list + refresh, baud combo, **detection-timeout field (D5)**, "Don't show on launch" checkbox, OK/Cancel |
| `ui` | `DumpPromptDialog` | Modal ADDR (4 hex, **C8**) + BYTES (decimal 1..99999, **C3** snarky 0-popup) entry dialog, ESC=Cancel / ENTER=OK. Returned by `showDialog()` as a `Result(int addr, int bytes)` or `null` on cancel. Lands in M4 as a standalone class so `Main` doesn't have to grow for the prompt UI; re-used verbatim by the full `MainFrame` Dump button in M5+. |
| `ui` | `MainFrame` | Menu bar (Settings → Port…), ADDRESS (4 hex, **C8**) / BYTES inputs, Dump/Cancel buttons, HEX/ASCII radios, scrolling output pane, progress/ETA/Bps indicators. Opens at **2/3 screen size (C9)**; close triggers app exit + `HO N`. M4 and **M5** temporarily use the placeholder window in `Main` as a stand-in — M5 Q2 chose to keep extending `Main.showPlaceholderMainWindow` rather than promote to `ui.MainFrame` yet (user-deferred per §8 Change Log 2026-04-21 "M5 design locked"). `ui.MainFrame` promotion + `app.DetectionRunner` extraction move to M6+. |
| `ui` | `HexDumpView` | Append-only 128-col rendering; re-render on view-mode toggle |
| `config` | `AppSettings` | Load/save preferences via `java.util.prefs.Preferences` |
| `serial` | `SerialLink` | Thin wrapper over `jSerialComm` (open/close/read/write + BREAK, 8-N-1, no flow control) |
| `protocol` | `HostBlock`, `HostCodec` | Framing encode/decode per `PK232_HostMode_Reference.md` (DLE-escape of `SOH`/`DLE`/`ETB` in payload) |
| `protocol` | `StartupDetector` | Detection flow per `logic` spec (authoritative) |
| `protocol` | `HostModeEntry` | `AWLEN 8` → `PARITY 0` → `8BITCONV ON` → `RESTART` (2000 ms settle + `cmd:` verify) → resilient `XON CAN ^C HOST Y<CR>` → `OGG` probe (**never `RESET`**) |
| `protocol` | `PK232Client` | 2-thread framed client (§5.2): owns a Serial Read Thread → `LinkedBlockingQueue<HostBlock>` → Protocol Thread. API: `start()`, `sendAndAwait(HostBlock req, long timeoutMs)` (single outstanding via `ReentrantLock`, 1500 ms default per §5.6), `setUnsolicitedHandler(Consumer<HostBlock>)`, `close()` (idempotent). Started after `HostModeEntry.enter()` succeeds; stopped **before** `HostModeEntry.tryExit` so the shutdown path can reuse the bare `SerialLink`. M4 helpers: `setAddress(int addr)` issues `AE <decimal>` and validates the `[0x41 0x45 0x00]` ack (non-zero status byte → `IOException`); `readOneByte()` issues `MM` and decodes the 2-ASCII-hex-pair payload to an unsigned `int` 0..255. Payload validation extracted to package-private static methods `validateAEPayload` / `parseMMPayload` for unit-testability (Option-3 relaxed for M4 per §8 Change Log 2026-04-20). |
| `dump` | `DumpController` | Serialized `AE` + `MM` loop (depth=1), 32-byte UI batching, Bps/ETA, cancel via `volatile boolean cancelled` flag (Q3 2026-04-21 — checked between `readOneByte` calls; final in-flight `sendAndAwait` takes up to 1500 ms to unblock), **`0x5F` abort via handler swap** installed in `start()` / restored in `finally` (Q4 2026-04-21), full result `byte[]` kept for HEX↔ASCII toggle re-render (Q5, C6), 128-col append-only rendering (C9 view column count), always-re-issue `AE` on every Dump press (Q7, C7). |
| `util` | `HexUtils` | Hex↔decimal↔ASCII helpers |
| `util` | `PacketLogger` | Rolling 10 KB × 3-file log in `./Logs/` with direction + command + hex + ASCII (per **F1–F3**) |

### 5.4 Startup & Connection Flow

1. Load `AppSettings`.
2. If `skipStartupDialog` **and** saved port/baud exist → attempt auto-connect.
3. Otherwise (or on auto-connect failure) → show `StartupConnectDialog`.
4. Open port at 8-N-1, no flow control.
5. Show modal progress dialog while `StartupDetector` runs with the configured `detectionTimeoutMs` (per **D5**).
6. `StartupDetector.detect()` runs the `logic`-file ladder (COMMAND → AUTOBAUD → HOSTMODE → OFF); on success the modem is always parked at `cmd:` (locked Q6). Each branch's post-condition:
   - **COMMAND** — two `CR`s have produced `cmd:`; nothing more to drain.
   - **AUTOBAUD** — `*` has produced the `PK-232` banner; detector keeps reading until the banner's terminal `cmd:` appears (within `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms` after `PK-232` is first seen) so the next stage doesn't step on banner residue.
   - **HOSTMODE** — the double-SOH `HO N` probe + `CR` has produced `cmd:` (the probe exited host mode and left the modem at the command prompt).
   - **OFF** — user-prompted power-cycle, wait for banner, re-run the three stages once; `DetectionException` on second fall-through.
7. `HostModeEntry.enter()` runs the five-step verbose/`RESTART`/HOST-Y/OGG sequence regardless of which branch fired.
8. `MainFrame` shown at 2/3 screen size (per **C9**); status bar: "HOSTMODE ready".
9. Menu → Settings → Port… disconnects and re-runs this flow.

### 5.5 Host-Mode Entry (Cmd → Hostmode)

Invoked by `HostModeEntry.enter()` immediately after `StartupDetector.detect()`
returns (modem parked at `cmd:` per Q6). Verbose-command stage uses the same
"accumulator reset between steps" discipline as the detector, so any
unsolicited bytes that arrive after the previous `cmd:` (e.g. a trailing banner
tail) cannot bleed into the next step's match window.

1. **Verbose-settings stage** (at `cmd:`, each terminated with `CR` only per Q1):
   - `AWLEN 8<CR>` → await substring `cmd:` within **500 ms** (Q2).
   - `PARITY 0<CR>` → await substring `cmd:` within 500 ms.
   - `8BITCONV ON<CR>` → await substring `cmd:` within 500 ms.
2. **`RESTART<CR>`** — required per `hCmd.md` (the three settings changes
   above do not take effect until `RESTART` is performed; see `hCmd.md`
   §AWLEN and §PARITY notes). Drain RX for **2000 ms** (bytes logged as
   `RX RST` — this is the modem's soft-reboot banner / register dump),
   then verify the cumulative buffer contains `cmd:` as a substring. Fail
   with `HostModeEntryException` if it does not. **Do not confuse `RESTART`
   with `RESET`** — the latter is destructive and forbidden (E2).
3. **Resilient HOST-ON frame** (Q8 — spec-preferred per `hCmd.md` §4.1.3):
   - TX: `11 18 03 48 4F 53 54 20 59 0D` (`XON CAN ^C HOST Y<CR>`).
   - The three leading control bytes cancel any residual line state before
     the `HOST Y<CR>` command; no `cmd:` ack wait because the modem is
     transitioning to host mode on this write and will fall silent (Q3).
4. **OGG probe** (host framing now active):
   - TX: `01 4F 47 47 17` (single-SOH first, per spec step 2).
   - RX: feed bytes into `HostCodec.Parser`; accept the first block with
     `ctl == 0x4F` and `payload == [0x47, 0x47, 0x00]` as success.
   - Timeout: **750 ms** (Q4 — matches detector per-step).
5. **Retry on miss** (double-SOH recovery per Q5):
   - TX: `01 01 4F 47 47 17` (double-SOH form). Wait another 750 ms for the
     same ack shape. Fail on second miss with `HostModeEntryException`.
6. `SerialLink.readTimeoutMs` is saved on entry and restored in `finally` —
   safe to sandwich this call between other protocol code.

**Exit**: `HostModeEntry.tryExit(link, log)` static helper — best-effort send
of the double-SOH `HO N` frame `01 01 4F 48 4F 4E 17` (Q6), waits up to 500 ms
for ack `01 4F 48 4F 00 17`, falls back to `SerialLink.sendBreak(300)` on write
error or ack timeout. Total shutdown path capped at 1500 ms wall-clock (§5.11).

On entry failure `HostModeEntry` never issues `RESET` (destructive — E2).
The `Main` boot flow surfaces the error in a Retry/Cancel dialog (Q7); Retry
closes and reopens the `SerialLink` before re-running `detect()` + `enter()` (Q10).

Framing, CTL byte classes, and the probe byte sequence are defined in
`PK232_HostMode_Reference.md`; verbose-command semantics in `hCmd.md`.

### 5.6 Memory Dump Pipeline (Depth = 1)

Per decision **A3**, commands are strictly serialized. Per **C7**, `AE` is always re-issued on every Dump press. All wire-level I/O in this section flows through `PK232Client.sendAndAwait(HostBlock, long)` (§5.3); `DumpController` never touches `SerialLink` directly. `PK232Client` enforces the depth-1 invariant via a `ReentrantLock`, so even a buggy caller cannot interleave commands.

1. `DumpController` validates `ADDR + BYTES` against `0xFFFF` and clamps per **C4**.
2. Converts `ADDR` (4 hex chars) to decimal per **A2** and transmits:
   - **TX**: `AE <decimal>` as a `0x4F` global-command block.
   - **RX**: expected `01 4F 41 45 00 17` (per **A6**). Any non-zero `c` aborts with an error dialog.
3. Loop, for each byte `i` in `[0, N)`:
   1. **TX**: `MM` as a `0x4F` global-command block (no args) — payload = `4D 4D`.
   2. **RX**: expected `01 4F 4D 4D <hi-ASCII> <lo-ASCII> 17`. Decode the 2 ASCII hex chars into 1 byte.
   3. Store the byte into preallocated `byte[N]`. The PK-232 auto-increments ADDRESS per `hCmd.md` §MEmory.
   4. Every 32 bytes, fire an EDT batch to `HexDumpView` (append only completed 128-byte rows; maintain a partial-row buffer).
   5. Update Bps/ETA counters.
4. Overlap = parser/decoder running on the protocol thread while serial I/O reads the next response.
5. On error response (`0x5F` bad-block/CTL, or non-zero `c` in a `0x4F` response): resync parser, abort dump, surface dialog.
6. Per-command timeout: **1500 ms** → abort dump, keep modem in HOSTMODE, surface error dialog.
7. Cancel: cooperative — the loop checks a `volatile boolean` each iteration. The next Dump press unconditionally re-issues `AE` (per **C7**).

### 5.7 Display Rendering

- Monospaced font, append-only, **auto-scroll-to-bottom** unless the user has scrolled up.
- Line = `AAAA` + space + **128 chars of data** (ASCII default, no spacing).
- HEX mode: same 128 bytes rendered as `AAAA 00 01 02 … FF` (wider — horizontal scroll enabled).
- Non-printables → `.` (per **C5**).
- Updates coalesced every 32 bytes via `invokeLater`.
- View-mode radio toggles re-render the entire buffered `byte[]` (per **C6**).

### 5.8 Persistence (`AppSettings`)

Stored under `java.util.prefs.Preferences /BFSoft/MemoryInspector`:

- `comPort` (string, e.g. `COM3`)
- `baud` (int)
- `detectionTimeoutMs` (int, default `8000`, per **D5**)
- `skipStartupDialog` (bool)
- `viewMode` (`HEX` | `ASCII`)
- `lastAddress` (string, 4 hex, uppercase)
- `lastBytes` (int)

### 5.9 Logging (`PacketLogger`)

- Directory: `./Logs/` (created alongside the running jar on first use).
- File: `memory_inspector.log`, rotated to `.1` / `.2` suffixes; oldest beyond `.2` is deleted.
- Rotation trigger: file size ≥ **10 KB**.
- Line format (one per packet):

```text
<TX|RX> <CMD>  <HEX bytes, space-separated>  <ASCII render, '.' for non-printable>
```

Examples:

```text
TX AE   01 4F 41 45 34 38 39 30 39 17   .OAE48909.
RX ACK  01 4F 41 45 00 17               .OAE..
TX MM   01 4F 4D 4D 17                  .OMM.
RX MM   01 4F 4D 4D 33 46 17            .OMM3F.
TX HOX  01 01 4F 48 4F 4E 17            ..OHON.
RX HOA  01 4F 48 4F 00 17                .OHO..
```

Reserved mnemonic tokens by milestone:

| Milestone | Direction + token | Meaning |
|---|---|---|
| M3a | `TX CR` / `TX ST` / `TX HPB` | `CR` / `*` / double-SOH host-mode probe sent during detection |
| M3a | `RX DET` / `RX BAN` / `RX DRN` | Detection-stage RX / power-on banner / post-detect residual drain |
| M3b | `TX AWL` / `TX PAR` / `TX 8BC` | `AWLEN 8<CR>` / `PARITY 0<CR>` / `8BITCONV ON<CR>` verbose commands |
| M3b | `TX RST` | `RESTART<CR>` soft-reboot command (non-destructive, applies queued settings — never confuse with `RESET`) |
| M3b | `RX RST` | Bytes received during the 2000 ms post-`RESTART` settle window (reset banner / register dump) |
| M3b | `TX HYR` | Resilient HOST-ON frame `11 18 03 48 4F 53 54 20 59 0D` |
| M3b | `RX CMD` | RX buffer gathered while waiting for `cmd:` between verbose commands |
| M3b | `TX OGG` / `TX OGR` | OGG probe single-SOH / OGG probe double-SOH retry |
| M3b | `RX OGG` | OGG ack block `01 4F 47 47 00 17` |
| M3b | `TX HOX` / `RX HOA` | Shutdown `HO N` (double-SOH) and its ack |
| M3b | free-form `logRaw` | BREAK assertion + reason — e.g. `shutdown: HO N ack timeout, asserting 300 ms BREAK` |
| M3c | `TX <mnem>` | Command sent via `PK232Client.sendAndAwait` — mnemonic derived from the first two ASCII-alpha bytes of the payload (e.g. `TX AE`, `TX MM`, `TX HP`). Falls back to `CMD` if payload starts non-printable. |
| M3c | `RX <mnem>` | Correlated `0x4F` response block — same mnemonic derivation as TX. Logged by the Protocol Thread, not by the caller. |
| M3c | `RX UNS` | Unsolicited non-error block (e.g. `0x2F`, `0x3F`, or a `0x4F` arriving without a pending command). Routed to the `Consumer<HostBlock>` unsolicited handler. |
| M3c | `RX ERR` | Unsolicited `0x5F` status/error block. Also routed to the unsolicited handler; per §5.6 a `0x5F` during a dump aborts the dump. |
| M3c | free-form `logRaw` | Client lifecycle (`PK232Client: started` / `stopped`) and non-fatal thread events (reader IOException, unsolicited handler threw). |
| M1 (legacy) | `TX HON` | Single-SOH `HO N`. Reserved for log-archive readability; superseded by `TX HOX` in M3b+ |

Protocol error codes from the `0x4F` `c` table are recorded here; they are **not** shown in the UI for v1 (per **F4**).

### 5.10 Build & Distribution

- **Runtime dependency (single):** `lib/jSerialComm-2.11.4.jar` (per **H1**).
- **Test dependency (single, not shipped):** `lib/junit-platform-console-standalone-<ver>.jar` (per **H2**).
- **Dev build** (`build.ps1` / `build.bat`):
  ```bat
  javac -encoding UTF-8 -cp "lib\jSerialComm-2.11.4.jar" -d out @sources.txt
  jar cfe MemoryInspector.jar app.Main -C out .
  ```
- **Test build / run** (`test.bat`): compiles `test/*.java` against `out` + the JUnit console launcher jar, then invokes the launcher with `--scan-class-path`.
- **Release**: fat/uber `.jar` with `jSerialComm` merged in (manual `jar` extraction + repack, no Shade plugin). The JUnit jar is **never** included.
- **Launcher**: `run.bat` uses **`javaw`** (per **H3**) so no console window is created; all diagnostics flow through `PacketLogger`.
- **Target JRE**: **Java 17 LTS**.

### 5.11 Error Handling Summary

| Condition | Action |
|---|---|
| Serial open failure | Dialog → return to Startup Dialog |
| Detection timeout (> `detectionTimeoutMs`, **D5**) | Unknown-state dialog (Retry / Change Port / Cancel / Power-cycle prompt) |
| Probe failure | One double-SOH retry, then fail |
| Dump error (`0x5F`, non-zero `c` in `0x4F` response) | Abort dump, keep HOSTMODE, error dialog |
| Per-command timeout (1500 ms) | Abort dump, keep HOSTMODE, surface message |
| Host-mode entry failure (no OGG ack after double-SOH retry, or `cmd:` missing between verbose commands) | Throw `HostModeEntryException`. `Main` surfaces a `JOptionPane` with **Retry** (closes + reopens the port, re-runs `detect()` + `enter()`) and **Cancel** (runs `runShutdown` + `System.exit(1)`). |
| `MainFrame` closed | Trigger app exit path |
| App exit | Double-SOH `HO N` (`01 01 4F 48 4F 4E 17`). Wait up to **500 ms** for ack `01 4F 48 4F 00 17`. On write-error or ack-timeout, assert **BREAK for 300 ms**. Total shutdown path capped at **1500 ms** wall-clock so JVM exit never hangs. |

### 5.12 Test Plan (Pre-Code)

- Framing unit tests: encode/decode round-trip including escape of `SOH`/`DLE`/`ETB` in payload (per `PK232_HostMode_Reference.md` §Escape handling).
- `StartupDetector` tests with a scripted fake serial stream (table-driven).
- `HostModeEntry` tests with simulated `cmd:` + `OGG` probe responses.
- `DumpController` tests against a synthetic PK-232 that:
  - Answers `AE` with `01 4F 41 45 00 17`.
  - Answers `MM` deterministically with `01 4F 4D 4D <hi> <lo> 17`.
  - Injects a `0x5F` fault and a non-zero `c` to verify abort paths.
- UI smoke test: 1 KB dump renders expected rows and address prefixes; HEX/ASCII toggle re-renders; Cancel + re-Dump re-issues `AE`.
- Manual integration test on real hardware.

---

## 6. Open Questions & Risks

### Open Questions

None currently blocking. All previously open items are resolved — see §4 and the Change Log:

- Protocol / UI / logging clarifications (A1, E5, C3, C8, C9, C10, D5, F1–F3, A6, post-Cancel semantics, status bar states) — resolved 2026-04-20.
- Build / dependency / launcher / font items (**H1** jSerialComm version, **H2** test harness, **H3** `javaw` launcher, **H4** default font) — resolved 2026-04-20 (see §4.H).

### Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Depth-1 pipeline + 9600 baud + ASCII-hex response doubles bytes on wire → ~480 bytes/sec theoretical ceiling; 99,999 bytes ≈ **3.5+ minutes** minimum, likely longer with round-trip overhead. | User frustration during full-RAM dumps. | Progress bar, Bps, ETA, Cancel (per **B1/B2**). Document the ceiling in user-visible text. |
| Manual power-cycle requirement for fully-wedged modems (`RESET` is forbidden — see E2) | Recovery from a wedged modem is not fully automated. | Clear modal dialog with instructions and a Retry action. `RESTART` (soft reboot) handles in-band recovery during Host-Mode entry. |
| `HPOLL OFF` implicit ordering | Push responses must line up 1:1 with requests; any drop desyncs the stream. | Strict depth-1 pipeline; per-command timeout + abort on error code. |
| 128-char-wide lines don't fit typical windows | Poor UX on small screens. | Horizontal scrollbar + monospaced layout; MainFrame opens at 2/3 screen size (C9). |
| 10 KB log cap is small — may rotate mid-dump and lose context. | Diagnostic gaps. | Already decided; revisit if it proves too tight during hardware testing. |
| Flat 64 KB space: user may target non-RAM regions and get garbage or device-side errors. | Confusing dumps. | Accepted by design (C10); errors surface via the `0x4F` `c` field + log. |
| jSerialComm 2.11.4 native bits may need explicit extraction when built as fat jar | Release launch failure. | Validate fat-jar build on a clean Windows box before declaring M8 complete. |
| `AE` decimal vs `$hex` ambiguity | `hCmd.md` §ADDress uses `$0000` hex notation for the verbose form; host form takes decimal per **A2**. Parser confusion possible. | Single code path: always format `AE` argument as plain ASCII decimal (no `$`, no `0x`). Covered by `DumpController` unit test. |
| Log file growth when many short sessions | Disk clutter. | 3-file rotation caps total log footprint at ~30 KB. |

---

## 7. Next Steps / Phase 1 Plan

Phase 1 covers milestones **M1–M3**, which are the foundation needed before any dumping can be meaningfully tested. With the protocol questions resolved via `PK232_HostMode_Reference.md` and `hCmd.md`, coding can begin immediately.

### Immediate Pre-Code Actions

1. Lock test vectors from `PK232_HostMode_Reference.md`:
   - Probe TX/RX: `01 4F 47 47 17` / `01 4F 47 47 00 17`.
   - Double-SOH recovery: `01 01 4F 47 47 17`.
   - `HO N`: `01 4F 48 4F 4E 17`.
   - `0x4F` response skeleton: `01 4F a b c 17`.
2. Lock dump test vectors:
   - `AE <decimal>` ack: `01 4F 41 45 00 17`.
   - `MM` read response: `01 4F 4D 4D <hi-ASCII> <lo-ASCII> 17`.
3. Confirm escape semantics for `SOH`/`DLE`/`ETB` in payload per `PK232_HostMode_Reference.md` §Escape handling.

### M1 — Serial link + Startup dialog + Persistence

- Create project layout: `src/`, `test/`, `lib/`, `out/`, `Logs/`, `build.ps1`, `build.bat`, `run.bat`, `test.bat`.
- Move **`jSerialComm-2.11.4.jar`** (currently at the project root, per **H1**) into `lib/`.
- Vendor **`junit-platform-console-standalone-<ver>.jar`** into `lib/` for the test harness (per **H2**).
- Wire `run.bat` to launch with **`javaw`** (per **H3**).
- Implement `AppSettings` (Preferences-backed, including `detectionTimeoutMs`).
- Implement `SerialLink` (open/close/read/write + BREAK, 8-N-1, no flow control).
- Implement `StartupConnectDialog` (port list + refresh, baud combo default 9600, **detection-timeout field**, "don't show on launch", OK/Cancel).
- Wire auto-connect path with fallback to dialog on failure (**D4**).
- Implement `PacketLogger` (F1–F3).

### M2 — Host-mode codec + parser + unit tests

- Implement `HostBlock`, `HostCodec` (SOH / CTL / payload w/ DLE escape / ETB).
- Unit tests for encode/decode including payload bytes that require escaping.
- Table-driven tests against the locked test vectors above.

### M3 — Startup detector + Cmd→Hostmode transition

- Implement `StartupDetector` following the `logic` spec (authoritative per **E4**).
- Implement `HostModeEntry` with `RESTART` (required per `hCmd.md` §4.1.3 to apply AWLEN/PARITY/8BITCONV before host mode); **never `RESET`** (E2). Prompt-for-power-cycle path for unrecoverable states.
- Implement `OGG` probe + double-SOH retry.
- `HO N` on exit (and on `MainFrame` close, **C9**) with BREAK fallback.

### Subsequent Milestones (reference only)

| # | Deliverable |
|---|---|
| M4 | `PK232Client.setAddress` + `readOneByte` + simple dump display |
| M5 | Serialized dump controller + 32-byte UI batching + Bps/ETA/Cancel + always-re-AE on Dump (C7) |
| M6 | HEX/ASCII toggle + address-prefixed 128-col layout + horizontal scroll + MainFrame 2/3-screen sizing (C9) |
| M7 | Menu bar, Settings → Port… re-entry, graceful shutdown (`HO N` / BREAK), 0-byte snarky popup (C3) |
| M8 | Hardening, rolling log (10 KB × 3 in `./Logs/`), fat-jar release build, docs |

---

## 8. Change Log

### 2026-04-22 — Uber-jar release packaging (`release.bat` + `releases/beta-<date>/`)

Fat/uber-jar packaging script that produces a self-contained
distributable — end users don't need the `lib/` folder. Previously
planned as "M8" in truenorth §7; landed ahead of any M7 refactor
because the app is feature-complete and shippable after M6 +
polish.

**New script: `release.bat`** (99 lines). Flow:

1. Delegates compilation to `build.bat` (keeps javac / `jar.exe`
   resolution in one place; avoids code duplication).
2. Resolves `jar.exe` via the same `JAVA_HOME` → `java -XshowSettings`
   probe pattern that `build.bat` uses (see §9 PATH quirk).
3. Computes today's date via `powershell Get-Date -Format yyyy-MM-dd`
   (wmic is deprecated on recent Windows).
4. Stages `out/fatjar-stage/`:
   - Extracts `lib/jSerialComm-2.11.4.jar` into the stage.
   - Deletes the extracted `META-INF/` (jSerialComm's
     MANIFEST/signatures would either overwrite ours or invalidate
     the repackaged jar at load time).
   - Copies every top-level package under `out/` into the stage
     (`app`, `config`, `dump`, `protocol`, `serial`, `ui`, `util`),
     explicitly skipping `out/test/` (test-only classes) and
     `out/fatjar-stage/` itself.
5. Runs `jar cfe releases\beta-<YYYY-MM-DD>\MemoryInspector.jar app.Main -C out\fatjar-stage .`
   — `cfe` writes a fresh `META-INF/MANIFEST.MF` with
   `Main-Class: app.Main`; `-C` + `.` recursively includes the
   whole stage.
6. Cleans up `out/fatjar-stage/` after packaging.

**Artifact verified today (2026-04-22):**

- Path: `releases/beta-2026-04-22/MemoryInspector.jar`
- Size: 941 KB (our ~100 KB of classes + jSerialComm's ~900 KB
  including 28 native libs covering Android / FreeBSD / Linux /
  macOS / OpenBSD / Solaris / Windows on x86 / x86_64 / arm /
  aarch64 / armv5-8 / ppc64le / sparc).
- Manifest has `Main-Class: app.Main`.
- Contains `Windows/x86_64/jSerialComm.dll` (primary target
  platform per truenorth §2).
- Contains all 40 of our `.class` entries (including anonymous
  inner classes like `Main$1`, `Main$2`, `Main$3` for the
  action-listener lambdas).

**Usage for end users:** drop `MemoryInspector.jar` anywhere
(no adjacent `lib/` needed), launch via `java -jar MemoryInspector.jar`
or, on Windows, double-click if `.jar` is associated with `javaw`.
The existing `run.bat` still launches the development thin jar;
distribution uses the fat jar in `releases/`.

**Release cadence:** `beta-<YYYY-MM-DD>` subdirectory per day —
re-running `release.bat` on the same day overwrites the existing
artifact; running on a different day creates a fresh folder. No
automatic versioning pulled from `APP_VERSION` in `Main.java`
(manual step if semantic versioning becomes necessary).

**No source / behavior changes** in this entry — pure packaging.

---

### 2026-04-21 — Polish: last-save-dir persistence + auto-scroll-unless-scrolled-up

Two small UX improvements requested after M6 hardware-acceptance.
Both are behavior tweaks; no new classes, no new tests (UI glue,
hardware-gated per Option-3).

**1. Last-save-dir persistence.** `File → Save Dump…` now remembers
the directory the user last saved into, across app launches.

- `config/AppSettings.java` (132 → 153 lines): new preference key
  `lastSaveDir` with getter/setter matching the existing
  `lastAddress` convention (empty-string sentinel for "not set";
  setter clears the pref when passed null/empty).
- `src/app/Main.java` `doSaveDumpClick`:
  - Reads `AppSettings.getLastSaveDir()` via new helper
    `resolveSaveStartDir(settings)` which falls back to
    `user.home` if the preference is unset OR the saved
    directory no longer exists (fallback is important for
    portable drives / cleaned-up profiles).
  - After a successful save, persists the **parent directory**
    of the chosen file (not the file itself) via
    `settings.setLastSaveDir(parent.getAbsolutePath())` +
    `settings.flush()`.
  - Default filename in the chooser is unchanged
    (`dump_$AAAA_NNNbytes_<mode>.txt`).

**2. Auto-scroll-unless-scrolled-up.** Previously the dump output
pane always jumped to the end on every batch, completion footer,
and `[RECONNECTED]` marker — disruptive if the user was scrolling
back through earlier bytes while a dump was streaming. Now the
pane auto-scrolls only if the user was already at (or within a
fuzzy tolerance of) the tail BEFORE the mutation; otherwise the
user's scroll position is preserved.

- `src/app/Main.java` new helpers:
  - `mutateOutputPreservingScroll(JTextArea, Runnable)` —
    snapshots `wasAtBottom` + `savedValue` before running the
    mutation, then defers a scrollbar write via
    `SwingUtilities.invokeLater` so the restore/stick-to-bottom
    decision fires AFTER layout settles (before the defer,
    `getMaximum()` still reflects the pre-mutation extent and
    sticky-to-bottom landed on the old bottom).
  - `isAtOrNearBottom(JScrollPane)` — tolerance is
    `max(40 px, 2 × unitIncrement)`; comfortably larger than one
    line height so "scrolled up a couple of lines while reading"
    is honored as "user is reading history, don't yank".
    Invisible scrollbars (content fits in viewport) are treated
    as "at bottom" so the one-line banner at dump-start still
    counts as a tail view.
  - `resolveScrollPane(JTextArea)` — walks the ancestor chain
    via `SwingUtilities.getAncestorOfClass`; avoids threading
    scrollpane references through every method signature.
- Call sites wrapped:
  - `rerenderDumpView` — the `setText` replace on every batch /
    HEX↔ASCII toggle / post-reconnect.
  - `onCompleted` — previously did `rerenderDumpView` + a
    separate `output.append` + `setCaretPosition`; those three
    merged into a single `setText(banner + body + footer)` so
    the wrapper captures `wasAtBottom` ONCE around a single
    mutation. Prevents a brief flicker where render and footer
    append would otherwise be two scroll decisions.
  - Reconnect worker's `[RECONNECTED to PORT @ BAUD]` append.
- Call sites deliberately NOT wrapped:
  - Initial banner write in `startDumpViaController` — a new
    dump should always show the banner. "Preserve scroll" on
    new-dump-start would confuse users returning from a long
    prior dump.

**Files touched:** `src/config/AppSettings.java` (+21 lines),
`src/app/Main.java` (1101 → 1216, +115 lines). No new classes;
no new tests.

**Build / test / lint status.** `javac` clean, `.jar` repackaged,
86/86 tests green, lints clean.

**Hardware gate (2 cases):**

1. **Save Dump… remembers dir** — save to a non-default directory,
   close app, reopen, run a dump, File → Save Dump… — the file
   chooser should open on that directory, not `user.home`.
2. **Scroll preservation mid-dump** — start a long dump
   (ADDR `0000`, BYTES `2048`), wait until the pane has multiple
   screens of output, scroll up a few lines, confirm subsequent
   batches do NOT yank you back to the bottom; scroll to the
   bottom and confirm the sticky-to-bottom behavior is restored
   (next batch scrolls with the growing output).

---

### 2026-04-21 — M6 hardware-accepted

User ran the five-case M6 smoke matrix (§8 "M6 scope locked"
entry, below) on real PK-232 hardware — all gates green. Menu bar
is live, `File → Save Dump…` writes HEX + ASCII files matching the
on-screen rendering, `File → Exit` produces the expected
`exit via File → Exit` → `PK232Client: stopped` → `TX HOX` → ack /
BREAK trace, `Settings → Port…` reconnect works on both the happy
path and the dump-in-flight edge, and `Help → About` shows correct
version + environment info.

**Status.** M1 / M2 / M3a / M3b / M3c / M4 / M5 / **M6** all
hardware-accepted. 86/86 unit tests green. No open gates.

**What's left** (from truenorth §7 and the M6 scope-lock
deferrals):

- **Structural refactor (previously "M6 prep", now tentatively
  M7):** promote the `Main.showPlaceholderMainWindow` to a proper
  `ui.MainFrame` class, extract the 128-col rendering pipeline
  into `ui.HexDumpView`, split detection/entry orchestration into
  `app.DetectionRunner`. Pure-refactor milestone; the user
  deferred it in M6 because "the current UI is looking good". A
  real ignition point would be if `Main.java` (1101 lines) or its
  unit-testability becomes painful — neither is blocking today.
- **Fat/uber jar packaging (previously M8):** single-file
  distributable that embeds `jSerialComm`. `build.bat` currently
  produces a thin jar + `run.bat` adds `lib/jSerialComm` to the
  classpath. Pure packaging; no behavior change.
- **Minor polish items from truenorth §7** (if user raises
  them): persist-last-save-dir preference, ETA smoothing,
  auto-scroll-unless-scrolled-up, byte-click-to-copy in the
  output pane, `File → Save Log…`, etc. None called out as
  required yet.

No code changes in this entry — pure milestone acceptance marker.

---

### 2026-04-21 — M6 code landed (menu bar: File / Settings / Help + Settings → Port… reconnect)

Follow-up to the M6 scope-lock entry (next below). All four menu
items implemented verbatim; no new classes, no new packages, all
additions in `src/app/Main.java`.

**Code deltas:**

- `src/app/Main.java` (723 → 1101 lines, +378). New surface:
  - `static final String APP_VERSION = "0.6.0-M6"` — displayed in
    the About dialog.
  - `static final AtomicBoolean reconnectInProgress` — gate against
    overlapping `Settings → Port…` clicks.
  - `buildStatusBarHtml(SerialLink, ModemState)` — refactored out
    of `showPlaceholderMainWindow` so the reconnect worker can
    produce byte-identical status-bar HTML after a port swap.
  - `buildMenuBar(frame, log, session, statusBar, output,
    linkRef, clientRef, dumpCtlRef, shutdownDone)` — constructs
    `File` / `Settings` / `Help` menus with the three action items
    per the scope lock. All listeners are closures over the
    AtomicReference slots so reconnect-driven ref swaps are
    picked up transparently by subsequent clicks.
  - `doSaveDumpClick(frame, session, link, log)` — Ctrl+S
    handler. `JFileChooser` starts at `user.home` with a default
    name of `dump_$AAAA_NNNbytes_<mode>.txt`. Writes a small
    metadata header (`Port / Timestamp / Start address / Bytes /
    View mode`) followed by `DumpController.renderBuffer(...)`
    output. Empty mirror shows "No dump data available. Run a
    dump first." and returns. Failures log + show a modal error.
  - `doExitClick(...)` — calls `runShutdown` with reason
    `"exit via File → Exit"` then `System.exit(0)`.
  - `showAboutDialog(parent)` — `JOptionPane` HTML with app name,
    `APP_VERSION`, Java version, OS name/version, absolute Logs
    directory path.
  - `doSettingsPortClick(...)` — guards against overlapping
    reconnects via `reconnectInProgress`; if a dump is running,
    shows the "Cancel in-progress dump and reconnect?" modal; on
    OK cancels the dump; shows `StartupConnectDialog` parented on
    the main frame; if the user picks a port, dispatches
    `runReconnectOnWorker`.
  - `runReconnectOnWorker(...)` — ~130-line sequence on the
    `MemoryInspector-reconnect` non-daemon worker:
    1. Wait up to 2500 ms for any in-flight dump to exit.
    2. `client.close()` on the old client (500 ms × 2 joins cap).
    3. `HostModeEntry.tryExit(oldLink, log)` on the old link —
       double-SOH `HO N` + BREAK fallback + 1500 ms cap (§5.11).
    4. `oldLink.close()`, null all three refs.
    5. Open new link at user-chosen port/baud. Failure →
       dispose progress dialog + modal error + worker exits
       (app stays running with no active connection; a fresh
       Settings → Port… retry is the recovery path).
    6. `StartupDetector.detect()` on the new link. Same failure
       handling.
    7. `HostModeEntry.enter()`. Same failure handling; safe-close
       the link on failure so a half-opened state doesn't linger.
    8. Construct + `start()` a new `PK232Client`; construct a new
       `DumpController` with a fresh idle unsolicited handler
       (identical shape to the boot-path handler). Install both
       in the existing AtomicReference slots.
    9. EDT hop: dispose progress dialog, update status-bar HTML
       via `buildStatusBarHtml`, update frame title to include
       the new port name, append `[RECONNECTED to PORT @ BAUD]`
       marker to the output pane and auto-scroll.
    10. `finally` block clears `reconnectInProgress`.
  - `safeClose(linkRef, link)` — null-and-close helper for
    reconnect failure paths; avoids leaking a half-opened
    `SerialLink` if one of the later steps fails.
  - `reconnectFailUi(frame, progress, userMsg, log, logLine)` —
    shared UI path for reconnect failures (log line + EDT-hop
    progress-dialog dispose + modal error).
- Title format: `"Memory Inspector — M6 placeholder — <PORT>"`.
  First-open uses the boot link's port; reconnect updates it to
  the new port. Keeps the active connection visible on the
  taskbar.

**Not touched:**

- No new classes in `ui/` or `dump/`.
- No `MainFrame` / `HexDumpView` / `DetectionRunner` extraction.
- No new tests (same policy as `StartupConnectDialog`,
  `HostModeEntry`, `Main` itself — UI + worker-thread glue is
  hardware-gated per Option-3).
- HEX/ASCII radios stay on the bottom bar — NOT mirrored into a
  View menu (user preference: "current UI is looking good").

**Build / test / lint status:**

- `javac` clean.
- 86/86 tests green (unchanged).
- Linter clean.
- `.jar` repackage was blocked on the in-session `javaw` test
  process holding the file open — a non-issue; `out/` contains
  the fresh classes and the tests ran against them. The next
  clean `.\build.bat` after the test window closes will regenerate
  the jar without issue.
- File sizes: `Main.java` 723 → 1101 (over the 350 soft cap by a
  wider margin; user deferral still in effect). `DumpController.java`
  330 (unchanged). `DumpControllerTests.java` 182 (unchanged).

**Hardware gate.** Five cases per the scope-lock entry below
(1: menu bar open / mnemonics; 2: Save Dump HEX + ASCII;
3: File → Exit; 4: Settings → Port… happy + dump-cancel edge;
5: Help → About). User to run on real hardware and paste log
excerpts — especially interesting: case 4's log should show the
teardown + reopen sequence and the `reconnect: complete` marker.

---

### 2026-04-21 — M6 scope locked (menu-bar only; no `MainFrame` / `HexDumpView` / `DetectionRunner` extraction)

User explicitly scoped M6 down after the M5 hardware gate closed:
**keep the M5 placeholder window as-is — just add a menu bar**.
This retires (for M6) three previously-planned extractions:

- `ui.MainFrame` promotion — deferred. The placeholder window is
  "providing the necessary functionality" (user 2026-04-21) and the
  JMenuBar attaches cleanly to it. `ui.MainFrame` as a separate
  class moves to M7+ if ever needed.
- `ui.HexDumpView` class extraction — deferred. The M5
  `rerenderDumpView` + `DumpController.renderBuffer` pipeline is
  working; no visible UX win from promoting it now.
- `app.DetectionRunner` split — deferred. `Main.java` file-size
  concern remains user-waived.

**What M6 DOES include (all four answers verbatim):**

- **`File → Save Dump…`** (Ctrl+S mnemonic). Writes the current
  `DumpSession` buffer to a text file via a `JFileChooser`. Output
  format = metadata header (`Port / Timestamp / Start address /
  Bytes / View mode`) + blank line + the exact same render that
  the UI currently shows (`DumpController.renderBuffer(mirror,
  mirrorLen, startAddr, viewMode)`). Default filename
  `dump_$AAAA_NNNbytes_<mode>.txt`. Grayed-out-by-message when the
  session mirror is empty — clicking with nothing to save shows
  "No dump data available; run a dump first." and returns. The
  HEX/ASCII toggle in the main window decides which format saves
  (simplifies UX; no separate HEX+ASCII export path).
- **`File → Exit`.** Same behavior as the window-close handler —
  calls `runShutdown` with a distinct reason string
  (`"exit via File → Exit"`) then `System.exit(0)`.
- **`Settings → Port…`.** Opens the existing
  `StartupConnectDialog` (reused verbatim — no new dialog class),
  then runs a reconnect sequence on a dedicated
  `MemoryInspector-reconnect` non-daemon worker. Sequence:
  1. If a dump is running, prompt "Cancel in-progress dump and
     reconnect?" — Cancel aborts the menu action; OK cancels the
     dump and waits up to 2500 ms for the worker to unblock (the
     in-flight `sendAndAwait` completes within `DEFAULT_TIMEOUT_MS
     = 1500 ms`).
  2. `client.close()` (500 ms × 2 thread-joins).
  3. `HostModeEntry.tryExit(oldLink, log)` — double-SOH `HO N`
     + 500 ms ack wait + 300 ms BREAK fallback (§5.11 unchanged).
  4. `oldLink.close()`, null refs.
  5. Open the user-picked port; run `StartupDetector.detect()`;
     run `HostModeEntry.enter()`; construct + `start()` a new
     `PK232Client`; construct a new `DumpController` with the
     same idle unsolicited handler shape; install both into the
     existing `AtomicReference` slots so the Dump / Cancel /
     HEX / ASCII buttons pick them up transparently.
  6. Update the status-bar JLabel HTML with the new port / baud /
     modem state. Append a `[RECONNECTED to PORT @ BAUD]` marker
     to the output pane so history reads sanely.
  Failure at any step (open fails, detection fails, host-mode
  entry fails, client start fails) disposes the progress dialog
  and shows a modal error; the app remains running with no active
  link. A fresh `Settings → Port…` retry is the recovery path.
  An `AtomicBoolean reconnectInProgress` guard prevents overlapping
  reconnects (second click while the first is mid-flight shows a
  "reconnect already in progress" warning).
- **`Help → About`.** `JOptionPane.showMessageDialog` with HTML:
  app name + version constant (`APP_VERSION = "0.6.0-M6"`) + short
  description + Java version + OS name/version + absolute Logs
  directory path. No separate `ui.AboutDialog` class — one-shot
  standard dialog is sufficient.

**Menu layout confirmed:**

```
File        Settings    Help
└─ Save Dump… Ctrl+S    └─ Port…    └─ About
└─────────
└─ Exit
```

HEX / ASCII radios **stay on the bottom bar** — not mirrored
into a `View` menu. User said "current UI is looking good"; moving
the toggle into a menu would make the active view-mode less
visible.

**Implementation notes locked:**

- Everything lives in `Main.java`. No new classes in `ui/` or
  `dump/`. `Main.java` grows (723 → ~900 estimated). User's
  standing file-size waiver (§10 "File-size caps are deferred")
  covers this; flagged in the code-landing Change Log entry.
- A new `buildStatusBarHtml(SerialLink, ModemState)` helper
  centralizes the status bar's HTML so both the first-open and
  the reconnect paths produce byte-identical output.
- `runDetectAndEntryLoop` (boot path) is NOT factored into a
  shared helper with the reconnect worker — they share a lot
  conceptually but diverge in progress-label content, failure
  UX (exit-app vs. stay-open), and handle-passing. Factoring
  would mean an extra helper class (revisit in M7 alongside
  `app.DetectionRunner`); for now the reconnect worker duplicates
  the sequence inline, flagged clearly in its javadoc.
- Save Dump uses standard `FileWriter` + `PrintWriter` (no new
  dependencies). Unicode via the JVM default charset is fine —
  the content is all printable ASCII and hex digits.
- `StartupConnectDialog` accepts a `Window` parent; current boot
  code passes `null`. The M6 Settings-Port click will pass the
  main frame so the dialog centers on the app instead of the
  screen.
- No new unit tests. All M6 additions are UI / worker-thread
  glue — hardware-gated per Option-3 (same policy as
  `StartupConnectDialog`, `HostModeEntry`, `Main` itself).

**Hardware gate (5 cases) once M6 code lands:**

1. **Menu bar shows + opens** — File / Settings / Help visible;
   each menu opens with the expected items and mnemonics.
2. **File → Save Dump…** — after running a dump (any size), Save
   Dump… opens a file chooser; confirm the saved file matches the
   on-screen rendering exactly (HEX view saves HEX; flip to ASCII
   and save; confirm ASCII file); log shows
   `dump saved to <path> (<N> bytes, <MODE>)`.
3. **File → Exit** — clean shutdown, same log trace as
   window-close (`exit via File → Exit` + `PK232Client: stopped`
   + `TX HOX` + `RX HOA` or BREAK fallback).
4. **Settings → Port…** — happy path: connects to a different
   COM port (or the same one again), status bar updates,
   `[RECONNECTED to ...]` marker appears; a fresh Dump works on
   the new port. Edge: during an in-progress dump — the prompt
   appears, Cancel aborts the reconnect, OK cancels dump and
   proceeds.
5. **Help → About** — shows the About dialog with the correct
   version, Java version, OS, and Logs path.

---

### 2026-04-21 — M5 hardware-accepted + 128-byte HEX-only row tweak

User ran the five-case M5 smoke matrix (§8 M5 design-lock entry,
below) on real PK-232 hardware — all gates green. With M5 accepted,
one same-session layout request landed as a small follow-up to
`DumpController.renderBuffer` and its tests:

- **Row width 16 → 128.** Per project-status §6 M5 target ("128-col
  layout"); previously hedged at 16 because it matched the M4
  placeholder's 80-col `JTextArea`. HEX-mode rows are now
  `$AAAA: XX XX ... XX` with up to 128 hex bytes per row; ASCII-mode
  rows are 128 printable-or-dot chars per row.
- **Drop ASCII tail from HEX mode.** Previously HEX mode rendered
  `$AAAA: XX XX ... XX  <ASCII-of-those-bytes>`; user observed that
  with the HEX/ASCII radio selector this was redundant — the radio
  already decides which representation to show. Now HEX mode shows
  hex only; ASCII mode shows ASCII only. Delegated rendering to the
  existing `HexUtils.bytesToSpacedHex(data, off, len)` and
  `HexUtils.bytesToAsciiRender(data, off, len)` helpers — no new
  hex-row padding logic needed (partial rows are naturally
  short-ended by the helper).

**Tests.** `DumpControllerTests`:

- `render_hex_one_full_row_printable` rewritten as
  `render_hex_mode_no_ascii_tail` — asserts the ASCII rendering of
  the same bytes is NOT present in the output.
- `render_hex_two_rows_address_increment` (old 16-byte assumption)
  + `render_hex_partial_final_row_is_padded` (old ASCII-column
  alignment) replaced by a single `render_hex_128_bytes_per_row` —
  257 bytes span three rows at `$BF00` / `$BF80` / `$C000`, partial
  final row is 1 byte (`00`).
- `render_hex_row_width_fits_80_cols` removed (128-byte rows do not
  fit in 80 cols by design — that was an M4 placeholder constraint).
- Net delta: 10 → 10 `DumpControllerTests` (one new, three
  retired-or-merged). **Total: 88 → 86 tests green.**

No other files touched. Builds clean, lints clean.

**Status.** M1 / M2 / M3a / M3b / M3c / M4 / **M5** all
hardware-accepted. Only file-level change vs. the M5 design lock:
the 128-byte / HEX-only-in-HEX-mode rendering above. Next step is
M6 scoping (per §7 in the project-status handoff) — `ui.MainFrame`
promotion + `ui.HexDumpView` extraction + `app.DetectionRunner`
split + menu bar + Settings → Port… re-entry + 2/3 screen sizing
(C9).

---

### 2026-04-21 — M5 code landed (`dump.DumpController` + placeholder UI)

Follow-up to the M5 design-lock entry (next below). All seven locked
answers were implemented verbatim — this entry records the concrete
code deltas and the test-count increment.

**New package: `src/dump/`** (previously empty on disk since M1).

**New class: `src/dump/DumpController.java`** (330 lines). Surface:

- `public DumpController(PK232Client, PacketLogger, Consumer<HostBlock> idleUnsolicitedHandler)` —
  captures the idle handler so the swap / restore dance (Q4) is
  self-contained; `null` gets a no-op default.
- `public void start(int addr, int bytes, Listener)` — validates
  address / byte range / addr+bytes ≤ 0x10000 (defensive against a
  buggy caller; `DumpPromptDialog` still clamps per C4), then spawns
  the `MemoryInspector-dump` worker. Throws `IllegalStateException`
  if another dump is already running (C2 gate, belt-and-braces with
  the UI's Dump-button disable).
- `public void cancel()` — flips `volatile boolean cancelled`.
  Idempotent; safe from any thread.
- `public boolean isRunning()` — reflects worker state for EDT
  button-enablement checks.
- `public static String renderBuffer(byte[], int length, int startAddr, ViewMode)` —
  pure rendering helper, made public (not package-private) because
  `app.Main` also calls it for HEX↔ASCII toggle re-renders. HEX
  mode: `$AAAA: XX XX ... XX  ASCII` with partial-row hex padding so
  the ASCII column stays aligned. ASCII mode: `$AAAA: <printable-or-dot>`.
- `public enum ViewMode { HEX, ASCII }` and
  `public enum Outcome { COMPLETED, CANCELLED, ABORTED, FAILED }`.
- `public interface Listener { onBatchReady(...); onCompleted(...); }` —
  both callbacks fire on the EDT via `SwingUtilities.invokeLater`.
  `onBatchReady` delivers a fresh `byte[] newChunk` (≤ 32 bytes) per
  batch so the UI doesn't have to snapshot the full buffer each time.
  `onCompleted` delivers the trimmed `byte[] fullBuffer` + outcome.

**Worker-loop internals:**

- `client.setUnsolicitedHandler(this::captureUnsolicited)` installed
  at loop entry; restored to `idleUnsolicitedHandler` in `finally`
  (Q4). `captureUnsolicited` flips `volatile HostBlock abortBlock`
  on any `0x5F` (`isStatusOrError()`) block and forwards to the
  idle handler so traces still see the block.
- Cancellation check + abort-block check run before every
  `readOneByte()` call (Q3 flag-only contract). An in-flight
  `sendAndAwait` is NOT interrupted — worst-case unblock latency is
  the client's 1500 ms `DEFAULT_TIMEOUT_MS` on the current byte.
- Throughput timer starts AFTER `client.setAddress(addr)` returns,
  so the one-shot AE round-trip doesn't depress Bps.
- Batch boundary = every 32 bytes OR the final byte (Q6). One
  `invokeLater` per batch that publishes `(bytesSoFar, totalBytes,
  bps, etaMs, newChunk[])`.
- `Outcome.COMPLETED` on full read; `Outcome.CANCELLED` on flag
  flip; `Outcome.ABORTED` on unsolicited 0x5F; `Outcome.FAILED`
  on `IOException` / `RuntimeException`. `Outcome.CANCELLED` also
  used when the worker is interrupted (defensive; the current UI
  never sets the interrupt flag).

**New tests: `test/dump/DumpControllerTests.java`** (182 lines, 10
tests). Pure `renderBuffer` coverage:

- null/empty input, length=0, null-mode, length > data.length,
- HEX full row (printable + non-printable mix),
- HEX partial final row alignment (checks ASCII column offset
  matches a full-row reference so the spacing math survives future
  tweaks),
- HEX two full rows with `$BF00 / $BF10` address increment,
- ASCII mode printable + dot handling,
- ASCII mode full printable row,
- subset rendering (length < data.length),
- 80-col width sanity check (HEX full row ≤ 80 chars so the M4/M5
  `JTextArea(24, 80)` displays without wrap).

Thread / cancel plumbing / listener dispatch stay hardware-gated.

**Modified: `src/app/Main.java`** (617 → 723 lines, +106). Changes:

- New imports for `dump.DumpController` + inner types,
  `protocol.HostBlock`, `util.HexUtils`, `ButtonGroup`,
  `JRadioButton`, `Consumer`.
- Added `AtomicReference<DumpController> dumpCtlRef` to the boot
  chain alongside the existing `linkRef` / `clientRef`. Threaded
  through `boot`, `startDetection`, `runDetectAndEntryLoop`,
  `failFatal`, `showFatalErrorAndExit`, `showPlaceholderMainWindow`,
  `runShutdown`. `dumpController` is constructed once per successful
  `PK232Client.start()` and stored in the ref before the placeholder
  window opens.
- `runDetectAndEntryLoop` now captures the idle unsolicited handler
  in a named `Consumer<HostBlock>` local so it can be passed both to
  `client.setUnsolicitedHandler(...)` and to the `DumpController`
  constructor (Q4 handler-swap wiring).
- `showPlaceholderMainWindow`: new layout in the SOUTH region —
  `BorderLayout` with WEST = `Dump…` / `Cancel` buttons + HEX / ASCII
  `JRadioButton` pair, EAST = progress / Bps / ETA labels.
  Cancel button starts disabled; enabled by `startDumpViaController`,
  re-disabled on completion. HEX/ASCII radio group is backed by a
  `DumpSession` state holder that retains the dump's mirror buffer,
  length, start address, total bytes, and view mode — the toggle
  listeners call `rerenderDumpView(output, session)` which does a
  full `output.setText(...)` from the mirror. Title updated
  `M4 placeholder` → `M5 placeholder`.
- New helper `startDumpViaController(...)` — resets the session
  mirror, paints the banner line + zeroed labels, disables Dump +
  enables Cancel, and installs a fresh `DumpController.Listener`.
  `onBatchReady` copies the new chunk into the mirror, re-renders,
  and updates progress / Bps / ETA labels. `onCompleted` re-seats
  the mirror to the controller's final buffer (handles the
  trimmed-on-cancel/abort case), renders, appends a `[DUMP ...]`
  footer line, re-enables Dump, disables Cancel, and shows a modal
  error dialog on `ABORTED` / `FAILED`.
- New helper `footerFor(Outcome, bytesRead, startAddr, failureMessage)`
  — central formatting for the `[DUMP COMPLETED / CANCELLED /
  ABORTED / FAILED at byte N ...]` trailer line.
- New helper `formatBps(double)` — `"123.4 B/s"` or `"— B/s"` when
  the rate isn't meaningful yet.
- New helper `formatEta(long ms)` — `"mm:ss"` or `"--:--"` for
  unknown; caps at `"99:59+"` for very-slow edge cases.
- New helper `rerenderDumpView(output, session)` — always
  `setText` (not `append`) because HEX↔ASCII toggle requires a full
  re-render and this also simplifies the incremental-batch path
  (cross-row alignment from partial chunks is nontrivial). Prepends
  the `== Dump $XXXX..$YYYY (N bytes) ==` banner. Auto-scrolls to
  end. Performance note: re-rendering a 99,999-byte buffer is ~100
  KB × ~3,125 batches = ~300 MB of String allocation over a full
  dump run, comfortably within GC headroom for Java 17.
- Removed old `runDumpOnWorker(...)` (~60 lines) and the
  `DUMP_BATCH_BYTES` constant (now `DumpController.BATCH_BYTES`).
- `runShutdown` gained a `AtomicReference<DumpController>` parameter.
  New ordering: **cancel dump** → close client → `tryExit` → close
  link → close log. Cancelling the dump before closing the client
  is a pure optimization (§5.11 ordering preserved; client.close()
  alone would still force the dump worker to exit, but via a less
  graceful `IOException` path). Updated the method's javadoc to
  reflect the new step ordering.

**Not touched (still placeholder-level):**

- No `ui.MainFrame` promotion (Q2 deferred).
- No `app.DetectionRunner` extraction (Q2 deferred).
- No `ui.HexDumpView` class (still inline in `Main` via
  `rerenderDumpView`; extraction deferred to M6 alongside the 2/3
  screen sizing (C9) and menu bar).
- No "user scrolled up → don't auto-scroll" UX; always scrolls to
  end. Acceptable for the M5 placeholder.

**Build / test status.**

- `build.bat` clean.
- 88/88 tests green (78 → 88, +10 `DumpControllerTests`).
- Lint: clean.
- File sizes: `Main.java` 617 → 723 (over the 350 soft cap; user
  deferral in effect); `DumpController.java` 330 (under the 400
  soft cap); `DumpControllerTests.java` 182.

**Hardware gate open.** The five M5 cases locked in the design
entry (below) are the pass/fail set. User to run on real hardware
and paste `Logs\memory_inspector.log` excerpts; the most revealing
is Case 2 (Cancel mid-dump) — expect the `[DUMP CANCELLED at byte
N]` footer, `TX MM` / `RX MM` pairs stop within 1.5 s of the Cancel
click, Dump button re-enables, and the log shows `dump cancelled at
byte N`.

---

### 2026-04-21 — M5 design locked (`dump.DumpController` + placeholder extension)

The seven M5 Step-B scoping questions (project-status §7) were
answered. Code lands in the following Change Log entry once written;
this entry captures the design decisions ahead of implementation so
any drift is visible.

**Answers (verbatim for the record):**

- **Q1 — `DumpController` class location.** `dump.DumpController`
  per §5.3. The `dump` package has been empty on disk since M1; M5
  populates it. Rejected alternatives: folding into `ui`
  (layering smear) or `app` (muddies boot/shutdown responsibility).
- **Q2 — UI home.** **Keep extending `Main.showPlaceholderMainWindow`.**
  User opted to defer the `ui.MainFrame` promotion + `app.DetectionRunner`
  extraction (my §7 Step-B recommendation). Net: `Main.java` will grow
  past its current 617 lines — the user has standing authorization to
  ignore the file-size soft cap (§10 "File-size caps are deferred")
  and we flag the growth here but don't block on it. `ui.MainFrame`
  promotion moves to M6+ alongside `HexDumpView` extraction and
  2/3-screen sizing (C9).
- **Q3 — Cancel contract.** **Flag only.** A `volatile boolean cancelled`
  is checked between every `readOneByte` call in the dump loop. Simpler
  than the flag-plus-interrupt alternative; trade-off is the last
  in-flight `sendAndAwait` may take up to the 1500 ms
  `DEFAULT_TIMEOUT_MS` to unblock if the modem is slow to respond
  (usually it's already on the wire and finishes in < 100 ms).
  No changes to `PK232Client` thread-interrupt handling.
- **Q4 — `0x5F` unsolicited handler wiring.** **Swap-in at
  `start()` / restore in `finally`.** `DumpController.start` calls
  `client.setUnsolicitedHandler(block -> notifyAbort(block))` before
  the dump loop and restores the no-op handler in `finally`. A `0x5F`
  (`CTL_STATUS_ERROR`) block during the dump flips a
  `volatile HostBlock abortBlock`; the loop checks it on the next
  iteration and raises a `DumpAbortedException` surfaced to the EDT
  via the completion callback. Handler-swap pattern preserved for
  future error types (e.g. a per-dump unsolicited-data dialog in M7+).
- **Q5 — HEX↔ASCII toggle buffer.** **Full `byte[]` kept for the
  lifetime of the dump result.** Even at the 99,999-byte ceiling
  that's ~100 KB — trivially fine. Toggle calls a pure
  `renderBuffer(byte[] data, ViewMode mode, int startAddr)` which
  returns a complete `String`; `Main` replaces the `JTextArea`
  contents in one `setText` call. Rejected: line-level caches
  (doubles memory for no real win at this scale) and lazy
  render-on-scroll (adds state we don't need).
- **Q6 — UI batching cadence.** **Same 32-byte batches as M4.**
  `HexDumpView` rendering is still inside `Main` (placeholder
  window) for M5 — append-only, auto-scroll-to-bottom unless user
  has scrolled up (§5.7). One `SwingUtilities.invokeLater` per
  32-byte batch (or at the final byte). Bps and ETA labels are
  computed inside `DumpController` on the worker thread and
  published with the same batch event — they update every ~1–6 s
  depending on link speed, which is acceptable for M5.
- **Q7 — `AE` re-issue on every Dump press.** **Always re-issue.**
  `DumpController.start(addr, bytes, …)` unconditionally calls
  `client.setAddress(addr)` as the first command in the loop. Never
  relies on the modem's auto-increment from a prior dump. C7
  re-locked; the "skip if contiguous" optimization is explicitly
  rejected as it breaks determinism.

**Design notes derived from these answers:**

- **Public API of `DumpController`** (all thread-safe, callable from
  the EDT):
  - `start(int addr, int bytes, Listener l)` — spawns
    `MemoryInspector-dump` non-daemon worker; idempotent guard
    throws if a dump is already in flight.
  - `cancel()` — sets the `volatile boolean cancelled` flag;
    worker observes on its next iteration.
  - `isRunning()` — reflects worker state, used to gate Dump /
    Cancel button enablement on the EDT.
  - `Listener` — callback contract: `onBatchReady(int bytesSoFar, int totalBytes, double bytesPerSecond, long etaMillis, byte[] bufferSnapshotSoFar)` delivered on EDT;
    `onCompleted(byte[] fullBuffer, int startAddr, Outcome outcome, String failureMessage)` delivered on EDT.
  - `ViewMode { HEX, ASCII }` enum + static
    `renderBuffer(byte[] data, int startAddr, ViewMode mode)` pure
    helper — unit-testable, Option-3 relaxed pattern (same rationale
    as M4's `parseMMPayload` extraction).
- **Bps / ETA math.** Bps = (bytesRead since dump start) /
  (wall-clock seconds since dump start). ETA = (bytesRemaining) /
  Bps. Formatted as `123.4 B/s` and `mm:ss` (or `--:--` if the
  first batch hasn't landed yet). First batch's Bps is suppressed
  to avoid the startup spike from the one-shot `AE` round-trip.
- **128-col rendering (C9).** The HEX-mode render produces
  16-bytes-per-row lines of the form
  `$AAAA: 00 01 02 ... 0F  ........OMM.....`; that's 77 chars of
  content. For an 80-column `JTextArea` this fits without wrap.
  The 128-col target in project-status §6 M5 is about the
  eventual `HexDumpView` sizing, not line content — deferred
  alongside `HexDumpView` to M6+.
- **Shutdown interaction.** `Main.runShutdown` keeps the same
  §5.11 ordering: `dumpController.cancel()` (if running) →
  `client.close()` → `HostModeEntry.tryExit(link, log)` →
  `link.close()` → `log.close()`. The cancel-before-close step
  is new but is a pure optimization — without it, `client.close()`
  interrupts both client threads and the dump worker's pending
  `sendAndAwait` unblocks via its eventual timeout, which would
  violate the 1500 ms close budget. `cancel()` first makes the
  worker bail cleanly on the next iteration.
- **Testing policy.** `DumpController.renderBuffer` is pure logic —
  gets unit tests (HEX + ASCII modes, startAddr offsets, partial
  final row, 0-byte buffer). Worker lifecycle / cancel plumbing /
  listener dispatch stay hardware-gated (Option-3 strict, same as
  `PK232Client` threading).

**Hardware gate.** Five cases will be required before M5 is
accepted:

1. **Small round-trip** (ADDR `0000`, BYTES `16`) — identical
   expected log evidence to M4 case 1, but now routed through
   `DumpController`; view shows 16 bytes in HEX mode; toggle to
   ASCII re-renders from the buffered `byte[]`.
2. **Cancel mid-dump** (ADDR `0000`, BYTES `4096`, press Cancel
   after ~2 s) — dump stops within 1.5 s, output pane shows a
   `[DUMP CANCELLED at byte N]` footer, Dump button re-enabled.
3. **HEX/ASCII toggle** mid-dump — render updates with no data
   loss; progress label keeps incrementing; no flicker.
4. **`0x5F` abort** (if synthesizable on the bench) — unsolicited
   status/error block during a dump aborts with a modal dialog;
   `[DUMP ABORTED: <reason>]` footer appears.
5. **Close window mid-dump** — identical to M4 case 5, but with
   `DumpController.cancel()` in the shutdown ordering; dump
   worker exits within the 1500 ms close budget.

---

### 2026-04-21 — M4 hardware-accepted

User ran the five-case §8 M4 smoke matrix on real PK-232 hardware
after the MM payload `$`-separator fix (previous entry). All cases
passed with the expected UX and log evidence — `TX AE` / `RX AE`
with `00` status byte, N× `TX MM` / `RX MM  ... 24 <hi> <lo> ...`
round-trips, `dump completed: <n> bytes from $<addr>` footers, C3
snark popup on 0 bytes, C4 end-of-memory clamp on `FFF0 + 32`, ESC
cancel from `DumpPromptDialog`, and clean shutdown when the window
is closed mid-dump (client stopped before `HostModeEntry.tryExit`
per §5.11 ordering).

**Status.** M1 / M2 / M3a / M3b / M3c / M4 all hardware-accepted.
78/78 unit tests green. No open gates before M5. Next step per §7
is M5 scoping (Step-F questions on `dump.DumpController` location,
`ui.MainFrame` promotion, Cancel contract, `0x5F` handler wiring,
HEX/ASCII toggle buffer, UI batching cadence, and `AE` re-issue on
every Dump press — answers landed in the following Change Log entry
when M5 design is locked).

**No code or behavior changes in this entry** — pure milestone
acceptance + handoff marker.

---

### 2026-04-21 — MM payload `$` separator fix (parseMMPayload)

**Symptom.** Every `MM` round-trip on real hardware was failing with
`dump failed at byte 0: MM: malformed payload, got HostBlock{ctl=0x4F, payload=[4D 4D 24 30 31]}`.
The raw RX trace (`RX MM  01 4F 4D 4D 24 30 31 17   .OMM$01.`) was
actually a **correct** PK-232 response per the host-mode protocol —
the parser was wrong, not the modem.

**Root cause.** M4's `PK232Client.parseMMPayload` was coded against
the prior assumption (see §4.A1 before this date) that the `MM`
response payload is exactly 4 bytes — the two mnemonic echo bytes
`M M` followed by an ASCII-hex pair `<hi><lo>`. The hardware-observed
format is 5 bytes: **`M M $ <hi> <lo>`** — the mnemonic echo is
separated from the hex pair by a literal ASCII `$` (0x24). The
validator's length and index checks never accounted for that
separator, so every real response tripped the "malformed payload"
branch at byte 0 of the very first dump.

**Fix.** `parseMMPayload` now:

1. Requires `payloadLength() == 5` (was 4).
2. Asserts `payload[0..2] == 'M','M','$'`.
3. Reads the hex-pair from `payload[3]` (hi) and `payload[4]` (lo),
   unchanged decode via `HexUtils.fromAsciiHexPair`.

The javadoc on `readOneByte` and `parseMMPayload` was updated to
document the `MM$<hi><lo>` on-wire shape and cite this entry.

**Scope.** `AE` and the other M1–M3 command envelopes do **not** use
this `$<hex>` pattern, so no other parsers were touched.
`validateAEPayload` (`[0x41 0x45 0x??]` — mnemonic + status byte) is
unchanged. If a future `hCmd.md` entry is observed to use the same
`XX$<hex>…` pattern (candidates would be other "register-read"
commands), a shared helper can be extracted then; premature
generalization rejected for this hotfix.

**Tests.** `test/protocol/PK232ClientHelperTests.java` updated:

- All existing `parseMMPayload` fixtures extended to the 5-byte
  `{'M','M','$',<hi>,<lo>}` shape (canonical `3F`, `00`/`FF`
  boundaries, lowercase `ab`, non-hex `GH`, wrong-ctl / wrong-mnem /
  too-short / too-long envelope matrix).
- New `parseMM_decodes_hardware_observed_example` exercises the
  exact bug-report bytes (`4D 4D 24 30 31`) and asserts a decode of
  `0x01`.
- New `parseMM_rejects_missing_separator` guards against regression
  by feeding the pre-fix canonical shape `{'M','M','3','F'}` and
  asserting it is rejected as malformed. Net: `parseMMPayload` test
  count rises from 5 to 7; overall file rises from 9 to 11 tests.
  Total project tests: 76 → 78.

**Key-decision update.** §4.A1 was rewritten to name the `$`
separator explicitly, cite the hardware trace, and note that
`parseMMPayload` drains `MM$` before decoding the hex pair. The
original A1 text understated the payload shape — the `$` was
invisible in the reference manual's summary and only surfaced once
we had a live bus trace.

**No other files touched** — `setAddress`, `validateAEPayload`, the
reader/protocol threads, the dump worker, `DumpPromptDialog`, and
all other commands are unchanged. M4 hardware gate (§8 cases 1–5)
can now be re-run.

---

### 2026-04-20 — M4 design locked + code (setAddress / readOneByte / simple dump display)

The four M4 Step-E scoping questions (project-status §7 Step E) were
answered and the milestone's code landed. M4 is deliberately minimal
per truenorth §7: just enough to perform a real `AE` + N×`MM`
round-trip on hardware and eyeball the output. Full
`DumpController` with 128-col / HEX↔ASCII toggle / Bps / ETA / Cancel
ships in M5+.

**Answers (verbatim for the record):**

- **Q1 — Option-3 policy for M4.** **Relax.** `setAddress` and
  `readOneByte` validate payload shape + status byte + ASCII-hex pair
  decode — classic silent-regression territory. Implementation choice:
  the validation is extracted into two package-private static methods
  (`PK232Client.validateAEPayload` / `PK232Client.parseMMPayload`) so
  tests exercise them directly, avoiding a scripted-client subclass
  (which was our fallback) and avoiding any `SerialLink` mocking
  entirely. Net: `SerialLink` / thread lifecycle / correlation logic
  stay hardware-gated (unchanged from M3c); only the payload-decode
  static methods are unit-tested. Seven new tests land.
- **Q2 — `setAddress` signature.** `setAddress(int addr)` with
  range check `0 <= addr <= 0xFFFF`. Caller uses existing
  `HexUtils.parse4Hex(String)` for the UI's 4-char hex → int.
- **Q3 — `readOneByte` return type.** Primitive `int` (0..255).
  Any error (timeout, malformed payload, non-hex decode, non-zero
  status byte on the response) throws `IOException`
  (or `ProtocolTimeoutException`, an `IOException` subtype, on
  timeout). Caller catches once at the dump-loop level per §5.6.
- **Q4 — "Simple dump display" scope.** Minimal UI. The M3c
  placeholder window in `Main` gains: a **Dump** button wired to a
  new modal `ui.DumpPromptDialog` (ADDR 4-hex + BYTES 1..99999, with
  the C3 snarky 0-popup and C8 uppercase normalization), a
  `JTextArea` output pane (monospaced, line-wrap off) inside a
  `JScrollPane`, and a progress `JLabel` showing `n / N bytes`. A
  dedicated `MemoryInspector-dump` non-daemon worker thread runs
  `client.setAddress(addr)` then a N-iteration `client.readOneByte()`
  loop, batching EDT appends every 32 bytes (the M5 batching cadence,
  landed early because it's a trivial perf win and the right shape
  for the future `DumpController`). No 128-col layout, no HEX/ASCII
  toggle, no Bps/ETA, no Cancel button — all M5/M6. Closing the
  main window mid-dump still works (runShutdown kills everything;
  the dump worker dies cleanly via the existing client-stop path).

**Code deltas this milestone:**

- `src/protocol/PK232Client.java` (372 → 463 lines; see file-size note at end):
  - Public `setAddress(int addr)` — wraps `sendAndAwait` of a
    `0x4F` block with payload `"AE" + addr`. Calls the static
    `validateAEPayload` on the response.
  - Public `readOneByte()` — wraps `sendAndAwait` of a `0x4F`
    block with payload `"MM"`. Returns the static `parseMMPayload`
    result.
  - Package-private static `validateAEPayload(HostBlock)` —
    enforces `ctl == 0x4F`, payload exactly `[0x41 0x45 0x??]`,
    status byte `== 0x00`. Throws `IOException` with a diagnostic
    message on any mismatch.
  - Package-private static `parseMMPayload(HostBlock)` — enforces
    `ctl == 0x4F`, payload exactly `[0x4D 0x4D <hi> <lo>]`, decodes
    `<hi><lo>` via `HexUtils.fromAsciiHexPair` to an unsigned int.
    Throws `IOException` wrapping any decode failure.
- `src/ui/DumpPromptDialog.java` (new, ~135 lines):
  - `DumpPromptDialog.showDialog(Window parent, int defaultAddr, int defaultBytes)`
    returns a `Result(int addr, int bytes)` record or `null` on
    Cancel.
  - ESC/Cancel dismiss with no result. ENTER from either field
    submits (via a root-pane default button binding).
  - ADDR field: 4 chars max, `[0-9A-Fa-f]` live-filtered at the
    document level, displayed uppercase, parsed via
    `HexUtils.parse4Hex`.
  - BYTES field: up to 5 digits, leading zeros stripped on submit.
    `BYTES == 0` triggers `JOptionPane.showMessageDialog` with the
    verbatim C3 text `"OK ive done nothing are you happy?"` and
    leaves the dialog open.
  - `ADDR + BYTES > 0x10000` raises a warning dialog and returns
    the clamped value; the caller sees `bytes' = 0x10000 - addr`
    (truenorth C4 "stop at 0xFFFF").
- `src/app/Main.java` (478 → 617 lines; see file-size note at end):
  - Placeholder window re-laid out: `BorderLayout` with NORTH =
    existing status label (shortened), CENTER = `JScrollPane`
    wrapping a `JTextArea(24, 80)` (monospaced), SOUTH = a panel
    with a **Dump** button and a progress `JLabel`.
  - New `runDumpOnWorker(PK232Client, int addr, int bytes, ...)`
    method spawns a non-daemon `MemoryInspector-dump` thread.
    Loop body catches `IOException` / `InterruptedException` /
    `RuntimeException` and appends a `[DUMP FAILED: ...]` footer
    line before re-enabling the button. Successful completion
    logs `dump completed: <bytes> bytes from $<addr-hex>` via
    `logRaw`.
  - EDT batching: every 32 bytes OR at the final byte, a single
    `invokeLater` appends the accumulated hex chunk and updates
    the progress label. Per-byte `invokeLater` would swamp the
    EDT on a 99,999-byte dump.
  - Placeholder window title flipped `M3c placeholder` →
    `M4 placeholder`.
- `test/protocol/PK232ClientHelperTests.java` (new, ~165 lines):
  - 4 tests for `validateAEPayload` (canonical success, wrong ctl,
    non-zero status byte, malformed-payload matrix).
  - 5 tests for `parseMMPayload` (canonical `3F`, `00`/`FF`
    boundaries, lowercase hex, non-hex chars, malformed-envelope
    matrix).
  - 67 → 76 tests total.
- Doc deltas: §5.3 `ui` row adds `DumpPromptDialog`; §5.3
  `PK232Client` row expanded with the M4 helpers + test-extraction
  note. No §4 decision changes; no `logic`-file changes.

**Hardware-gate scope for M4:**

Same discipline as M3a/M3b/M3c — user-driven. The placeholder
window now has a working Dump button, so the gate is:

1. Small round-trip: ADDR `0000`, BYTES `16`. Dump button completes
   in < 1 s, `JTextArea` shows 16 hex bytes, log contains one
   `TX AE` line, one `RX AE` line with `00` status byte, 16
   `TX MM` + 16 `RX MM` lines, and a `dump completed` logRaw.
2. Clamp case: ADDR `FFF0`, BYTES `32`. Dialog warns about the
   `0x10000` overflow and clamps to 16 bytes; dump proceeds.
3. Zero-bytes case: ADDR `0000`, BYTES `0`. C3 snark popup fires
   with the exact literal text.
4. Cancel-from-dialog: Dump button → ESC in dialog. No dump
   starts, button re-enabled.
5. Close-mid-dump: start a larger dump (e.g. ADDR `0000`, BYTES
   `2048` ≈ 4 s at 9600 baud), then close the main window.
   Shutdown still completes cleanly within ~1.5 s, `TX HOX`
   + `RX HOA` still appear, dump-worker thread exits without
   blocking JVM exit.

Pass criteria: all five cases clean. Cases 1 + 3 are the must-haves;
cases 2, 4, 5 validate the UX guardrails.

File-size status: both soft caps blown further, user-authorized
("ignore file-size limits for now", 2026-04-20).

- `Main.java` 478 → 617 lines. Still past the 350 soft cap. The
  extraction of `ui.DumpPromptDialog` (+284 lines in its own file)
  prevented a ~900-line monstrosity; most of the +139 in `Main`
  is the new dump-UI block (button wiring, `JTextArea` output,
  font-fallback helper, worker-thread loop with
  `InterruptedException` / `IOException` / `RuntimeException`
  branches and a `finally` button-re-enable). The next growth
  increment (full `MainFrame` in M5/M6) will force the
  already-planned `app.DetectionRunner` split and a
  `ui.MainFrame` proper.
- `PK232Client.java` 372 → 463 lines. Past the 400-line class
  soft cap; the growth is `setAddress` + `readOneByte` +
  `validateAEPayload` + `parseMMPayload` + their javadoc. A
  split would separate the 2-thread infrastructure from the
  mnemonic-level helpers, but the cut-line isn't obvious until
  M5 adds more command wrappers; deferred to M5 when the full
  API surface is visible.

No protocol decision in §4 changes.

### 2026-04-20 — M3c hardware-accepted

`PK232Client` + `Main` lifecycle integration passed the project-status
§7 Step-D hardware-smoke gate on real PK-232 hardware (user sign-off
2026-04-20). Observations confirmed:

- On boot the placeholder window shows `PK232Client: running (2 threads)`;
  `memory_inspector.log` contains `PK232Client: started (reader + protocol threads)`
  immediately after `host-mode entry: resolved (HOSTMODE ready)`.
- On `MainFrame` close the shutdown path prints (in order):
  `exit via MainFrame close` → `PK232Client: stopped` →
  `TX HOX  01 01 4F 48 4F 4E 17  ..OHON.` → `RX HOA` (or the BREAK
  fallback lines). The `PK232Client: stopped` line appearing BEFORE
  `TX HOX` is the §5.11 ordering contract — confirmed working.
- No stray `RX UNS` / `RX ERR` lines were emitted while the client
  was idle in host mode (expected — modem is quiescent; the
  unsolicited routing path is still wired and will be exercised by
  `0x5F` error cases in M4+).

M3c is closed; **M4** (`PK232Client.setAddress(int)` +
`readOneByte()` helpers + a minimal dump display on the placeholder
window) is cleared to start once its scoping questions are answered.
No protocol decision in §4 changes; the §5.3 / §5.6 / §5.9 + §8
entries from the preceding 2026-04-20 design-lock entry remain
authoritative.

### 2026-04-20 — M3c design locked + code (PK232Client, 2-thread, Option-3 strict)

The five M3c Step-C scoping questions (project-status §7 Step C) were
answered and the `PK232Client` class implemented + wired into `Main`.

**Answers (verbatim for the record):**

- **Q1 — Reader thread vs. inline reads.** Keep the 2-thread model
  per §5.2. Serial Read Thread does blocking `SerialLink.read()`,
  feeds a single `HostCodec.Parser`, and `add`s emitted blocks to a
  `LinkedBlockingQueue<HostBlock>`. Protocol Thread `poll`s the queue,
  correlates against a single `pending` slot, and routes non-matching
  blocks to the user-supplied `Consumer<HostBlock>` unsolicited handler.
- **Q2 — Correlation API.** `sendAndAwait(HostBlock req, long timeoutMs)`
  returns the next `0x4F` block. Single outstanding enforced by a
  fair `ReentrantLock`. Default timeout `1500 ms` per §5.6; caller
  passes it explicitly. On timeout → `ProtocolTimeoutException`
  (extends `IOException` so callers can treat it as an I/O failure).
  Caller is expected to abort the wider operation on timeout — §5.6
  is clear that a per-command timeout aborts the dump.
- **Q3 — Unsolicited routing.** One `Consumer<HostBlock>` hook,
  invoked on the Protocol Thread. Receives any block that is not a
  `0x4F` response correlating to the current `pending` request —
  specifically: every `0x5F` (error/status), every non-`0x4F` class
  block (`0x2F`, `0x3F`, etc.), and any stray `0x4F` that arrives
  without an outstanding command. Handler exceptions are caught and
  logged via `logRaw`; they never poison the thread.
- **Q4 — Lifecycle.** `PK232Client` owns both threads and the
  `LinkedBlockingQueue<HostBlock>`. `start()` spawns them as
  non-daemon threads named `PK232-Reader` and `PK232-Protocol`.
  `close()` is idempotent (`AtomicBoolean`), signals `closed=true`,
  interrupts both threads, and joins each with a
  `SHUTDOWN_JOIN_MS = 500 ms` cap so `Main.runShutdown` can still
  fit under its own `1500 ms` wall-clock cap (§5.11). `Main.runShutdown`
  calls `client.close()` BEFORE `HostModeEntry.tryExit(link, log)` so
  the exit-ack read on the bare `SerialLink` does not race the
  client's reader.
- **Q5 — Unit tests.** **Option-3 strict.** `PK232Client` joins
  `StartupDetector`, `HostModeEntry`, `SerialLink`, `Main`, and the
  UI classes as hardware-gated-only. No `test/protocol/PK232ClientTests.java`
  lands. Rationale: adding unit tests would require a `Transport`
  interface on `SerialLink`, which is itself a classpath-visible
  design change, and the user deemed the extra surface not worth the
  regression coverage given that `PK232Client`'s behavior is
  dominated by timing and I/O anyway. Test count holds at 67/67
  through M3c; M4's `DumpController` re-opens this decision.

**Code deltas this milestone:**

- `src/protocol/PK232Client.java` (new): `start()` / `sendAndAwait` /
  `setUnsolicitedHandler` / `close()` / `isRunning()`. Nested
  `ProtocolTimeoutException extends IOException`. Private
  `PendingRequest` holder (CountDownLatch + volatile HostBlock).
  Mnemonic for TX/RX log lines is derived from the first two bytes
  of payload when they are ASCII-alpha (matches the convention for
  `AE` / `MM` / `HO` / `HP`-class commands); falls back to `CMD`
  for TX without an alpha prefix, `UNS` / `ERR` for RX.
- `src/app/Main.java` (+~20 lines): holds an
  `AtomicReference<PK232Client>`, starts the client on a
  `no-op + logRaw` unsolicited handler right after
  `HostModeEntry.enter()` returns, stops it at the top of
  `runShutdown` before `tryExit`. Placeholder window title/label
  flipped from `M3b placeholder` to `M3c placeholder` and a new
  status line shows `PK232Client: running (2 threads)` so the
  hardware gate can confirm the threads launched.
- §5.3 table row for `PK232Client` rewritten.
- §5.6 intro paragraph added: "all wire-level I/O in this section
  flows through `PK232Client.sendAndAwait`".
- §5.9 token table: added `TX <mnem>` / `RX <mnem>` / `RX UNS` /
  `RX ERR` plus client-lifecycle `logRaw` row.

**What M3c does NOT include** (explicit guardrails for the next
milestone):

- No `setAddress(int)` or `readOneByte()` helpers — those are M4.
- No `DumpController` integration — M5.
- No retry-on-timeout logic — per §5.6 timeout is terminal for the
  current operation; caller is responsible for higher-level recovery.
- No re-entry to host mode on reader failure — if the reader thread
  dies with an `IOException`, subsequent `sendAndAwait` calls will
  throw `IOException` wrapping it. Caller is expected to close the
  client + surface a dialog (M4 will bind this to a Dump-Abort path).

**Hardware-gate scope for M3c** (user runs, same discipline as M3a/M3b):

Minimal observation-only gate — the client is idle in host mode with
no commands issued, so the gate simply confirms (a) the threads
launch without crashing, (b) the placeholder window shows the new
status line, (c) closing the window stops the client cleanly and
`tryExit` still lands `TX HOX` + `RX HOA` (or BREAK fallback) within
the 1.5 s cap. Real `sendAndAwait` coverage lands when M4 does its
first `AE` + `MM` round-trip.

File-size position unchanged: `Main.java` grew from 432 → 478 lines,
still past the 350 soft cap. Per the 2026-04-20 deferral (this
Change Log, previous entry), this is accepted; the growth is the
minimum-viable client integration (new `AtomicReference<PK232Client>`
plumbed through `boot` / `startDetection` / `runDetectAndEntryLoop` /
`failFatal` / `showFatalErrorAndExit` / `showPlaceholderMainWindow` /
`runShutdown`, plus 12 lines of actual start/stop logic) and will be
split out when M5/M6 lands real MainFrame wiring. `PK232Client.java`
itself is 372 lines — well under the 400-line class soft cap.

No protocol decision in §4 changes.

### 2026-04-20 — M3b hardware-accepted; log readability + AUTOBAUD settle landed

`HostModeEntry.enter()` + `HostModeEntry.tryExit()` + `Main` Retry/Cancel
wiring passed the §5.11 / §8 seven-case hardware-smoke matrix on COM10
@ 9600 against a real PK-232 (user sign-off 2026-04-20). All four
starting-state branches (COMMAND / AUTOBAUD / HOSTMODE / OFF), the
entry-failure Cancel exit (case 5), the entry-failure Retry recovery
with port close+reopen (case 6), and the `MainFrame`-close shutdown
path with `TX HOX` / `RX HOA` or BREAK fallback (case 7) are all green.
M3b is closed; M3c (`PK232Client` + reader/protocol threads +
command-correlation queue) is cleared to start once Step-C scoping
questions are answered.

Landed alongside the gate:

- **Log readability**: `PacketLogger.streamRx(String)` + nested
  `RxLineBuffer` accumulate command-mode RX byte-at-a-time reads into
  CR-framed log lines, ALFD-aware (CR alone or CRLF — a pending CR
  absorbs an immediately-following LF into the same line). Five call
  sites migrated: `StartupDetector.awaitSubstring` (`RX DET`),
  `StartupDetector.waitForPowerOnActivity` (`RX BAN`),
  `StartupDetector.drainResidual` (`RX DRN`),
  `HostModeEntry.restart` (`RX RST`), `HostModeEntry.awaitSubstring`
  (`RX CMD`). +9 unit tests, 67/67 green.
- **AUTOBAUD two-phase match**: `StartupDetector.tryAutobaud` now
  continues past the `PK-232` substring hit and waits for the banner
  terminator `cmd:` within `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms`
  (separate entry below).

File-size soft caps in §10 / project-status §9 (`Main.java` 432 lines
past the 350 cap; `HostModeEntry.java` 420 lines just over the 400
cap) are **deferred** — user call 2026-04-20. Both files hold as-is
through M3c. The caps will be revisited when M5/M6 wires the real
`MainFrame` and `DumpController`, at which point a split of `Main`
into `app.DetectionRunner` (or similar) is likely anyway.

No protocol decision in §4 changes; no detection-spec decision
changes beyond those already logged below.

### 2026-04-20 — AUTOBAUD two-phase match (banner-settle wait for `cmd:`)

`StartupDetector.tryAutobaud` was returning as soon as the literal
`PK-232` substring appeared in the RX accumulator, typically within the
first ~50 bytes of the banner. The `POST_DETECT_DRAIN_MS = 150 ms`
window was far too short to drain the remaining ~200-byte banner at
9600 baud, so `HostModeEntry.enter()` would send `AWLEN 8<CR>` while
banner bytes — crucially including the banner's own terminal `cmd:` —
were still streaming in. Worst case: the verbose-ack window for `AWLEN`
matches on the banner's trailing `cmd:` and subsequent steps race the
modem's actual `AWLEN` echo.

Fix: two-phase match in `tryAutobaud`. Phase 1 is unchanged — wait up
to `PER_STEP_TIMEOUT_MS = 750 ms` for the `PK-232` substring so the
banner has started. Phase 2 keeps reading into the same accumulator
(and the same `RX DET` line-buffered log stream) until `cmd:` appears,
with a deadline of `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms` starting from
the moment `PK-232` was first seen. On phase 2 timeout, the detector
falls through (returns `null` from `runLadder`) rather than reporting
AUTOBAUD, so the OFF / power-cycle recovery path engages instead of
handing a still-streaming modem to `HostModeEntry`.

Deltas:
- `logic` (authoritative per E4): AUTOBAUD stage now reads "wait for
  `cmd:` after `PK-232` (end of banner) before exiting detection."
- §5.4 step 6: AUTOBAUD bullet updated to note the phase-2 wait.
- `src/protocol/StartupDetector.java`: `tryAutobaud` rewritten to run
  the two-phase loop inline (new constant `AUTOBAUD_BANNER_SETTLE_MS`).
  Dual-deadline pattern keeps the per-stage-start window and the
  post-match window independent.

No protocol decision in §4 changes; Q3 (case-sensitive `PK-232`
substring match) is preserved verbatim in phase 1. §8 hardware-smoke
matrix case 2 (AUTOBAUD) should be re-run to validate the fix.

### 2026-04-20 — `RESET`/`RESTART` correction (E2 flipped)

Previously E2 read "Never send `RESTART`". This was wrong and conflated two
distinct immediate commands in `hCmd.md`:

- **`RESET`** (`hCmd.md` §RESET, line 2776+): destructive — resets *all*
  parameters to PROM defaults, wiping MYCALL, MailDrop messages, monitor
  lists, and stored baud rates. This is the command that must never be
  sent.
- **`RESTART`** (`hCmd.md` §RESTART, line 2792+): non-destructive — soft
  reboot equivalent to turning the PK-232 off and on again, retaining all
  user settings and bbRAM values. This is **required** in the Host-Mode
  entry sequence because `AWLEN` (§AWLEN, line 334: "*The RESTART command
  must be issued before a change in word length takes effect*") and
  `PARITY` (§PARITY, line 2234: "*The change will not take effect until a
  RESTART is performed*") only apply their queued values on the next
  `RESTART` or power-cycle. Without `RESTART` the 8-bit word-length and
  no-parity settings host-mode needs would not actually be in force.

**Deltas from the prior "Q1–Q10 locked" entry below:**

- §2 Success Criteria: "without sending `RESET`" (was `RESTART`).
- §3 In Scope: entry sequence now lists `RESTART` between 8BITCONV and the
  resilient HOST-ON frame; `RESET` called out as the forbidden command.
- §4.E2: re-anchored on `RESET` as destructive / never-sent; `RESTART`
  promoted to a required step in §5.5.
- §5.3: `HostModeEntry` row updated to reflect the new five-step wire
  order with a 2000 ms settle after `RESTART`.
- §5.5: new step 2 — `RESTART<CR>` + 2000 ms RX drain (`RX RST`) + `cmd:`
  verify. Subsequent steps renumbered.
- §5.9 token table: added `TX RST` and `RX RST` for the soft-reboot send
  and settle window.
- §6 Risks: updated the "manual power-cycle requirement" row — `RESTART`
  now covers in-band recovery during entry; power-cycle is reserved for
  a fully-wedged modem.
- §7 M3 description: `HostModeEntry` bullet flipped — with `RESTART`, never
  `RESET`.

Code consequence: `HostModeEntry.java` gains a `RESTART` step between
`8BITCONV ON` and the resilient HOST-ON frame — `TX RST` send,
`RESTART_SETTLE_MS = 2000` RX drain, substring check for `cmd:` in the
accumulated bytes. Timing per user-specified 2000 ms delay.

### 2026-04-20 — M3a accepted, M3b design locked (Q1–Q10)

Hardware-smoke gate for **M3a** passed on all four §8 cases (COMMAND /
AUTOBAUD / HOSTMODE / OFF) plus the Cancel-on-OFF-prompt side case.
`StartupDetector` is accepted as-is; no code changes required. The seven
Step-B questions for M3b were resolved, plus three follow-ups after the
user pasted the `hCmd.md` §4.1.3 entry-sequence snippet:

- **Q1**: Verbose commands (`AWLEN 8`, `PARITY 0`, `8BITCONV ON`) terminated
  with `CR` (0x0D) only — no `LF`.
- **Q2**: Per-command ack = **500 ms** substring-match on `cmd:` with the
  RX accumulator reset between steps, same discipline as the detector.
  Fire-and-forget for the HOST-ON transition itself (no `cmd:` follows).
- **Q3**: Post-HOST-ON state = **silent**; the OGG probe is the first
  host-frame exchange on the wire (confirmed by `hCmd.md` §4.1.3).
- **Q4**: OGG probe timeout = **750 ms** (matches detector per-step).
- **Q5**: Probe retry = **one** attempt with double-SOH `01 01 4F 47 47 17`
  (reconciles with spec §4.1.3 step 2 loop). Fail on second miss.
- **Q6**: Shutdown `HO N` form = **double-SOH** `01 01 4F 48 4F 4E 17`.
- **Q7**: Entry-failure UX = `JOptionPane` with **Retry** / **Cancel**;
  Retry re-runs `detect()` + `enter()`; Cancel = `runShutdown` + exit 1.
- **Q8**: HOST-ON invocation = **resilient** frame `11 18 03 48 4F 53 54
  20 59 0D` (`XON CAN ^C HOST Y<CR>`) per `hCmd.md` §4.1.3, not plain
  `HOST ON<CR>`.
- **Q9**: Shutdown ack handling = **wait up to 500 ms** for the `HO N`
  ack block; on write-error OR ack-timeout, fall back to
  `SerialLink.sendBreak(300)`; total wall-clock capped at **1500 ms**.
- **Q10**: Retry scope = **close and reopen** the `SerialLink` on every
  Retry, protecting against stuck DTR / stale RX buffer state.

**Documentation deltas**
- §3 In Scope: listed the resilient HOST-ON frame byte sequence instead
  of plain `HOST ON<CR>`.
- §5.5: rewrote the entry-sequence block with the verbose-ack discipline,
  the resilient HOST-ON frame, OGG probe + retry semantics, and the
  save/restore around `readTimeoutMs`. Added an explicit `HostModeEntry.tryExit`
  contract.
- §5.9: added a reserved-mnemonic-token table spanning M3a + M3b
  (`TX AWL`, `TX PAR`, `TX 8BC`, `TX HYR`, `RX CMD`, `TX OGG`, `TX OGR`,
  `RX OGG`, `TX HOX`, `RX HOA`, plus free-form `logRaw` for BREAK).
  `TX HON` is retained as a legacy mnemonic for M1-era logs.
- §5.11: added the host-mode-entry-failure row + the 500 ms ack /
  300 ms BREAK / 1500 ms total shutdown timing contract.
- §4.E3 still governs exit semantics; the only change is that the wire
  form is now explicitly double-SOH.

No protocol decision in §4 changed — M3b is a concretization of §5.5 and
§5.11, not a redefinition.

### 2026-04-20 — Incorporated answers to the 4 open questions

Resolved the remaining build / dependency / launcher / font questions that were outstanding at the close of the planning session:

- **H1 (new)**: jSerialComm version locked to **`jSerialComm-2.11.4.jar`**. Jar is currently staged at the project root and will be relocated to `lib/` in Step 6.1.
- **H2 (new)**: Test harness locked to the **JUnit 5 console launcher** (`junit-platform-console-standalone-<ver>.jar`), vendored into `lib/`. Test classpath only; never merged into the release fat jar.
- **H3 (new)**: `run.bat` and the release shortcut use **`javaw`** (no console window). Diagnostics flow through `PacketLogger` per **F1–F3**.
- **H4 (new)**: `HexDumpView` default font chain `Consolas → Cascadia Mono → Menlo → Monospaced`, 12 pt, resolved at construction time via `GraphicsEnvironment`. Not persisted in `AppSettings` for v1.
- **§5.10** updated to reference the concrete jar names, `javaw`, and `test.bat`.
- **§6 Open Questions** cleared of all residual items.
- **§7 M1** checklist updated to include vendoring the JUnit launcher, relocating the jSerialComm jar, and adding `test.bat` to the scaffolded layout.

### 2026-04-20 — Resolution pass on Additional Questions

- **A1 / E5**: Reframed to point at `PK232_HostMode_Reference.md` (Chapter 4 framing + `OGG` probe) and `hCmd.md` (`AE` / `MM` / `IO` semantics) as the authoritative protocol sources. No more physical-PDF dependency.
- **A6 (new)**: Documented expected `AE` ack envelope (`01 4F 41 45 00 17`) derived from the `0x4F` global-command response pattern.
- **C3**: Locked snarky popup copy to `OK ive done nothing are you happy?`.
- **C7 (new)**: Dump button always re-issues `AE` on every press — never trust the modem's last ADDRESS increment.
- **C8 (new)**: ADDRESS input is exactly 4 hex chars, case-insensitive, no `$` / `0x` prefix; normalized to uppercase.
- **C9 (new)**: `MainFrame` opens at 2/3 screen size and is always open; closing it exits the program (cleanly via `HO N`).
- **C10 (new)**: Treat ROM and RAM as one flat 64 KB address space; no region-based warnings or refusals.
- **D5 (new)**: Detection timeout is user-configurable via the COM port / Settings window; default 8000 ms.
- **F1 / F2 / F3**: Log location locked to `./Logs/` next to the jar; format locked to `<TX|RX> <CMD>  <HEX>  <ASCII>`; rotation rules (10 KB × 3) retained.
- **Status bar**: No additional states beyond "HOSTMODE ready" for v1.
- **§3 Scope**: Explicitly added `IO` and `GG` as out-of-scope.
- **§5.6**: Dump pipeline spec now references the concrete framed envelopes for `AE` and `MM` responses.
- **§6**: Open Questions section cleared; residual items folded into the Risks table where relevant (decimal-vs-`$hex` parser risk added).
- **§7**: Pre-code actions updated — all protocol test vectors can be locked from documented references without further manual lookup.

---

*Document owner: Project architect. Update this file whenever a decision in §4 changes, before touching code.*
