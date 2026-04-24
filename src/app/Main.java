package app;

import config.AppSettings;
import dump.DumpController;
import dump.DumpController.Outcome;
import dump.DumpController.ViewMode;
import protocol.HostBlock;
import protocol.HostModeEntry;
import protocol.HostModeEntry.HostModeEntryException;
import protocol.PK232Client;
import protocol.StartupDetector;
import protocol.StartupDetector.ModemState;
import serial.SerialLink;
import ui.DumpPromptDialog;
import ui.StartupConnectDialog;
import util.HexUtils;
import util.PacketLogger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Application entry point. Boot state machine:
 * <pre>
 *   auto-connect?  -->  StartupConnectDialog loop (max 3 failures)
 *        |                       |
 *        +--------+--------------+
 *                 v
 *       detection + host-mode-entry worker thread
 *                 (modeless progress dialog; label switches between
 *                  "Detecting PK-232 state…" / "Entering host mode…" /
 *                  "Reconnecting…" as phases advance)
 *                 v
 *       placeholder MainFrame (closing it exits the app + sends HO N)
 * </pre>
 *
 * <p>The {@code MainFrame} proper arrives in M5/M6 per truenorth §5.3; for
 * M1–M3c this class shows a minimal {@link JFrame} as the hardware-gate UI.
 * On M3b+ host-mode entry success a {@link PK232Client} is started (2
 * threads: reader + protocol) and held in an {@link AtomicReference} so
 * {@link #runShutdown} can stop it BEFORE
 * {@link HostModeEntry#tryExit(SerialLink, PacketLogger)} runs. {@code tryExit}
 * then uses the bare {@link SerialLink} to send the double-SOH {@code HO N}
 * frame (and falls back to BREAK per §5.11).
 */
public final class Main {

    private static final int MAX_CONNECT_FAILURES = 3;

    /** Displayed by Help → About (§8 Change Log 2026-04-21 "M6 scope locked"). */
    static final String APP_VERSION = "Beta Release";

    /**
     * Gate for the Settings → Port… reconnect worker — prevents a second
     * click from racing a first mid-flight reconnect, which would otherwise
     * double-close the SerialLink / PK232Client.
     */
    private static final AtomicBoolean reconnectInProgress = new AtomicBoolean(false);

    private Main() {}

    public static void main(String[] args) {
        final PacketLogger                    log           = PacketLogger.defaultLogger();
        final AppSettings                     settings      = new AppSettings();
        final AtomicReference<SerialLink>     linkRef       = new AtomicReference<>();
        final AtomicReference<PK232Client>    clientRef     = new AtomicReference<>();
        final AtomicReference<DumpController> dumpCtlRef    = new AtomicReference<>();
        final AtomicBoolean                   shutdownDone  = new AtomicBoolean(false);

        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> runShutdown(linkRef, clientRef, dumpCtlRef, log, shutdownDone,
                        "exit via shutdown hook"),
                "MemoryInspector-shutdown"));

        applySystemLookAndFeel();

        try {
            SwingUtilities.invokeAndWait(() ->
                    boot(settings, log, linkRef, clientRef, dumpCtlRef, shutdownDone));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.logRaw("boot interrupted: " + ie.getMessage());
        } catch (InvocationTargetException ite) {
            log.logRaw("boot failed: " + ite.getCause());
        }
    }

    private static void boot(AppSettings settings,
                             PacketLogger log,
                             AtomicReference<SerialLink> linkRef,
                             AtomicReference<PK232Client> clientRef,
                             AtomicReference<DumpController> dumpCtlRef,
                             AtomicBoolean shutdownDone) {
        SerialLink link = tryAutoConnect(settings, log);
        if (link == null) {
            link = promptAndConnect(settings, log);
        }
        if (link == null) {
            log.logRaw("startup aborted by user");
            runShutdown(linkRef, clientRef, dumpCtlRef, log, shutdownDone, "exit before MainFrame");
            System.exit(0);
            return;
        }
        linkRef.set(link);
        startDetection(link, log, linkRef, clientRef, dumpCtlRef, shutdownDone);
    }

    private static SerialLink tryAutoConnect(AppSettings settings, PacketLogger log) {
        if (!settings.isSkipStartupDialog()) {
            return null;
        }
        String port = settings.getComPort();
        if (port.isEmpty()) {
            return null;
        }

        SerialLink link = new SerialLink(port, settings.getBaud());
        try {
            link.open();
            log.logRaw("auto-connected to " + port + " @ " + settings.getBaud());
            return link;
        } catch (IOException ioe) {
            log.logRaw("auto-connect failed on " + port + ": " + ioe.getMessage());
            return null;
        }
    }

    private static SerialLink promptAndConnect(AppSettings settings, PacketLogger log) {
        StartupConnectDialog dialog = new StartupConnectDialog(null, settings);
        int failures = 0;
        while (failures < MAX_CONNECT_FAILURES) {
            StartupConnectDialog.Result r = dialog.showDialog();
            if (r == null) {
                return null;
            }
            SerialLink link = new SerialLink(r.portName(), r.baud());
            try {
                link.open();
                log.logRaw("connected to " + r.portName() + " @ " + r.baud());
                return link;
            } catch (IOException ioe) {
                failures++;
                log.logRaw("open failed (" + failures + "/" + MAX_CONNECT_FAILURES
                        + ") on " + r.portName() + ": " + ioe.getMessage());
                JOptionPane.showMessageDialog(null,
                        "Failed to open " + r.portName() + " @ " + r.baud() + ":\n"
                                + ioe.getMessage(),
                        "Memory Inspector — Connect",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        log.logRaw("giving up after " + MAX_CONNECT_FAILURES + " failed connection attempts");
        return null;
    }

    /**
     * Kicks off {@link StartupDetector} + {@link HostModeEntry} on a
     * non-daemon worker thread with a modeless progress dialog parked in
     * front of the user. Detection failure is fatal; host-mode-entry
     * failure opens a Retry / Cancel dialog (per Q7/Q10) — Retry closes +
     * reopens the serial port and loops back to detection. Running
     * detection on the EDT would freeze the UI for up to ~10 s on the OFF
     * branch and would deadlock the power-cycle / retry prompts.
     */
    private static void startDetection(SerialLink link,
                                       PacketLogger log,
                                       AtomicReference<SerialLink> linkRef,
                                       AtomicReference<PK232Client> clientRef,
                                       AtomicReference<DumpController> dumpCtlRef,
                                       AtomicBoolean shutdownDone) {
        DetectProgress progress = buildProgressDialog();
        progress.dialog.setVisible(true);

        Thread worker = new Thread(
                () -> runDetectAndEntryLoop(link, log, linkRef, clientRef, dumpCtlRef,
                        shutdownDone, progress),
                "MemoryInspector-detection");
        worker.setDaemon(false);
        worker.start();
    }

    /**
     * The worker-thread body: repeatedly tries
     * {@link StartupDetector#detect()} then {@link HostModeEntry#enter()}
     * until one succeeds (→ placeholder window), detection fatally fails
     * (→ error dialog + exit 1), or the user cancels out of an
     * entry-failure retry (→ error dialog + exit 1). The port is closed +
     * reopened between retries so stuck DTR / stale RX state cannot
     * persist across attempts.
     */
    private static void runDetectAndEntryLoop(SerialLink link,
                                              PacketLogger log,
                                              AtomicReference<SerialLink> linkRef,
                                              AtomicReference<PK232Client> clientRef,
                                              AtomicReference<DumpController> dumpCtlRef,
                                              AtomicBoolean shutdownDone,
                                              DetectProgress progress) {
        while (true) {
            ModemState state;
            try {
                setProgressLabel(progress, "Detecting PK-232 state…");
                state = new StartupDetector(link, log, Main::powerCyclePromptCrossThread)
                        .detect();
            } catch (IOException | StartupDetector.DetectionException e) {
                log.logRaw("detection failed: " + e.getMessage());
                failFatal(progress, e.getMessage(), linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                return;
            } catch (RuntimeException re) {
                log.logRaw("detection crashed: " + re);
                failFatal(progress, "unexpected: " + re, linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                return;
            }

            try {
                setProgressLabel(progress, "Entering host mode…");
                new HostModeEntry(link, log, state).enter();
            } catch (HostModeEntryException | IOException e) {
                log.logRaw("host-mode entry failed: " + e.getMessage());
                if (askRetryEntryCrossThread(e.getMessage())) {
                    if (!reopenLink(link, log, progress)) {
                        failFatal(progress, "Port reopen failed; cannot retry detection.",
                                linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                        return;
                    }
                    continue;
                }
                failFatal(progress, "Host-mode entry failed:\n" + e.getMessage(),
                        linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                return;
            } catch (RuntimeException re) {
                log.logRaw("host-mode entry crashed: " + re);
                failFatal(progress, "unexpected (entry): " + re,
                        linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                return;
            }

            // Host mode is up; spin up the 2-thread framed client (§5.3).
            // Failure here is fatal — if we can't bring up the reader, the
            // modem is useless to the app. The idle unsolicited handler
            // logs to packet log; DumpController swaps it out for the
            // duration of a dump and restores it on finish (M5 Q4).
            PK232Client client = new PK232Client(link, log);
            final Consumer<HostBlock> idleUnsolicitedHandler = block ->
                    log.logRaw("unsolicited block from modem: " + block);
            client.setUnsolicitedHandler(idleUnsolicitedHandler);
            try {
                client.start();
            } catch (RuntimeException re) {
                log.logRaw("PK232Client start failed: " + re);
                failFatal(progress, "Failed to start PK232Client:\n" + re.getMessage(),
                        linkRef, clientRef, dumpCtlRef, log, shutdownDone);
                return;
            }
            clientRef.set(client);

            DumpController dumpController = new DumpController(client, log, idleUnsolicitedHandler);
            dumpCtlRef.set(dumpController);

            final ModemState finalState = state;
            SwingUtilities.invokeLater(() -> {
                progress.dialog.dispose();
                showPlaceholderMainWindow(link, log, finalState,
                        linkRef, clientRef, dumpCtlRef, shutdownDone);
            });
            return;
        }
    }

    /**
     * Close + reopen the port before a detection/entry retry. Returns
     * {@code true} on success. On failure the link is left closed and the
     * caller transitions to the fatal-error path.
     */
    private static boolean reopenLink(SerialLink link, PacketLogger log, DetectProgress progress) {
        setProgressLabel(progress, "Reconnecting…");
        try {
            link.close();
            link.open();
            log.logRaw("retry: reopened " + link.getPortName() + " @ " + link.getBaud());
            return true;
        } catch (IOException ioe) {
            log.logRaw("retry: reopen failed on " + link.getPortName() + ": " + ioe.getMessage());
            return false;
        }
    }

    private static void failFatal(DetectProgress progress, String detail,
                                  AtomicReference<SerialLink> linkRef,
                                  AtomicReference<PK232Client> clientRef,
                                  AtomicReference<DumpController> dumpCtlRef,
                                  PacketLogger log,
                                  AtomicBoolean shutdownDone) {
        SwingUtilities.invokeLater(() -> {
            progress.dialog.dispose();
            showFatalErrorAndExit(detail, linkRef, clientRef, dumpCtlRef, log, shutdownDone);
        });
    }

    /** Small struct bundling the progress dialog with its mutable label. */
    private static final class DetectProgress {
        final JDialog dialog;
        final JLabel label;
        DetectProgress(JDialog dialog, JLabel label) {
            this.dialog = dialog;
            this.label = label;
        }
    }

    private static DetectProgress buildProgressDialog() {
        JDialog d = new JDialog((java.awt.Frame) null, "Memory Inspector — Detecting", false);
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        JLabel label = new JLabel("Detecting PK-232 state…", SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        panel.add(label, BorderLayout.NORTH);
        panel.add(bar,   BorderLayout.CENTER);

        d.setContentPane(panel);
        d.pack();
        // Give enough width for the longest label we'll show.
        Dimension pref = d.getPreferredSize();
        d.setSize(Math.max(pref.width, 320), pref.height);
        d.setLocationRelativeTo(null);
        return new DetectProgress(d, label);
    }

    private static void setProgressLabel(DetectProgress progress, String text) {
        SwingUtilities.invokeLater(() -> progress.label.setText(text));
    }

    /**
     * Bridge for {@link StartupDetector}'s {@link java.util.function.BooleanSupplier}
     * prompt hook. Safe to call from any thread: if we are already on the
     * EDT we show the dialog directly; otherwise we hop via invokeAndWait.
     */
    private static boolean powerCyclePromptCrossThread() {
        return runBooleanDialogCrossThread(Main::showPowerCycleDialog);
    }

    /** Cross-thread wrapper for the host-mode-entry Retry/Cancel dialog. */
    private static boolean askRetryEntryCrossThread(String detail) {
        return runBooleanDialogCrossThread(() -> showRetryEntryDialog(detail));
    }

    private static boolean runBooleanDialogCrossThread(java.util.function.BooleanSupplier dlg) {
        if (SwingUtilities.isEventDispatchThread()) {
            return dlg.getAsBoolean();
        }
        final boolean[] result = {false};
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = dlg.getAsBoolean());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException ite) {
            return false;
        }
        return result[0];
    }

    private static boolean showPowerCycleDialog() {
        int choice = JOptionPane.showConfirmDialog(null,
                "PK-232 is not responding.\n\n"
                        + "Please power the PK-232 on (or power-cycle it) now,\n"
                        + "then click OK to retry detection.",
                "Memory Inspector — Power on PK-232",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private static boolean showRetryEntryDialog(String detail) {
        int choice = JOptionPane.showConfirmDialog(null,
                "Failed to enter Host Mode:\n\n" + detail
                        + "\n\nRetry detection + entry, or cancel and exit?",
                "Memory Inspector — Host-mode entry failed",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private static void showFatalErrorAndExit(String detail,
                                              AtomicReference<SerialLink> linkRef,
                                              AtomicReference<PK232Client> clientRef,
                                              AtomicReference<DumpController> dumpCtlRef,
                                              PacketLogger log,
                                              AtomicBoolean shutdownDone) {
        JOptionPane.showMessageDialog(null,
                "Startup failed:\n\n" + detail
                        + "\n\nThe application will exit.",
                "Memory Inspector — Startup failed",
                JOptionPane.ERROR_MESSAGE);
        runShutdown(linkRef, clientRef, dumpCtlRef, log, shutdownDone, "startup failure: " + detail);
        System.exit(1);
    }

    private static void showPlaceholderMainWindow(SerialLink link,
                                                  PacketLogger log,
                                                  ModemState state,
                                                  AtomicReference<SerialLink> linkRef,
                                                  AtomicReference<PK232Client> clientRef,
                                                  AtomicReference<DumpController> dumpCtlRef,
                                                  AtomicBoolean shutdownDone) {
        JFrame frame = new JFrame(
                "Memory Inspector for PK-232 — Beta Release — " + link.getPortName());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(twoThirdsScreen());
        frame.setLocationRelativeTo(null);

        JLabel status = new JLabel(buildStatusBarHtml(link, state), SwingConstants.LEFT);
        status.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JTextArea output = new JTextArea(24, 80);
        output.setEditable(false);
        output.setLineWrap(false);
        output.setFont(resolveMonospacedFont());
        JScrollPane scroll = new JScrollPane(output,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Per-window session: the mirror buffer lets HEX↔ASCII toggle
        // re-render from the same bytes (Q5 / C6 locked 2026-04-21).
        final DumpSession session = new DumpSession();

        JButton      dumpBtn    = new JButton("Dump…");
        JButton      cancelBtn  = new JButton("Cancel");
        cancelBtn.setEnabled(false);

        JRadioButton hexRadio   = new JRadioButton("HEX", true);
        JRadioButton asciiRadio = new JRadioButton("ASCII", false);
        ButtonGroup  viewGroup  = new ButtonGroup();
        viewGroup.add(hexRadio);
        viewGroup.add(asciiRadio);

        JLabel progress = new JLabel("0 / 0 bytes", SwingConstants.LEFT);
        JLabel bpsLabel = new JLabel("— B/s",        SwingConstants.LEFT);
        JLabel etaLabel = new JLabel("ETA --:--",    SwingConstants.LEFT);

        JPanel southLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        southLeft.add(dumpBtn);
        southLeft.add(cancelBtn);
        southLeft.add(new JLabel("  View:"));
        southLeft.add(hexRadio);
        southLeft.add(asciiRadio);

        JPanel southRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 6));
        southRight.add(progress);
        southRight.add(bpsLabel);
        southRight.add(etaLabel);

        JPanel south = new JPanel(new BorderLayout());
        south.add(southLeft,  BorderLayout.WEST);
        south.add(southRight, BorderLayout.EAST);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(status, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(south,  BorderLayout.SOUTH);

        hexRadio.addActionListener(e -> {
            session.viewMode = ViewMode.HEX;
            rerenderDumpView(output, session);
        });
        asciiRadio.addActionListener(e -> {
            session.viewMode = ViewMode.ASCII;
            rerenderDumpView(output, session);
        });

        dumpBtn.addActionListener(e -> {
            PK232Client client = clientRef.get();
            DumpController controller = dumpCtlRef.get();
            if (client == null || !client.isRunning() || controller == null) {
                JOptionPane.showMessageDialog(frame,
                        "PK232Client is not running; cannot dump.",
                        "Memory Inspector — Dump",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (controller.isRunning()) {
                // Shouldn't happen — Dump button is disabled during a dump —
                // but defensive guard per C2.
                return;
            }
            DumpPromptDialog.Result r = DumpPromptDialog.showDialog(frame, 0x0000, 16);
            if (r == null) {
                return;
            }
            startDumpViaController(controller, r.addr(), r.bytes(), session,
                    frame, output, dumpBtn, cancelBtn, progress, bpsLabel, etaLabel);
        });

        cancelBtn.addActionListener(e -> {
            DumpController controller = dumpCtlRef.get();
            if (controller != null && controller.isRunning()) {
                controller.cancel();
                cancelBtn.setEnabled(false);
                progress.setText(progress.getText() + " — cancelling…");
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                runShutdown(linkRef, clientRef, dumpCtlRef, log, shutdownDone,
                        "exit via MainFrame close");
                System.exit(0);
            }
        });

        frame.setJMenuBar(buildMenuBar(frame, log, session, status, output,
                linkRef, clientRef, dumpCtlRef, shutdownDone));

        frame.setVisible(true);
    }

    /**
     * Spawn a dump via {@link DumpController} and wire its callbacks to
     * the placeholder window. The session mirror is reset here so the
     * HEX↔ASCII toggle re-renders this dump (not a prior one). Dump /
     * Cancel button enablement is tracked on the EDT via the listener's
     * completion callback (§5.6 / Q3+Q4 locks 2026-04-21).
     */
    private static void startDumpViaController(DumpController controller,
                                               int addr, int bytes,
                                               DumpSession session,
                                               JFrame frame,
                                               JTextArea output,
                                               JButton dumpBtn,
                                               JButton cancelBtn,
                                               JLabel progressLabel,
                                               JLabel bpsLabel,
                                               JLabel etaLabel) {
        session.mirrorBuffer = new byte[bytes];
        session.mirrorLen    = 0;
        session.startAddr    = addr;
        session.totalBytes   = bytes;

        output.setText("");
        output.append(String.format("== Dump $%04X..$%04X (%d bytes) ==%n",
                addr, (addr + bytes - 1) & 0xFFFF, bytes));
        progressLabel.setText("0 / " + bytes + " bytes");
        bpsLabel.setText("— B/s");
        etaLabel.setText("ETA --:--");

        dumpBtn.setEnabled(false);
        cancelBtn.setEnabled(true);

        controller.start(addr, bytes, new DumpController.Listener() {
            @Override
            public void onBatchReady(int bytesSoFar, int totalBytes,
                                     double bytesPerSecond, long etaMillis,
                                     byte[] newChunk) {
                System.arraycopy(newChunk, 0,
                        session.mirrorBuffer, session.mirrorLen, newChunk.length);
                session.mirrorLen += newChunk.length;

                rerenderDumpView(output, session);
                progressLabel.setText(bytesSoFar + " / " + totalBytes + " bytes");
                bpsLabel.setText(formatBps(bytesPerSecond));
                etaLabel.setText("ETA " + formatEta(etaMillis));
            }

            @Override
            public void onCompleted(byte[] fullBuffer, int startAddr,
                                    Outcome outcome, String failureMessage) {
                // Prefer the controller's buffer if it differs in length
                // (cancelled/aborted dumps: fullBuffer is trimmed).
                session.mirrorBuffer = fullBuffer;
                session.mirrorLen    = fullBuffer.length;
                session.startAddr    = startAddr;

                String footer = footerFor(outcome, fullBuffer.length, startAddr, failureMessage);
                mutateOutputPreservingScroll(output, () -> {
                    // Can't call rerenderDumpView here (it would capture
                    // its own pre-mutation scroll state inside a nested
                    // mutate call); inline the render so the outer
                    // wrapper captures ONCE, covers both the render and
                    // the footer append.
                    String banner = String.format(
                            "== Dump $%04X..$%04X (%d bytes) ==%n",
                            session.startAddr,
                            (session.startAddr + session.totalBytes - 1) & 0xFFFF,
                            session.totalBytes);
                    String body = DumpController.renderBuffer(
                            session.mirrorBuffer, session.mirrorLen,
                            session.startAddr, session.viewMode);
                    output.setText(banner + body + "\n" + footer + "\n");
                });

                progressLabel.setText(fullBuffer.length + " / "
                        + session.totalBytes + " bytes");
                etaLabel.setText("ETA --:--");

                dumpBtn.setEnabled(true);
                cancelBtn.setEnabled(false);

                if (outcome == Outcome.ABORTED || outcome == Outcome.FAILED) {
                    JOptionPane.showMessageDialog(frame,
                            footer,
                            "Memory Inspector — Dump",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static String footerFor(Outcome outcome, int bytesRead,
                                    int startAddr, String failureMessage) {
        String addr = HexUtils.toHex4(startAddr);
        switch (outcome) {
            case COMPLETED:
                return "[DUMP COMPLETED: " + bytesRead + " bytes from $" + addr + "]";
            case CANCELLED:
                return "[DUMP CANCELLED at byte " + bytesRead + "]";
            case ABORTED:
                return "[DUMP ABORTED at byte " + bytesRead + ": "
                        + (failureMessage == null ? "unknown" : failureMessage) + "]";
            case FAILED:
            default:
                return "[DUMP FAILED at byte " + bytesRead + ": "
                        + (failureMessage == null ? "unknown" : failureMessage) + "]";
        }
    }

    /**
     * Re-render the mirror buffer into the output pane using the session's
     * current view mode. No-op when the mirror is empty. Always uses
     * {@code setText} (not {@code append}) because the HEX↔ASCII toggle
     * needs a full re-render. Scroll behavior is auto-scroll-unless-
     * scrolled-up per the 2026-04-21 polish Change Log entry: if the
     * user was viewing the tail, the new tail scrolls into view; if
     * they were reading earlier content, their position is preserved.
     */
    private static void rerenderDumpView(JTextArea output, DumpSession session) {
        if (session.mirrorBuffer == null || session.mirrorLen <= 0) {
            return;
        }
        mutateOutputPreservingScroll(output, () -> {
            // Preserve any header line (the "== Dump ... ==" banner). We
            // prepend that banner to every render so scroll-back stays sane.
            String banner = String.format("== Dump $%04X..$%04X (%d bytes) ==%n",
                    session.startAddr,
                    (session.startAddr + session.totalBytes - 1) & 0xFFFF,
                    session.totalBytes);
            String body = DumpController.renderBuffer(
                    session.mirrorBuffer, session.mirrorLen,
                    session.startAddr, session.viewMode);
            output.setText(banner + body);
        });
    }

    /**
     * Run a mutation on the output pane with auto-scroll-unless-scrolled-up
     * semantics: snapshot whether the user was viewing the tail BEFORE
     * the mutation, run the mutation, then either sticky-scroll to the
     * new tail (if they were at-bottom) or preserve their prior scroll
     * position (if they were reading earlier content). The final
     * scrollbar write is deferred via {@link SwingUtilities#invokeLater}
     * so it runs after the text-area layout has settled — otherwise
     * {@link JScrollBar#getMaximum()} still reflects the pre-mutation
     * extent and sticky-to-bottom ends up on the old bottom.
     */
    private static void mutateOutputPreservingScroll(JTextArea output, Runnable mutation) {
        JScrollPane sp = resolveScrollPane(output);
        boolean wasAtBottom = isAtOrNearBottom(sp);
        int savedValue = sp == null
                ? 0
                : sp.getVerticalScrollBar().getValue();

        mutation.run();

        if (sp == null) {
            return;
        }
        JScrollBar vbar = sp.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> {
            if (wasAtBottom) {
                vbar.setValue(vbar.getMaximum());
            } else {
                vbar.setValue(Math.min(savedValue, vbar.getMaximum()));
            }
        });
    }

    /**
     * "At or near the bottom" is a fuzzy test — exact-pixel equality
     * with {@code value + extent == maximum} is too strict because the
     * scrollbar rarely lands on the exact max after layout. Tolerance
     * is max(40 px, 2 × unitIncrement) — comfortably larger than one
     * line height but still small enough that "scrolled up a couple of
     * lines" is honored as "user is reading history, don't yank".
     */
    private static boolean isAtOrNearBottom(JScrollPane sp) {
        if (sp == null) {
            return true;
        }
        JScrollBar vbar = sp.getVerticalScrollBar();
        if (!vbar.isVisible()) {
            return true; // content fits; there's no "up" to scroll to
        }
        int value     = vbar.getValue();
        int extent    = vbar.getModel().getExtent();
        int max       = vbar.getMaximum();
        int tolerance = Math.max(40, vbar.getUnitIncrement() * 2);
        return (value + extent) >= (max - tolerance);
    }

    /** Walk up the component hierarchy from {@code output} to its {@link JScrollPane}. */
    private static JScrollPane resolveScrollPane(JTextArea output) {
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, output);
    }

    /** Format throughput as "123.4 B/s" or "—" when not yet meaningful. */
    private static String formatBps(double bps) {
        if (bps <= 0 || Double.isNaN(bps) || Double.isInfinite(bps)) {
            return "— B/s";
        }
        return String.format("%.1f B/s", bps);
    }

    /** Format ETA as "mm:ss" or "--:--" if not yet known (etaMs < 0). */
    private static String formatEta(long etaMs) {
        if (etaMs < 0) {
            return "--:--";
        }
        long totalSeconds = Math.max(0, etaMs / 1000);
        long mm = totalSeconds / 60;
        long ss = totalSeconds % 60;
        if (mm > 99) {
            return "99:59+";
        }
        return String.format("%02d:%02d", mm, ss);
    }

    /**
     * Per-window dump-result mirror. Reset on each {@code startDumpViaController}
     * call. Lives on the EDT — no synchronization needed since every
     * listener callback from {@link DumpController} is delivered via
     * {@link SwingUtilities#invokeLater} and the two radio listeners run
     * on the EDT.
     */
    private static final class DumpSession {
        byte[]   mirrorBuffer;
        int      mirrorLen;
        int      startAddr;
        int      totalBytes;
        ViewMode viewMode = ViewMode.HEX;
    }

    /**
     * Font-fallback chain per truenorth H4:
     * {@code Consolas → Cascadia Mono → Menlo → Monospaced} @ 12 pt,
     * resolved at call-site time. Lives in {@code Main} for M4 so the
     * placeholder's {@code JTextArea} matches what {@code HexDumpView}
     * will pick in M6.
     */
    private static Font resolveMonospacedFont() {
        String[] candidates = { "Consolas", "Cascadia Mono", "Menlo", Font.MONOSPACED };
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> installed = new java.util.HashSet<>(
                java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (installed.contains(name) || Font.MONOSPACED.equals(name)) {
                return new Font(name, Font.PLAIN, 12);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    /**
     * Build the HTML for the status bar. Shared between initial window
     * open and {@code Settings → Port…} reconnect so both paths produce
     * byte-identical HTML (truenorth §8 Change Log 2026-04-21 "M6 scope
     * locked").
     */
    private static String buildStatusBarHtml(SerialLink link, ModemState state) {
        return "<html>"
                + "Connected: <b>" + link.getPortName() + " @ " + link.getBaud() + ", 8-N-1</b>"
                + " &nbsp;·&nbsp; "
                + "State: <b>" + state + "</b>"
                + " &nbsp;·&nbsp; "
                + "Host mode: <b>HOSTMODE ready</b>"
                + " &nbsp;·&nbsp; "
                + "PK232Client: <b>running (2 threads)</b>"
                + "</html>";
    }

    /**
     * Build the application menu bar per M6 scope (File / Settings / Help).
     * HEX/ASCII toggle stays on the bottom bar per user preference — not
     * mirrored into a View menu. All action listeners are closures over
     * the {@link AtomicReference} slots so they pick up the current
     * client / dump controller even after a {@code Settings → Port…}
     * swap.
     */
    private static JMenuBar buildMenuBar(JFrame frame,
                                         PacketLogger log,
                                         DumpSession session,
                                         JLabel statusBar,
                                         JTextArea output,
                                         AtomicReference<SerialLink> linkRef,
                                         AtomicReference<PK232Client> clientRef,
                                         AtomicReference<DumpController> dumpCtlRef,
                                         AtomicBoolean shutdownDone) {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem saveDumpItem = new JMenuItem("Save Dump…");
        saveDumpItem.setMnemonic('S');
        saveDumpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask));
        saveDumpItem.addActionListener(e -> doSaveDumpClick(frame, session, linkRef.get(), log));
        fileMenu.add(saveDumpItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('x');
        exitItem.addActionListener(e ->
                doExitClick(linkRef, clientRef, dumpCtlRef, log, shutdownDone));
        fileMenu.add(exitItem);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic('S');

        JMenuItem portItem = new JMenuItem("Port…");
        portItem.setMnemonic('P');
        portItem.addActionListener(e ->
                doSettingsPortClick(frame, log, statusBar, output,
                        linkRef, clientRef, dumpCtlRef));
        settingsMenu.add(portItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setMnemonic('A');
        aboutItem.addActionListener(e -> showAboutDialog(frame));
        helpMenu.add(aboutItem);

        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu);
        bar.add(settingsMenu);
        bar.add(helpMenu);
        return bar;
    }

    /**
     * File → Save Dump… handler. Writes the current session mirror to a
     * user-chosen file using whichever view mode is currently selected
     * (truenorth §8 Change Log 2026-04-21 "M6 scope locked" Q answer).
     * No-op with an info popup when the mirror is empty.
     */
    private static void doSaveDumpClick(JFrame frame, DumpSession session,
                                        SerialLink link, PacketLogger log) {
        if (session.mirrorBuffer == null || session.mirrorLen == 0) {
            JOptionPane.showMessageDialog(frame,
                    "No dump data available. Run a dump first.",
                    "Memory Inspector — Save Dump",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AppSettings settings = new AppSettings();
        File startDir = resolveSaveStartDir(settings);

        JFileChooser chooser = new JFileChooser(startDir);
        String defaultName = String.format("dump_%s_%dbytes_%s.txt",
                HexUtils.toHex4(session.startAddr),
                session.mirrorLen,
                session.viewMode.name().toLowerCase());
        chooser.setSelectedFile(new File(startDir, defaultName));
        chooser.setDialogTitle("Save Dump");

        int choice = chooser.showSaveDialog(frame);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();

        try (FileWriter fw = new FileWriter(target);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("== Memory Inspector dump ==");
            pw.println("Port: " + (link != null
                    ? link.getPortName() + " @ " + link.getBaud()
                    : "(unknown)"));
            pw.println("Timestamp: "
                    + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pw.println("Start address: $" + HexUtils.toHex4(session.startAddr));
            pw.println("Bytes: " + session.mirrorLen);
            pw.println("View mode: " + session.viewMode);
            pw.println("==");
            pw.println();
            pw.print(DumpController.renderBuffer(
                    session.mirrorBuffer, session.mirrorLen,
                    session.startAddr, session.viewMode));
            log.logRaw("dump saved to " + target.getAbsolutePath()
                    + " (" + session.mirrorLen + " bytes, " + session.viewMode + ")");

            // Persist the parent directory so the next Save Dump… lands
            // where the user was last working.
            File parent = target.getParentFile();
            if (parent != null && parent.isDirectory()) {
                settings.setLastSaveDir(parent.getAbsolutePath());
                settings.flush();
            }
        } catch (IOException ioe) {
            log.logRaw("save dump failed: " + ioe.getMessage());
            JOptionPane.showMessageDialog(frame,
                    "Failed to save dump:\n" + ioe.getMessage(),
                    "Memory Inspector — Save Dump",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Resolve the {@link JFileChooser} starting directory for Save
     * Dump…: prefer the persisted {@code lastSaveDir} if it still
     * exists, else fall back to {@code user.home}. Avoids the common
     * UX complaint where Save Dump always opens in the home folder
     * even though the user has a specific dumps directory they return
     * to every session.
     */
    private static File resolveSaveStartDir(AppSettings settings) {
        String saved = settings.getLastSaveDir();
        if (saved != null && !saved.isEmpty()) {
            File f = new File(saved);
            if (f.isDirectory()) {
                return f;
            }
        }
        return new File(System.getProperty("user.home"));
    }

    /** File → Exit handler — clean shutdown + JVM exit. */
    private static void doExitClick(AtomicReference<SerialLink> linkRef,
                                    AtomicReference<PK232Client> clientRef,
                                    AtomicReference<DumpController> dumpCtlRef,
                                    PacketLogger log,
                                    AtomicBoolean shutdownDone) {
        runShutdown(linkRef, clientRef, dumpCtlRef, log, shutdownDone,
                "exit via File → Exit");
        System.exit(0);
    }

    /** Help → About handler. */
    private static void showAboutDialog(JFrame parent) {
        String logsPath = new File("Logs").getAbsolutePath();
        String msg = "<html>"
                + "<b>Memory Inspector</b> " + APP_VERSION + "<br>"
                + "AEA PK-232 live memory dump utility<br><br>"
                + "By KJ7RBS using Cursor and Opus 4.7<br>"
                + "Beta Release 2026-04-22<br><br>"
                + "Contact: kj7rbs@gmail.com<br>"
                + "         14.088Mhz Dial PACTOR USB<br><br>"
                + "Java: " + System.getProperty("java.version") + "<br>"
                + "OS: " + System.getProperty("os.name")
                + " " + System.getProperty("os.version") + "<br>"
                + "Logs: <code>" + logsPath + "</code>"
                + "</html>";
        JOptionPane.showMessageDialog(parent, msg,
                "About Memory Inspector",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Settings → Port… handler. Prompts to cancel any in-flight dump,
     * then opens the {@link StartupConnectDialog} (reused verbatim)
     * and dispatches the reconnect worker. Overlapping clicks are
     * gated by {@link #reconnectInProgress}.
     */
    private static void doSettingsPortClick(JFrame frame,
                                            PacketLogger log,
                                            JLabel statusBar,
                                            JTextArea output,
                                            AtomicReference<SerialLink> linkRef,
                                            AtomicReference<PK232Client> clientRef,
                                            AtomicReference<DumpController> dumpCtlRef) {
        if (reconnectInProgress.get()) {
            JOptionPane.showMessageDialog(frame,
                    "A reconnect is already in progress. Please wait for it to finish.",
                    "Memory Inspector — Reconnect",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DumpController dc = dumpCtlRef.get();
        if (dc != null && dc.isRunning()) {
            int choice = JOptionPane.showConfirmDialog(frame,
                    "A dump is in progress.\n\n"
                            + "Cancel it and reconnect to a different port?",
                    "Memory Inspector — Reconnect",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            dc.cancel();
        }

        AppSettings settings = new AppSettings();
        StartupConnectDialog dialog = new StartupConnectDialog(frame, settings);
        StartupConnectDialog.Result result = dialog.showDialog();
        if (result == null) {
            return;
        }

        if (!reconnectInProgress.compareAndSet(false, true)) {
            JOptionPane.showMessageDialog(frame,
                    "A reconnect is already in progress. Please wait for it to finish.",
                    "Memory Inspector — Reconnect",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        runReconnectOnWorker(frame, log, result, statusBar, output,
                linkRef, clientRef, dumpCtlRef);
    }

    /**
     * Spawn the {@code MemoryInspector-reconnect} worker that tears down
     * the current SerialLink + PK232Client + DumpController, opens the
     * user-chosen port, re-runs detection + host-mode entry, and swaps
     * fresh instances into the existing {@link AtomicReference} slots.
     * Deliberately inline (not factored with {@link #runDetectAndEntryLoop})
     * — the two paths diverge in progress-label content, failure UX, and
     * handle-passing (truenorth §8 M6 design lock).
     */
    private static void runReconnectOnWorker(JFrame frame,
                                             PacketLogger log,
                                             StartupConnectDialog.Result target,
                                             JLabel statusBar,
                                             JTextArea output,
                                             AtomicReference<SerialLink> linkRef,
                                             AtomicReference<PK232Client> clientRef,
                                             AtomicReference<DumpController> dumpCtlRef) {
        DetectProgress progress = buildProgressDialog();
        progress.dialog.setTitle("Memory Inspector — Reconnecting");
        setProgressLabel(progress, "Preparing to reconnect…");
        progress.dialog.setVisible(true);

        Thread worker = new Thread(() -> {
            try {
                log.logRaw("reconnect: requested " + target.portName()
                        + " @ " + target.baud());

                // 1. Wait for any dump to unblock (the flag was already
                // flipped on the EDT; this gives the worker up to 2500 ms
                // to exit via sendAndAwait's 1500 ms default timeout).
                DumpController oldDump = dumpCtlRef.get();
                if (oldDump != null && oldDump.isRunning()) {
                    setProgressLabel(progress, "Cancelling in-progress dump…");
                    long deadline = System.currentTimeMillis() + 2500;
                    while (oldDump.isRunning()
                            && System.currentTimeMillis() < deadline) {
                        Thread.sleep(50);
                    }
                }

                // 2. Stop the old client BEFORE tryExit (§5.11 ordering).
                PK232Client oldClient = clientRef.getAndSet(null);
                if (oldClient != null) {
                    setProgressLabel(progress, "Stopping PK232Client…");
                    try {
                        oldClient.close();
                    } catch (RuntimeException re) {
                        log.logRaw("reconnect: old client.close crashed: " + re);
                    }
                }

                // 3. HO N + BREAK fallback on the old link.
                SerialLink oldLink = linkRef.getAndSet(null);
                if (oldLink != null && oldLink.isOpen()) {
                    setProgressLabel(progress, "Leaving host mode…");
                    try {
                        HostModeEntry.tryExit(oldLink, log);
                    } catch (RuntimeException re) {
                        log.logRaw("reconnect: tryExit crashed: " + re);
                    }
                    oldLink.close();
                }
                dumpCtlRef.set(null);

                // 4. Open new link.
                setProgressLabel(progress,
                        "Opening " + target.portName() + " @ " + target.baud() + "…");
                SerialLink newLink = new SerialLink(target.portName(), target.baud());
                try {
                    newLink.open();
                } catch (IOException ioe) {
                    reconnectFailUi(frame, progress,
                            "Failed to open " + target.portName() + ":\n" + ioe.getMessage(),
                            log, "reconnect: open failed: " + ioe.getMessage());
                    return;
                }
                linkRef.set(newLink);
                log.logRaw("reconnect: opened " + target.portName()
                        + " @ " + target.baud());

                // 5. Detect state.
                ModemState state;
                try {
                    setProgressLabel(progress, "Detecting PK-232 state…");
                    state = new StartupDetector(newLink, log,
                            Main::powerCyclePromptCrossThread).detect();
                } catch (IOException | StartupDetector.DetectionException e) {
                    reconnectFailUi(frame, progress,
                            "Detection failed:\n" + e.getMessage(),
                            log, "reconnect: detection failed: " + e.getMessage());
                    safeClose(linkRef, newLink);
                    return;
                }

                // 6. Enter host mode.
                try {
                    setProgressLabel(progress, "Entering host mode…");
                    new HostModeEntry(newLink, log, state).enter();
                } catch (HostModeEntryException | IOException e) {
                    reconnectFailUi(frame, progress,
                            "Host-mode entry failed:\n" + e.getMessage(),
                            log, "reconnect: host-mode entry failed: " + e.getMessage());
                    safeClose(linkRef, newLink);
                    return;
                }

                // 7. Start new PK232Client with a fresh idle unsolicited
                // handler identical in shape to the boot-path handler.
                final Consumer<HostBlock> idleUnsolicitedHandler = block ->
                        log.logRaw("unsolicited block from modem: " + block);
                PK232Client newClient = new PK232Client(newLink, log);
                newClient.setUnsolicitedHandler(idleUnsolicitedHandler);
                try {
                    setProgressLabel(progress, "Starting PK232Client…");
                    newClient.start();
                } catch (RuntimeException re) {
                    reconnectFailUi(frame, progress,
                            "PK232Client failed to start:\n" + re.getMessage(),
                            log, "reconnect: client start failed: " + re);
                    safeClose(linkRef, newLink);
                    return;
                }
                clientRef.set(newClient);

                DumpController newDump = new DumpController(
                        newClient, log, idleUnsolicitedHandler);
                dumpCtlRef.set(newDump);

                log.logRaw("reconnect: complete (port=" + target.portName()
                        + " baud=" + target.baud() + " state=" + state + ")");

                // 8. Update UI.
                final ModemState finalState = state;
                SwingUtilities.invokeLater(() -> {
                    progress.dialog.dispose();
                    statusBar.setText(buildStatusBarHtml(newLink, finalState));
                    frame.setTitle("Memory Inspector — Beta Release — "
                            + newLink.getPortName());
                    mutateOutputPreservingScroll(output, () ->
                            output.append(String.format(
                                    "%n[RECONNECTED to %s @ %d]%n",
                                    newLink.getPortName(), newLink.getBaud())));
                });
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(progress.dialog::dispose);
            } catch (RuntimeException re) {
                log.logRaw("reconnect crashed: " + re);
                SwingUtilities.invokeLater(() -> {
                    progress.dialog.dispose();
                    JOptionPane.showMessageDialog(frame,
                            "Reconnect failed unexpectedly:\n" + re.getMessage(),
                            "Memory Inspector — Reconnect",
                            JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                reconnectInProgress.set(false);
            }
        }, "MemoryInspector-reconnect");
        worker.setDaemon(false);
        worker.start();
    }

    /** Close + null-out a partially-opened link on a reconnect failure path. */
    private static void safeClose(AtomicReference<SerialLink> linkRef, SerialLink link) {
        linkRef.compareAndSet(link, null);
        if (link != null && link.isOpen()) {
            link.close();
        }
    }

    /** Dispose the reconnect progress dialog and show a modal error. */
    private static void reconnectFailUi(JFrame frame, DetectProgress progress,
                                        String userMessage, PacketLogger log,
                                        String logLine) {
        log.logRaw(logLine);
        SwingUtilities.invokeLater(() -> {
            progress.dialog.dispose();
            JOptionPane.showMessageDialog(frame,
                    userMessage + "\n\nThe app is still running; use Settings → Port… to try again.",
                    "Memory Inspector — Reconnect",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Idempotent shutdown path shared by the window-close handler, the
     * startup-failure path, and the JVM shutdown hook. Ordering per
     * truenorth §5.11 + M5 design-lock (2026-04-21):
     * <ol>
     *   <li>{@code dumpController.cancel()} — lets the dump worker bail
     *       on its next iteration instead of walking into a
     *       {@code PK232Client.close()}-induced {@code IOException}
     *       from the reader (pure optimization; the worker would exit
     *       the wrong way anyway).</li>
     *   <li>{@code client.close()} — stops the reader + protocol
     *       threads (500 ms join × 2 caps this).</li>
     *   <li>{@code HostModeEntry.tryExit(link, log)} — sends double-SOH
     *       {@code HO N} with a 300 ms BREAK fallback and 1500 ms cap
     *       (§5.11).</li>
     *   <li>{@code link.close()} + {@code log.close()}.</li>
     * </ol>
     * Reversing (2) and (3) causes the reader to race {@code tryExit}'s
     * bare-link read.
     */
    private static void runShutdown(AtomicReference<SerialLink> linkRef,
                                    AtomicReference<PK232Client> clientRef,
                                    AtomicReference<DumpController> dumpCtlRef,
                                    PacketLogger log,
                                    AtomicBoolean shutdownDone,
                                    String reason) {
        if (!shutdownDone.compareAndSet(false, true)) {
            return;
        }
        log.logRaw(reason);

        DumpController dump = dumpCtlRef != null ? dumpCtlRef.getAndSet(null) : null;
        if (dump != null && dump.isRunning()) {
            try {
                dump.cancel();
            } catch (RuntimeException re) {
                log.logRaw("shutdown: DumpController.cancel crashed (" + re + ")");
            }
        }

        PK232Client client = clientRef.getAndSet(null);
        if (client != null) {
            try {
                client.close();
            } catch (RuntimeException re) {
                log.logRaw("shutdown: PK232Client.close crashed (" + re + ")");
            }
        }

        SerialLink link = linkRef.getAndSet(null);
        if (link != null && link.isOpen()) {
            try {
                HostModeEntry.tryExit(link, log);
            } catch (RuntimeException re) {
                log.logRaw("shutdown: tryExit crashed (" + re + ")");
            }
            link.close();
        }
        log.close();
    }

    private static Dimension twoThirdsScreen() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension((screen.width * 2) / 3, (screen.height * 2) / 3);
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // Silent fallback to default L&F per the handoff spec.
        }
    }
}
