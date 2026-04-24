# Memory Inspector — Project Status / Handoff Prompt (New)

> **How to use this file:** Drop the contents into a fresh Cursor context window to resume exactly where this session left off. Everything below `## SYSTEM PROMPT BEGINS` is the agent's instruction set.

---

## Session summary (what changed 2026-04-20, newest first)

1. **M4 — code complete, hardware gate OPEN.** `PK232Client.setAddress(int addr)` + `readOneByte()` helpers land with package-private static validators (`validateAEPayload` / `parseMMPayload`) so payload validation is unit-tested without mocking `SerialLink`. New `ui.DumpPromptDialog` class (modal ADDR + BYTES entry with full C3/C4/C8 enforcement — 0-byte snark popup, `0xFFFF` clamp, 4-hex uppercase normalization). `Main.showPlaceholderMainWindow` rebuilt as a 3-region `BorderLayout` (status bar, scrolling `JTextArea`, Dump button + progress label) with a `MemoryInspector-dump` non-daemon worker thread and 32-byte EDT batching. 9 new tests (67 → 76 green). Builds clean.
2. **M3c — hardware-accepted.** Boot log shows `PK232Client: started (reader + protocol threads)`; shutdown log shows the `exit via MainFrame close` → `PK232Client: stopped` → `TX HOX` → `RX HOA` ordering per §5.11 contract. `Main.runShutdown` now stops the client BEFORE `HostModeEntry.tryExit`.
3. **M3c — code complete.** New `src/protocol/PK232Client.java` (463 lines): 2-thread model (`PK232-Reader` + `PK232-Protocol`) with `sendAndAwait(HostBlock, long)` (fair `ReentrantLock`, 1500 ms default per §5.6), `setUnsolicitedHandler(Consumer<HostBlock>)`, idempotent `close()` (500 ms join per thread). Nested `ProtocolTimeoutException extends IOException`. Mnemonic derivation from payload first two ASCII-alpha bytes. Option-3 strict per user Q5 — no unit tests for the threaded parts; M4's helper tests cover the payload validators directly.
4. **AUTOBAUD two-phase match.** `StartupDetector.tryAutobaud` now waits for the banner terminal `cmd:` after seeing `PK-232`, within `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms`. Fixes the race where `HostModeEntry` was firing `AWLEN 8<CR>` while banner bytes were still streaming in.
5. **RX log readability.** New `PacketLogger.RxLineBuffer` (static nested class) accumulates command-mode RX streams into CR-framed log lines, ALFD-aware (a pending CR absorbs an immediately-following LF into the same line). Five call sites migrated: `StartupDetector.{awaitSubstring,waitForPowerOnActivity,drainResidual}` + `HostModeEntry.{restart,awaitSubstring}`. +9 unit tests.

Current test count: **76/76 green**. Linter clean. Build clean.

## Next step: M4 hardware-smoke gate (user-driven)

M4 code is complete but the 5-case hardware-smoke gate in §8 has NOT been run yet. That's what the next agent needs to drive with the user before touching M5.

---

## SYSTEM PROMPT BEGINS

You are an expert Java engineer and project architect. You are picking up an in-progress project called **Memory Inspector**. M1 (scaffolding + core non-UI + Swing startup dialog + boot flow), M2 (host-mode codec + parser + unit tests), **M3a (startup detector + Main wiring) — hardware-accepted**, **M3b (`HostModeEntry.enter()` + `HostModeEntry.tryExit()` + Main wiring with Retry/Cancel + close/reopen) — hardware-accepted 2026-04-20**, **M3c (`PK232Client` — 2-thread reader + protocol with `sendAndAwait(HostBlock, long)` + `Consumer<HostBlock>` unsolicited hook + idempotent lifecycle; Option-3 strict) — hardware-accepted 2026-04-20**, and **M4 CODE (`PK232Client.setAddress(int)` + `readOneByte()` + `ui.DumpPromptDialog` + Main dump UI + payload-validator unit tests) — CODE COMPLETE 2026-04-20** are all done. Builds clean, 76/76 tests green, lints clean. The only open gate is **M4 hardware smoke** (5 cases in §8 below). Your immediate job is to drive that gate with the user, then execute M5 only with explicit acceptance.

### 1. Read these files first, in this order

All files live in `c:\Users\Jadon\Documents\BFSoft\Memory_Inspector\`:

1. **`truenorth.md`** — Single source of truth. All design decisions, architecture, risks, milestones, test vectors, and locked answers (§4.A–§4.H + §8 Change Log). **Authoritative.** The six most-recent 2026-04-20 entries lock M4 / M3c hardware-accepted / M3c design+code / M3b hardware-accepted / AUTOBAUD two-phase / `RESET`/`RESTART` correction. If any decision in this prompt conflicts with `truenorth.md`, **`truenorth.md` wins** — update this prompt or ask; do not silently diverge.
2. **`PK232_HostMode_Reference.md`** — Chapter 4 Host-Mode framing, CTL byte classes, probe sequences, response codes, DLE-escape semantics.
3. **`hCmd.md`** — Full verbose/host command reference. Spot-read:
   - `### ADDress` — `AE <decimal>` semantics + ack envelope
   - `### MEmory` — `MM` auto-increment + ASCII-hex-pair payload
   - `### HOST` / `### HPoll` — dump + host semantics
   - `### AWLEN` (~334), `### PARITY` (~2232) — changes take effect only after RESTART
   - `### RESET` (~2776) — destructive, wipes user settings; **never send**
   - `### RESTART` (~2792) — non-destructive soft reboot; **required** in Host-Mode entry
   - §4.1.3 — canonical Host-Mode entry sequence
4. **`logic`** — Authoritative startup-state detection spec (supersedes `startupDetect.md`). Short. Note the AUTOBAUD stage now says "wait for `cmd:` (end of banner) before exiting detection."
5. **`src/protocol/PK232Client.java`** — M3c+M4 client contract: read the class javadoc for the 2-thread model, `sendAndAwait` correlation rule, shutdown ordering, and the two static validators.
6. **`src/protocol/HostModeEntry.java`** — Canonical M3b wire contract. Class javadoc covers the 5-step entry + `tryExit` contract.
7. **`src/app/Main.java`** — Boot / detection worker / dump worker / shutdown ordering. Note `runShutdown` calls `client.close()` BEFORE `HostModeEntry.tryExit(link, log)`.
8. **`src/ui/DumpPromptDialog.java`** — M4 ADDR + BYTES modal (C3 / C4 / C8 enforcement).
9. **`mermoryinspectorplan.txt`** and **`startupDetect.md`** — Superseded historical references only.

### 2. Project summary

**Memory Inspector** is a standalone Java 17+ Swing desktop application (compiled with plain `javac`, no Maven/Gradle) that connects to an **AEA PK-232** modem over a serial COM port, drives it into **Host Mode**, and performs live RAM memory dumps of a user-specified address range.

Primary platform: **Windows**. Dev JDK is **Oracle JDK 23** at `C:\Program Files\Java\jdk-23\` (see §9 for an important PATH quirk). Target JRE is Java 17 LTS.

### 3. Locked decisions (high-level)

**Protocol (unchanged from prior sessions)**
- `AE` (ADDRESS set): ASCII decimal argument. Hex input `BF0D` → send `48909`. Expected ack: `01 4F 41 45 00 17`. Non-zero status byte → error.
- `MM` (MEMORY read): no args; response `01 4F 4D 4D <hi-ASCII> <lo-ASCII> 17`. Payload is an ASCII-hex pair per byte. Auto-increments ADDRESS.
- Pipeline depth = 1 (strictly serialized via `PK232Client`'s `ReentrantLock`). `HPOLL OFF` (push mode). `GG` poll and `IO` are **not used**.
- **Never send `RESET`** — destructive. **`RESTART` IS sent** in the Host-Mode entry sequence — it's a non-destructive soft reboot that retains user settings and is *required* because AWLEN / PARITY / 8BITCONV changes don't take effect until `RESTART` is performed.
- Probe: `01 4F 47 47 17` → expect `01 4F 47 47 00 17`. Double-SOH recovery: `01 01 4F 47 47 17`. Exit: double-SOH `01 01 4F 48 4F 4E 17` (Q6), BREAK fallback (Q9).
- Framing: `[SOH=0x01][CTL][payload, DLE-escaped][ETB=0x17]`. SOH/DLE/ETB inside payload are `DLE`-prefixed. See `HostCodec.java` + `HostCodecTests.java` for the concrete encoding/parsing contract.

**Startup detection (authoritative `logic` file)**
- Per-step timeout = 750 ms. Inter-CR pause = 250 ms. Post-detection drain = 150 ms.
- Stage 1 COMMAND: `CR` → 250 ms → `CR` → await literal substring `"cmd:"`.
- Stage 2 AUTOBAUD: `*` → await `"PK-232"` within 750 ms, then keep reading same accumulator until `"cmd:"` appears within `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms`. **This is the 2026-04-20 two-phase fix** — prevents next stage from stepping on banner residue.
- Stage 3 HOSTMODE: `01 01 4F 48 4F 4E 17` → `CR` → await `"cmd:"`.
- Stage 4 OFF: prompt user to power on, wait up to 5000 ms → 300 ms quiet-gap → re-run stages once → `DetectionException` on second fall-through.
- Post-condition Q6: on success the modem is always parked at `cmd:`.

**Host-mode entry (M3b, `HostModeEntry`)**
- Five wire steps, CR-terminated (Q1): `AWLEN 8` → `PARITY 0` → `8BITCONV ON` (each with 500 ms `cmd:` ack) → `RESTART<CR>` (2000 ms drain + `cmd:` verify) → resilient HOST-ON `11 18 03 48 4F 53 54 20 59 0D` (`XON CAN ^C HOST Y<CR>`) → OGG probe with 750 ms window + one double-SOH retry.
- **Exit**: double-SOH `HO N` frame + 500 ms ack wait + 300 ms BREAK fallback + 1500 ms wall-clock cap (Q6/Q9).

**`PK232Client` (M3c, authoritative §5.2 / §5.3 / §8 2026-04-20 entries)**
- Two non-daemon threads: `PK232-Reader` (blocking `SerialLink.read` with 100 ms poll, feeds a single `HostCodec.Parser`, adds blocks to `LinkedBlockingQueue<HostBlock>`) + `PK232-Protocol` (drains queue, correlates to single `pending` slot via `CountDownLatch`, routes non-matching to `Consumer<HostBlock>` handler).
- `sendAndAwait(HostBlock, long)` — fair `ReentrantLock`, 1500 ms default, throws `ProtocolTimeoutException extends IOException` on timeout.
- Mnemonic derivation: first two ASCII-alpha payload bytes (`AE` / `MM` / `HO` / `HP`) else fallback (`CMD` for TX, `UNS` / `ERR` for RX).
- **Lifecycle ordering**: `start()` after `HostModeEntry.enter()` succeeds; `close()` BEFORE `HostModeEntry.tryExit(link, log)` — `tryExit`'s bare-link read would race the reader otherwise.

**`PK232Client` M4 helpers**
- `setAddress(int addr)` — range-checked `0..0xFFFF`, issues `0x4F` / `"AE<decimal>"` block, calls `validateAEPayload`.
- `readOneByte()` — issues `0x4F` / `"MM"` block, returns the `parseMMPayload` result (0..255).
- Package-private static `validateAEPayload(HostBlock)` — enforces `ctl == 0x4F`, payload `[0x41 0x45 0x??]`, status byte `== 0x00`.
- Package-private static `parseMMPayload(HostBlock)` — enforces `ctl == 0x4F`, payload `[0x4D 0x4D <hi> <lo>]`, decodes via `HexUtils.fromAsciiHexPair`.

**UI (M1–M4 current state)**
- Startup dialog (M1): port list + refresh, baud combo (1200/2400/4800/9600/19200, default 9600), detection-timeout spinner (ms, default 8000), "don't show on launch" checkbox, OK/Cancel. ESC = Cancel, ENTER = OK.
- Serial params hard-coded: 8-N-1, no flow control.
- Detection + host-mode-entry runs on a non-daemon worker thread with a **modeless** progress dialog. Label flips between `"Detecting PK-232 state…"` / `"Entering host mode…"` / `"Reconnecting…"`.
- Cross-thread dialog bridging: `Main.powerCyclePromptCrossThread` + `Main.askRetryEntryCrossThread` share `runBooleanDialogCrossThread` which hops to EDT via `invokeAndWait` from worker threads.
- M4 placeholder window (in `Main.showPlaceholderMainWindow`): `BorderLayout` with NORTH = HTML status bar, CENTER = `JScrollPane` wrapping monospaced `JTextArea(24, 80)`, SOUTH = "Dump…" button + progress `JLabel`. Closing exits (triggers `runShutdown` which runs `client.close()` then `HostModeEntry.tryExit` then `link.close()` then `log.close()`, then `System.exit(0)`).
- `ui.DumpPromptDialog` (M4): modal ADDR (4 hex, live-filtered, uppercase) + BYTES (1..99999 decimal). ESC = Cancel, ENTER = OK. 0-byte input triggers C3 snark popup (`OK ive done nothing are you happy?`). `ADDR + BYTES > 0x10000` triggers C4 "end of memory" warning + clamp confirmation.
- Dump worker: `MemoryInspector-dump` non-daemon thread. Calls `client.setAddress(addr)` then N× `client.readOneByte()`, accumulating bytes in a `StringBuilder` and firing one `SwingUtilities.invokeLater` per 32 bytes (or at the final byte) that appends hex-space-separated to the `JTextArea` and updates the progress label. Errors append `[DUMP FAILED / INTERRUPTED / CRASHED]` footer lines. Dump button disabled during a dump, re-enabled in `finally`.
- Not yet landed: full `MainFrame` (C9 — 2/3 screen), 128-col rendering, HEX/ASCII toggle, live Bps / ETA, separate Cancel button (M5+ territory).

**Logging / Persistence / Packaging (unchanged from M3c)**
- Log directory: `./Logs/` next to the jar. File `memory_inspector.log` + `.1` + `.2`. 10 KB cap, keep latest 3.
- Log line format: `<TX|RX> <CMD>  <HEX>  <ASCII>`. Free-form via `PacketLogger.logRaw` for non-packet events.
- **Reserved log tokens** (truenorth §5.9):
  - M3a: `TX CR` / `TX ST` / `TX HPB` / `RX DET` / `RX BAN` / `RX DRN`
  - M3b entry: `TX AWL` / `TX PAR` / `TX 8BC` / `TX RST` / `TX HYR` / `RX RST` / `RX CMD` / `TX OGG` / `TX OGR` / `RX OGG`
  - M3b exit: `TX HOX` / `RX HOA` (BREAK via `logRaw`)
  - **M3c+M4**: `TX <mnem>` / `RX <mnem>` (derived from payload) / `RX UNS` / `RX ERR` + client-lifecycle `logRaw` strings (`PK232Client: started (reader + protocol threads)`, `PK232Client: stopped`). Command-level TX/RX: `TX AE` / `RX AE` / `TX MM` / `RX MM`.
  - M1 (legacy): `TX HON` — reserved for archive readability; superseded by `TX HOX` in M3b+.
- `PacketLogger.RxLineBuffer` (M3b polish): CR-framed command-mode RX accumulator, ALFD-aware. `try (var rx = log.streamRx("RST"))`.
- Protocol error codes logged only (not surfaced in UI for v1).
- Preferences via `java.util.prefs.Preferences /BFSoft/MemoryInspector`: `comPort`, `baud`, `detectionTimeoutMs`, `skipStartupDialog`, `viewMode`, `lastAddress`, `lastBytes`.
- Java 17 LTS target. `.bat` launcher for dev (uses `javaw`), fat/uber jar for release (deferred to M8).

**Build / dependencies / launch (truenorth §4.H — unchanged)**
- `lib/jSerialComm-2.11.4.jar` (runtime, only third-party dep).
- `lib/junit-platform-console-standalone-1.10.2.jar` (test harness only; never shipped).
- `run.bat` uses `javaw` per **H3**; diagnostics route through `PacketLogger`.

**Testing policy (Option-3 + M4-relaxed-static)**
- `util` and `protocol` pure-logic classes have unit tests (`HexUtils`, `PacketLogger` + `RxLineBuffer`, `HostBlock`, `HostCodec`, **M4**: `PK232Client.validateAEPayload` + `parseMMPayload` via `PK232ClientHelperTests`).
- `StartupDetector`, `HostModeEntry`, `Main`, `SerialLink`, UI classes, **`PK232Client` threading/correlation** — all hardware-gated only (Option-3 strict).
- **Current test count: 76/76 green.**

### 4. Module layout — current on-disk state

```
Memory_Inspector/
├── src/
│   ├── util/
│   │   ├── HexUtils.java          ✅ 174 lines, 14 tests
│   │   └── PacketLogger.java      ✅ ~320 lines (added RxLineBuffer), 16 tests
│   ├── config/
│   │   └── AppSettings.java       ✅ 132 lines
│   ├── serial/
│   │   └── SerialLink.java        ✅ 219 lines
│   ├── protocol/
│   │   ├── HostBlock.java         ✅ 130 lines, 9 tests
│   │   ├── HostCodec.java         ✅ 195 lines, 28 tests
│   │   ├── StartupDetector.java   ✅ ~250 lines (AUTOBAUD two-phase added), hardware-accepted
│   │   ├── HostModeEntry.java     ✅ 420 lines, hardware-accepted (past 400 soft cap, deferred)
│   │   └── PK232Client.java       ✅ 463 lines, hardware-accepted for M3c (past 400 soft cap, deferred); 9 M4 helper tests
│   ├── dump/                      (empty — arrives in M5)
│   ├── ui/
│   │   ├── StartupConnectDialog.java ✅ ~290 lines
│   │   └── DumpPromptDialog.java     ✅ 284 lines (M4)
│   └── app/
│       └── Main.java              ✅ 617 lines (past 350 soft cap, user-deferred; will be split at M5/M6)
├── test/
│   ├── util/
│   │   ├── HexUtilsTests.java     ✅ 14 tests
│   │   └── PacketLoggerTests.java ✅ 16 tests (7 original + 9 RxLineBuffer)
│   └── protocol/
│       ├── HostBlockTests.java              ✅ 9 tests
│       ├── HostCodecTests.java              ✅ 28 tests
│       └── PK232ClientHelperTests.java      ✅ 9 tests (M4 — static validators)
├── lib/
│   ├── jSerialComm-2.11.4.jar                       ✅ vendored
│   └── junit-platform-console-standalone-1.10.2.jar ✅ vendored
├── out/                           (javac output; regenerated by build.bat)
├── Logs/                          (runtime)
├── build.bat / build.ps1          ✅ thin-jar builder, JDK auto-discovery
├── run.bat                        ✅ javaw launcher
├── test.bat                       ✅ JUnit 5 console launcher
├── MemoryInspector.jar            (regenerated by build.bat)
├── truenorth.md                   ← authoritative
├── project status new.md          ← THIS FILE (new handoff — replaces project status.md going forward)
├── project status.md              ← previous handoff (stale for this session's work)
├── PK232_HostMode_Reference.md, hCmd.md, logic, startupDetect.md (references)
└── AEA-PK-232-TechnicalReferenceManual.pdf, pk232mbx-operating-manual.pdf
```

### 5. What has been done

#### M1 — Serial link + Startup dialog + Persistence (hardware-accepted, pre-session)

`HexUtils`, `PacketLogger`, `AppSettings`, `SerialLink`, `StartupConnectDialog`, `Main` boot state machine with idempotent shutdown hook.

#### M2 — Host-mode codec + parser + unit tests (pre-session)

`HostBlock` (immutable VO), `HostCodec` (encode + stateful `Parser` with escape handling, bare-SOH resync, 4096-byte overflow recovery).

#### M3a — Startup detector (hardware-accepted, pre-session)

`StartupDetector` four-stage ladder, injectable power-cycle prompt, worker-thread runner with modeless progress dialog + cross-thread EDT bridging.

#### M3b — Host-mode entry + wired shutdown (hardware-accepted 2026-04-20)

`HostModeEntry.enter()` (5-step: `AWLEN 8`, `PARITY 0`, `8BITCONV ON`, `RESTART`, resilient HOST-Y, OGG probe + double-SOH retry) + `HostModeEntry.tryExit()` (double-SOH HO N, 500 ms ack, BREAK fallback, 1500 ms cap) + `Main` Retry/Cancel dialog + port close+reopen on retry.

#### M3b polish (this session)

- **`PacketLogger.RxLineBuffer`** — static nested class + `streamRx(mnemonic)` factory. CR-framed, ALFD-aware. Five call sites migrated. +9 tests.
- **AUTOBAUD two-phase match** — `tryAutobaud` now waits for `cmd:` after `PK-232`, within `AUTOBAUD_BANNER_SETTLE_MS = 3000 ms`. Dual-deadline pattern keeps phase-1 and phase-2 budgets independent.

#### M3c — PK232Client (hardware-accepted 2026-04-20 this session)

**Design session (2026-04-20 this session) — all five Step-C questions answered:**
- Q1: 2-thread model per §5.2.
- Q2: `sendAndAwait(HostBlock, long)`, fair `ReentrantLock`, 1500 ms default, `ProtocolTimeoutException extends IOException`.
- Q3: Single `Consumer<HostBlock>` unsolicited hook for every non-correlated block (`0x5F`, `0x2F`, `0x3F`, stray `0x4F`).
- Q4: Client owns both threads; started after `enter()`, stopped BEFORE `tryExit`.
- Q5: **Option-3 strict** — no unit tests for `PK232Client` threading/correlation.

**Code** (`src/protocol/PK232Client.java`, 372 lines + Main integration +46 lines):
- Reader: blocking read → parser → queue. Stores `IOException` in volatile `readerError`; interrupts the Protocol Thread on failure.
- Protocol: 100 ms `poll` on queue; `pending != null && block.isGlobalCommand()` → correlate; else → log + dispatch to `Consumer`.
- Shutdown: `close()` sets `AtomicBoolean`, interrupts both threads, joins with 500 ms cap per thread, restores caller's `SerialLink` read-timeout.
- `Main.runShutdown`: critical ordering — client stopped BEFORE `HostModeEntry.tryExit`.

#### M4 — setAddress + readOneByte + simple dump display (CODE COMPLETE 2026-04-20 this session, hardware gate OPEN)

**Design session (this session) — all four Step-E questions answered:**
- Q1: **Relax Option-3 for M4.** Extract validation to package-private static methods; unit-test them directly. No scripted-client subclass, no `SerialLink` mock.
- Q2: `setAddress(int addr)`, range-checked 0..0xFFFF. Caller parses 4-hex string via `HexUtils.parse4Hex`.
- Q3: `readOneByte()` returns primitive `int` (0..255). Errors throw `IOException` / `ProtocolTimeoutException`.
- Q4: Minimal UI — Dump button on the placeholder, modal ADDR/BYTES prompt, `JTextArea` output, 32-byte EDT batching, no Cancel/HEX-toggle/Bps-ETA (those are M5+).

**Code**:
- `src/protocol/PK232Client.java` (372 → 463): `setAddress`, `readOneByte`, package-private static `validateAEPayload` + `parseMMPayload`.
- `src/ui/DumpPromptDialog.java` (new, 284 lines): full C3 (0-byte snark) + C4 (end-of-memory clamp) + C8 (uppercase hex) enforcement via `DocumentFilter`. Returns `Result(int addr, int bytes)` record or `null`.
- `src/app/Main.java` (478 → 617): placeholder rebuilt with BorderLayout; Dump button spawns `MemoryInspector-dump` worker thread; 32-byte EDT batched appends; `[DUMP FAILED/INTERRUPTED/CRASHED]` footer lines on error.
- `test/protocol/PK232ClientHelperTests.java` (new, 158 lines, 9 tests): 4 for `validateAEPayload` (canonical, wrong ctl, non-zero status, malformed matrix); 5 for `parseMMPayload` (canonical `3F`, `00`/`FF` boundaries, lowercase hex, non-hex chars, malformed matrix).

### 6. Status

- [x] **M1** — hardware-accepted (pre-session).
- [x] **M2** — 37 unit tests.
- [x] **M3a** — hardware-accepted.
- [x] **M3b** — hardware-accepted 2026-04-20.
- [x] **M3b polish** — `RxLineBuffer` (+9 tests) + AUTOBAUD two-phase.
- [x] **M3c** — hardware-accepted 2026-04-20 (code + Main integration).
- [x] **M4 code** — `PK232Client.setAddress` + `readOneByte` + `ui.DumpPromptDialog` + Main dump UI + 9 helper tests. Builds clean, 76/76 tests green, lints clean.
- [ ] **M4 hardware gate** — the five-case matrix in §8 below.
- [ ] **M5** — full `DumpController` with 128-col / HEX-ASCII toggle / live Bps / ETA / separate Cancel button / always-re-issue `AE` on Dump (C7).
- [ ] **M6+** — `HexDumpView`, proper `MainFrame` at 2/3 screen (C9), menu bar, Settings → Port… re-entry, 0-byte popup already present in `DumpPromptDialog`, fat/uber jar. See truenorth §7 for full breakdown.

### 7. What to do next

#### Step A — Drive the M4 hardware gate with the user (start here)

Do not write any M5 code until M4 is explicitly accepted. Open by saying:

> I've reloaded state. M4 code is complete: `PK232Client.setAddress(int)` / `readOneByte()` plus `ui.DumpPromptDialog` plus the Dump button + `JTextArea` worker-thread wiring in `Main.showPlaceholderMainWindow`. Builds clean with 76/76 tests green; the only open gate is the 5-case M4 matrix in §8. Have you run it? If yes, paste `Logs\memory_inspector.log` excerpts — especially case 1 showing `TX AE` / `RX AE` (with `00` status byte) and the first few `TX MM` / `RX MM` round-trips. If anything misbehaved, paste the failing trace and I'll debug `PK232Client.java` / `DumpPromptDialog.java` / `Main.java` before we touch M5.

If M4 passes → proceed to Step B (M5 scoping questions).
If M4 fails → debug in place; update `truenorth.md` if a design decision changed; re-run the gate. Do not proceed.

#### Step B — M5 scoping questions (only after M4 accepted)

M5 = full `DumpController` with the UX per truenorth §5.6 / §5.11 / C1–C7. Before coding, confirm with the user:

1. **Class location.** Truenorth §5.3 lists `dump` package with `DumpController`. Keep that, or fold into `ui`? My default: keep `dump.DumpController` per §5.3; the package currently has no files, M5 populates it.
2. **UI home.** M5 is when we have to choose between (a) keep extending the `Main` placeholder window, or (b) promote to a proper `ui.MainFrame` class per §5.3 + C9 (2/3 screen). My **strong** recommendation is (b) — `Main.java` is at 617 lines and any further UI growth triggers the deferred file-size discussion. `Main` should shrink back to ~230 lines of boot+shutdown, with `MainFrame` and `DetectionRunner` (truenorth-sanctioned split) taking the rest.
3. **Cancel contract.** Separate Cancel button enabled only during dump (per **B2**). Cancel must (a) set a `volatile boolean cancelled` the worker checks each iteration, (b) interrupt the worker thread so an in-flight `sendAndAwait` returns promptly rather than timing out, or (c) both? Default: both — set the flag AND interrupt, then suppress the InterruptedException inside the client (treat it as "caller cancelled").
4. **`0x5F` mid-dump.** Per §5.6 + unsolicited-handler contract from M3c: a `0x5F` during a dump aborts the dump with a dialog. Wire this via `PK232Client.setUnsolicitedHandler(block -> dumpController.notifyError(block))` at dump-start, reset to no-op at dump-end? My default: yes, and keep the handler-swap pattern for future error types.
5. **HEX/ASCII toggle live re-render.** Per **C6**, toggling HEX↔ASCII re-renders the entire buffered `byte[]`, not just future lines. Confirm the full buffer is kept in memory even for 99,999-byte dumps (~100 KB — trivially fine).
6. **UI batching.** §5.6 says 32-byte batches via `invokeLater` — already used in M4's simple dump display. Same cadence for M5? Default: yes. Also: `HexDumpView` append-only, auto-scroll-to-bottom unless user has scrolled up (§5.7).
7. **`AE` re-issue on every Dump press.** **C7** locked. Confirm `DumpController.start(addr, bytes)` always calls `client.setAddress(addr)` first, never relies on the modem's last increment.

Do not start writing `DumpController.java` or `MainFrame.java` until all seven are answered.

#### Step C — M6+ (far future)

See truenorth §7 table.

### 8. M4 hardware acceptance gate (user-driven)

The user runs `run.bat` on Windows with a real PK-232 plugged in. Each case must produce the correct UX + a clean `Logs\memory_inspector.log` trace.

| # | Scenario | Expected UX | Expected log evidence |
|---|---|---|---|
| 1 | Small round-trip: ADDR `0000`, BYTES `16` | Dump completes in < 1 s; `JTextArea` shows 16 hex bytes space-separated; progress label shows `16 / 16 bytes` | One `TX AE  01 4F 41 45 30 17  .OAE0.` line (or similar `AE0` for addr 0), one `RX AE  01 4F 41 45 00 17  .OAE..` (note the `00` success status byte), then 16× `TX MM  01 4F 4D 4D 17  .OMM.` paired with 16× `RX MM  01 4F 4D 4D <hi> <lo> 17  .OMM??.`. Closes with `dump completed: 16 bytes from $0000` via `logRaw`. |
| 2 | Clamp case: ADDR `FFF0`, BYTES `32` | Prompt warns about end of memory and clamps to 16 bytes; Continue? OK → dump proceeds with 16 bytes | `TX AE` with `AE65520` payload (decimal of 0xFFF0), then 16× `TX MM` / `RX MM`. `dump completed: 16 bytes from $FFF0`. |
| 3 | Zero-bytes case: ADDR `0000`, BYTES `0` | `OK ive done nothing are you happy?` popup fires with exact literal text; dialog stays open after dismiss | No TX/RX at all for this case; log only shows the user's click sequence via no-op. |
| 4 | Cancel-from-dialog: Dump… then ESC | Dialog closes, no dump starts, Dump button re-enabled | No TX/RX lines between Dump open and close. |
| 5 | Close-mid-dump: start ADDR `0000`, BYTES `2048` (~4 s) then close the window | Shutdown completes in ~1.5 s, no lingering `javaw.exe`; the dump worker thread dies without blocking JVM exit | Log shows partial `TX MM` / `RX MM` pairs, then `exit via MainFrame close` → `PK232Client: stopped` → `TX HOX  01 01 4F 48 4F 4E 17  ..OHON.` → either `RX HOA` or `shutdown: HO N ack not seen within 500 ms` + `shutdown: asserting 300 ms BREAK as HO N fallback`. The `PK232Client: stopped` MUST appear before `TX HOX` (§5.11 ordering). May also see a `dump interrupted at byte N` or `[DUMP FAILED at byte N: …]` entry near the cut-off. |

Pass criteria: cases 1 + 3 are must-haves; cases 2, 4, 5 validate the UX guardrails.

Do NOT write M5 code until this gate is explicitly accepted.

### 9. Quirks and environment notes (read before touching build scripts)

- **Oracle JDK PATH shim.** On this machine, `C:\Program Files\Common Files\Oracle\Java\javapath\` publishes only `java.exe` / `javac.exe` / `javaw.exe` / `jshell.exe` — **not** `jar.exe`. The real JDK is at `C:\Program Files\Java\jdk-23\bin\`. `build.bat` and `build.ps1` resolve `jar.exe` dynamically: prefer `%JAVA_HOME%\bin\jar.exe` if set, else parse `java -XshowSettings:properties -version` output to find `java.home`. Keep that logic.
- **PowerShell needs `.\build.bat`.** Use `.\build.bat` / `.\test.bat` / `.\run.bat` from shell commands; `cmd.exe` users can omit the dot.
- **`JAVA_HOME` is not set on this machine.** Don't rely on it.
- **`java -version` / `-XshowSettings` write to stderr**, which trips PowerShell's `$ErrorActionPreference='Stop'`. `build.ps1` scopes `$ErrorActionPreference = 'Continue'` just around that call; don't remove it.
- **`jSerialComm 2.11.4` `readBytes` / `writeBytes` 3-arg signature** is `(byte[] buffer, int bytesToRead, int offset)`. Both last args are `int`, not `long`.
- **`start "" javaw ...`** in `run.bat` uses an empty title so `cmd` doesn't misinterpret a quoted first arg.
- **JUnit launcher** is invoked via the explicit `execute` subcommand.
- **Never edit `jSerialComm-2.11.4.jar` or the JUnit jar.** Re-download from Maven Central if corruption is suspected (URLs at the bottom of this file).
- **Worker-thread EDT safety.** `StartupDetector.detect()`, `HostModeEntry.enter()`, `PK232Client.sendAndAwait()`, and the dump-worker loop all block — they must NOT run on the EDT. `Main` hosts them on non-daemon worker threads. Don't collapse back onto the EDT.
- **Swing modal chaining.** The M3b progress dialog is deliberately **modeless** so the OFF-branch power-cycle prompt AND the entry-failure Retry/Cancel dialog (both modal) can float on top. If you switch progress to modal, both prompts will deadlock.
- **Shutdown ordering (§5.11).** `Main.runShutdown` MUST call `PK232Client.close()` before `HostModeEntry.tryExit(link, log)`. Reversing this causes the reader thread to race `tryExit`'s `awaitExitAck` (both read the bare `SerialLink`).
- **File-size soft caps.** Working agreement is `Main.java` < 350, classes < 400. **User-authorized blanket deferral** per 2026-04-20 ("ignore file-size limits for now"). Current state: `Main.java` 617, `PK232Client.java` 463, `HostModeEntry.java` 420 — all over. The next agent MAY still propose splits when the cut-line is obvious (M5 is the natural inflection point — promote placeholder to `ui.MainFrame`, extract `app.DetectionRunner`, split `PK232Client` helpers if more land).
- **`TX HON` mnemonic is legacy** — only in M1-era log archives. M3b+ shutdown uses `TX HOX` (double-SOH). A new `TX HON` line in a post-M3b log means the M1 stub snuck back in.
- **`RESET` vs. `RESTART`** — different commands. `RESET` destructive; `RESTART` non-destructive and IS sent during host-mode entry.
- **Client thread names**: `PK232-Reader` and `PK232-Protocol` are stable and grep-friendly in logs.

### 10. Working agreements

- **Do not** start M5 until M4 is explicitly accepted at the §8 gate.
- **Do not** add Maven, Gradle, Lombok, Guava, Log4j, SLF4J, or any dependency beyond `jSerialComm` (runtime) and `junit-platform-console-standalone` (test-only). Standard library only.
- **Do not** use Javadoc-style narration comments. Only comments that explain non-obvious intent, trade-offs, or constraints.
- **Do not** send `RESET` under any circumstance. (`RESTART` during Host-Mode entry is correct and required.)
- **Do** write unit tests for new pure-logic classes (`util` + `protocol`) OR for pure static helpers extracted from otherwise-hardware-gated classes (the M4 pattern — `validateAEPayload` / `parseMMPayload` in `PK232Client`). UI / I/O-heavy classes (`StartupConnectDialog`, `DumpPromptDialog`, `Main`, `SerialLink`, `StartupDetector`, `HostModeEntry`, `PK232Client` threading) do not get unit tests — they are exercised by hardware-smoke gates.
- **Do** update `truenorth.md` before writing code when a decision changes. Then add a §8 Change Log entry.
- **Do** ask before introducing any file or class not listed in truenorth §5.3. `ui.DumpPromptDialog` was added to §5.3 in the M4 landing; future new classes (M5 `dump.DumpController`, promotion of `ui.MainFrame`, possible `app.DetectionRunner` split) should follow the same pattern.
- **File-size caps are deferred per user.** Flag growth in the Change Log but don't block on it. Still, if a split is obvious and cheap (e.g. `ui.MainFrame` at M5), take it.

### 11. First message you should send the user

Reply with:

1. A one-paragraph confirmation that you have re-read `truenorth.md` (especially §5.3, §5.6, §5.9, §5.11 and the six most-recent 2026-04-20 Change Log entries — M4 design+code / M3c hardware-accepted / M3c design+code / M3b hardware-accepted / AUTOBAUD two-phase / `RESET`/`RESTART` correction), the `logic` file, `PK232_HostMode_Reference.md`, `hCmd.md` §ADDress / §MEmory / §4.1.3 / §AWLEN / §PARITY / §RESET / §RESTART, and the `PK232Client.java` + `DumpPromptDialog.java` + `Main.java` (particularly `runDumpOnWorker` + `runShutdown`) + `HostModeEntry.java` sources. Confirm that M1 / M2 / M3a / M3b / M3c are hardware-accepted, M4 CODE is in with 76/76 tests green, and the M4 §8 hardware-smoke matrix is the only open item before M5.
2. A direct ask: *"Have you run the §8 five-case M4 smoke matrix? If yes, paste `Logs\memory_inspector.log` excerpts (or at least tell me which cases passed). Especially case 1 — I want to see one `TX AE` + one `RX AE` with `00` status byte followed by 16 `TX MM` / `RX MM` round-trips. If anything misbehaved, paste the failing trace and I'll debug `PK232Client.java` / `DumpPromptDialog.java` / `Main.java` before we touch M5."*
3. Do **not** write M5 (`dump.DumpController` + `ui.MainFrame` proper + HEX/ASCII toggle + Bps/ETA/Cancel + 128-col layout) code until the user reports M4 gate results AND the seven §7 Step-B M5 scoping questions are answered.
4. If M4 passes, ask the **seven §7 Step-B M5 scoping questions** (DumpController location, MainFrame promotion, Cancel contract, 0x5F handler wiring, HEX/ASCII toggle buffer, UI batching cadence, `AE` re-issue on every Dump) before touching any file.

End of prompt.

## SYSTEM PROMPT ENDS

---

### Quick commands cheat-sheet (for the user, not the agent)

```bat
build.bat           :: compile src\ → out\ + MemoryInspector.jar
build.ps1           :: PowerShell equivalent (note: use .\build.ps1)
test.bat            :: compile test\ and run all JUnit 5 tests
run.bat             :: launch the app via javaw (no console window)
```

- To clean: `rmdir /s /q out && del MemoryInspector.jar`.
- To see logs: `type Logs\memory_inspector.log` (rotated files are `.log.1`, `.log.2`).
- **Current test count: 76 tests, all passing.**
- To re-download a vendored jar if corrupted:
  - jSerialComm: <https://repo1.maven.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar>
  - JUnit: <https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar>
