package ui;

import config.AppSettings;
import serial.SerialLink;
import serial.SerialLink.PortInfo;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;

/**
 * Modal startup dialog that collects the COM port, baud, detection timeout,
 * and "don't show on launch" preference per truenorth §5.3 / §5.4 and the
 * locked answers D1/D3/D4/D5.
 *
 * <p>On OK, the four fields are written through to {@link AppSettings} and a
 * {@link Result} is returned. On Cancel / ESC / window-close the return is
 * {@code null}. Serial params (8-N-1, no flow control) are not exposed here
 * per D2 — they live inside {@link SerialLink}.
 */
public final class StartupConnectDialog extends JDialog {

    public static final int[] BAUDS = {1200, 2400, 4800, 9600, 19200};

    public static final int MIN_TIMEOUT_MS  = 1000;
    public static final int MAX_TIMEOUT_MS  = 60000;
    public static final int TIMEOUT_STEP_MS = 500;

    /** Immutable selection result returned to {@code Main}. */
    public record Result(String portName, int baud, int detectionTimeoutMs, boolean skipStartupDialog) {}

    private final AppSettings settings;

    private JComboBox<PortInfo> portCombo;
    private JButton             refreshButton;
    private JComboBox<Integer>  baudCombo;
    private JSpinner            timeoutSpinner;
    private JCheckBox           skipCheckBox;
    private JButton             okButton;
    private JButton             cancelButton;

    private Result result;

    public StartupConnectDialog(Frame owner, AppSettings settings) {
        super(owner, "Memory Inspector — Connect", true);
        this.settings = Objects.requireNonNull(settings, "settings");
        buildUi();
        populateFromSettings();
        wireActions();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Show the dialog modally. Blocks until the user hits OK or Cancel.
     * Returns the populated {@link Result}, or {@code null} if the user
     * cancelled / closed the window.
     */
    public Result showDialog() {
        this.result = null;
        setVisible(true);
        return this.result;
    }

    private void buildUi() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(fields, gbc, row++, "Port:",              buildPortPanel());
        addRow(fields, gbc, row++, "Baud:",              buildBaudPanel());
        addRow(fields, gbc, row++, "Detection timeout:", buildTimeoutPanel());

        gbc.gridy     = row;
        gbc.gridx     = 0;
        gbc.gridwidth = 2;
        gbc.weightx   = 1.0;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        fields.add(buildSkipPanel(), gbc);

        setContentPane(new JPanel(new BorderLayout()));
        getContentPane().add(fields,            BorderLayout.CENTER);
        getContentPane().add(buildFooterPanel(), BorderLayout.SOUTH);
    }

    private static void addRow(JPanel host, GridBagConstraints gbc, int row, String label, JPanel widget) {
        gbc.gridy     = row;
        gbc.gridx     = 0;
        gbc.gridwidth = 1;
        gbc.weightx   = 0;
        gbc.fill      = GridBagConstraints.NONE;
        host.add(new JLabel(label), gbc);

        gbc.gridx   = 1;
        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        host.add(widget, gbc);
    }

    private JPanel buildPortPanel() {
        // Default renderer calls toString() — PortInfo.toString() already
        // yields the "COM3 — USB Serial" form we want to display.
        portCombo     = new JComboBox<>();
        refreshButton = new JButton("Refresh");

        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(portCombo,     BorderLayout.CENTER);
        p.add(refreshButton, BorderLayout.EAST);
        return p;
    }

    private JPanel buildBaudPanel() {
        baudCombo = new JComboBox<>();
        for (int b : BAUDS) {
            baudCombo.addItem(b);
        }
        JPanel p = new JPanel(new BorderLayout());
        p.add(baudCombo, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTimeoutPanel() {
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(
                AppSettings.DEFAULT_DETECTION_TIMEOUT_MS,
                MIN_TIMEOUT_MS,
                MAX_TIMEOUT_MS,
                TIMEOUT_STEP_MS));
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(timeoutSpinner, BorderLayout.CENTER);
        p.add(new JLabel("ms"), BorderLayout.EAST);
        return p;
    }

    private JPanel buildSkipPanel() {
        skipCheckBox = new JCheckBox("Don't show this dialog on launch");
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(skipCheckBox);
        return p;
    }

    private JPanel buildFooterPanel() {
        okButton     = new JButton("OK");
        cancelButton = new JButton("Cancel");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        p.add(okButton);
        p.add(cancelButton);

        getRootPane().setDefaultButton(okButton);
        return p;
    }

    private void populateFromSettings() {
        reloadPortList();

        int savedBaud = settings.getBaud();
        for (int i = 0; i < BAUDS.length; i++) {
            if (BAUDS[i] == savedBaud) {
                baudCombo.setSelectedIndex(i);
                break;
            }
        }

        timeoutSpinner.setValue(clampTimeout(settings.getDetectionTimeoutMs()));
        skipCheckBox.setSelected(settings.isSkipStartupDialog());
    }

    private void reloadPortList() {
        PortInfo previous  = (PortInfo) portCombo.getSelectedItem();
        String   savedPort = settings.getComPort();

        List<PortInfo> ports = SerialLink.listPorts();
        DefaultComboBoxModel<PortInfo> model = new DefaultComboBoxModel<>();
        for (PortInfo pi : ports) {
            model.addElement(pi);
        }
        portCombo.setModel(model);

        if (ports.isEmpty()) {
            okButton.setEnabled(false);
            return;
        }
        okButton.setEnabled(true);

        PortInfo target = findByName(ports, previous == null ? null : previous.systemName());
        if (target == null && !savedPort.isEmpty()) {
            target = findByName(ports, savedPort);
        }
        portCombo.setSelectedItem(target != null ? target : ports.get(0));
    }

    private static PortInfo findByName(List<PortInfo> ports, String systemName) {
        if (systemName == null || systemName.isEmpty()) {
            return null;
        }
        for (PortInfo pi : ports) {
            if (pi.systemName().equals(systemName)) {
                return pi;
            }
        }
        return null;
    }

    private void wireActions() {
        refreshButton.addActionListener(e -> reloadPortList());
        okButton    .addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> onCancel());

        // ESC anywhere in the window maps to Cancel. Default button (OK)
        // already covers ENTER via JRootPane.setDefaultButton.
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void onOk() {
        PortInfo selected = (PortInfo) portCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "No COM port selected. Plug one in and press Refresh.",
                    "Memory Inspector — Connect",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer baudBoxed = (Integer) baudCombo.getSelectedItem();
        int baud      = baudBoxed == null ? AppSettings.DEFAULT_BAUD : baudBoxed;
        int timeoutMs = clampTimeout(((Number) timeoutSpinner.getValue()).intValue());
        boolean skip  = skipCheckBox.isSelected();

        settings.setComPort(selected.systemName());
        settings.setBaud(baud);
        settings.setDetectionTimeoutMs(timeoutMs);
        settings.setSkipStartupDialog(skip);
        settings.flush();

        this.result = new Result(selected.systemName(), baud, timeoutMs, skip);
        dispose();
    }

    private void onCancel() {
        this.result = null;
        dispose();
    }

    private static int clampTimeout(int ms) {
        if (ms < MIN_TIMEOUT_MS) return MIN_TIMEOUT_MS;
        if (ms > MAX_TIMEOUT_MS) return MAX_TIMEOUT_MS;
        return ms;
    }
}
