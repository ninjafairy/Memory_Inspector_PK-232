package ui;

import util.HexUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Modal ADDRESS + BYTES prompt used by the M4 Dump button and, in M5+,
 * by the full {@link ui.MainFrame} Dump button verbatim. Truenorth C3 + C8
 * rules live here:
 *
 * <ul>
 *   <li>ADDRESS: exactly 4 hex chars ({@code 0-9 A-F} case-insensitive),
 *       no {@code $} / {@code 0x} prefix, normalized to uppercase on
 *       submit. Live-filtered at the document level so non-hex keystrokes
 *       are dropped silently.</li>
 *   <li>BYTES: decimal 1..99999. Leading zeros stripped on submit.
 *       Input of {@code 0} fires {@link JOptionPane} with the exact literal
 *       copy <em>{@code "OK ive done nothing are you happy?"}</em> and
 *       leaves the dialog open.</li>
 *   <li>{@code ADDR + BYTES > 0x10000} fires an "end of memory" warning
 *       and returns the clamped value {@code bytes' = 0x10000 - addr}
 *       per truenorth C4.</li>
 * </ul>
 *
 * <p>ESC and window-close both cancel; ENTER in either field is wired to
 * the OK action via the root pane's default button.
 */
public final class DumpPromptDialog extends JDialog {

    public static final int MAX_ADDRESS   = 0xFFFF;
    public static final int MAX_BYTES     = 99999;
    public static final int ADDRESS_SPACE = 0x10000;

    /** Result of a successful OK — ADDR and BYTES both already validated + clamped. */
    public record Result(int addr, int bytes) { }

    private final JTextField addrField  = new JTextField(6);
    private final JTextField bytesField = new JTextField(6);

    private Result result;

    /**
     * Convenience factory used by callers that just want to show-and-await.
     * {@code parent} may be {@code null}. Defaults populate the two fields
     * so a quick retry doesn't require re-typing.
     */
    public static Result showDialog(Window parent, int defaultAddr, int defaultBytes) {
        DumpPromptDialog d = new DumpPromptDialog(parent, defaultAddr, defaultBytes);
        d.setVisible(true);
        return d.result;
    }

    private DumpPromptDialog(Window parent, int defaultAddr, int defaultBytes) {
        super(parent, "Memory Inspector — Dump", Dialog.ModalityType.APPLICATION_MODAL);
        setResizable(false);

        ((PlainDocument) addrField.getDocument()).setDocumentFilter(new HexUppercaseFilter(4));
        ((PlainDocument) bytesField.getDocument()).setDocumentFilter(new DecimalDigitsFilter(5));

        addrField.setText(clampAddrToHex(defaultAddr));
        bytesField.setText(Integer.toString(Math.max(1, Math.min(defaultBytes, MAX_BYTES))));

        JButton okBtn     = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> onOk());
        cancelBtn.addActionListener(e -> onCancel());
        getRootPane().setDefaultButton(okBtn);

        JComponent root = (JComponent) getContentPane();
        root.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        root.setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 0, 4, 8);
        gc.anchor = GridBagConstraints.LINE_START;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("ADDRESS (4 hex):"), gc);
        gc.gridx = 1;
        form.add(addrField, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel("BYTES (1..99999):"), gc);
        gc.gridx = 1;
        form.add(bytesField, gc);
        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(okBtn);
        buttons.add(cancelBtn);
        root.add(buttons, BorderLayout.SOUTH);

        // ESC cancels; mirrors the StartupConnectDialog convention.
        root.registerKeyboardAction(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onCancel(); }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onCancel(); }
        });

        pack();
        setLocationRelativeTo(parent);
    }

    private void onOk() {
        String hex   = addrField.getText();
        String bytes = bytesField.getText().replaceFirst("^0+(?!$)", "");

        int addr;
        try {
            addr = HexUtils.parse4Hex(hex);
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this,
                    "ADDRESS must be exactly 4 hex characters (0-9, A-F).",
                    "Invalid ADDRESS", JOptionPane.WARNING_MESSAGE);
            addrField.requestFocusInWindow();
            addrField.selectAll();
            return;
        }

        int bytesInt;
        try {
            bytesInt = Integer.parseInt(bytes);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this,
                    "BYTES must be a decimal number from 1 to " + MAX_BYTES + ".",
                    "Invalid BYTES", JOptionPane.WARNING_MESSAGE);
            bytesField.requestFocusInWindow();
            bytesField.selectAll();
            return;
        }

        // Truenorth C3: exact literal copy on BYTES == 0. Anything negative
        // can't happen (DecimalDigitsFilter rejects '-'), but guard anyway.
        if (bytesInt <= 0) {
            JOptionPane.showMessageDialog(this,
                    "OK ive done nothing are you happy?",
                    "Memory Inspector", JOptionPane.INFORMATION_MESSAGE);
            bytesField.requestFocusInWindow();
            bytesField.selectAll();
            return;
        }
        if (bytesInt > MAX_BYTES) {
            JOptionPane.showMessageDialog(this,
                    "BYTES must be at most " + MAX_BYTES + ".",
                    "Invalid BYTES", JOptionPane.WARNING_MESSAGE);
            bytesField.requestFocusInWindow();
            bytesField.selectAll();
            return;
        }

        // Truenorth C4: clamp at 0xFFFF with an "end of memory" warning.
        long end = (long) addr + (long) bytesInt;
        if (end > ADDRESS_SPACE) {
            int clamped = ADDRESS_SPACE - addr;
            int choice = JOptionPane.showConfirmDialog(this,
                    "Requested range runs past the end of memory (0xFFFF).\n\n"
                            + "Clamping to " + clamped + " bytes ("
                            + String.format("$%04X..$FFFF", addr) + ").\n\n"
                            + "Continue?",
                    "Memory Inspector — End of memory",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            bytesInt = clamped;
        }

        this.result = new Result(addr, bytesInt);
        dispose();
    }

    private void onCancel() {
        this.result = null;
        dispose();
    }

    private static String clampAddrToHex(int addr) {
        int a = addr < 0 ? 0 : Math.min(addr, MAX_ADDRESS);
        return HexUtils.toHex4(a);
    }

    // ------------------------------------------------------------
    // Document filters — live input validation + normalization.
    // ------------------------------------------------------------

    /** Accepts only hex digits up to {@code maxLen}, stored uppercase. */
    private static final class HexUppercaseFilter extends DocumentFilter {
        private final int maxLen;
        HexUppercaseFilter(int maxLen) { this.maxLen = maxLen; }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet a)
                throws BadLocationException {
            String sanitized = sanitize(text, fb.getDocument().getLength(), 0);
            if (!sanitized.isEmpty()) super.insertString(fb, offset, sanitized, a);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet a)
                throws BadLocationException {
            String sanitized = sanitize(text, fb.getDocument().getLength(), length);
            super.replace(fb, offset, length, sanitized, a);
        }

        private String sanitize(String text, int currentLen, int replacingLen) {
            if (text == null) return "";
            int room = maxLen - (currentLen - replacingLen);
            StringBuilder out = new StringBuilder(Math.min(text.length(), Math.max(0, room)));
            for (int i = 0; i < text.length() && out.length() < room; i++) {
                char c = text.charAt(i);
                if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                    out.append(c);
                } else if (c >= 'a' && c <= 'f') {
                    out.append(Character.toUpperCase(c));
                }
                // Silently drop everything else (pasted spaces, $, 0x prefix, etc.).
            }
            return out.toString();
        }
    }

    /** Accepts only decimal digits up to {@code maxLen}. */
    private static final class DecimalDigitsFilter extends DocumentFilter {
        private final int maxLen;
        DecimalDigitsFilter(int maxLen) { this.maxLen = maxLen; }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet a)
                throws BadLocationException {
            String sanitized = sanitize(text, fb.getDocument().getLength(), 0);
            if (!sanitized.isEmpty()) super.insertString(fb, offset, sanitized, a);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet a)
                throws BadLocationException {
            String sanitized = sanitize(text, fb.getDocument().getLength(), length);
            super.replace(fb, offset, length, sanitized, a);
        }

        private String sanitize(String text, int currentLen, int replacingLen) {
            if (text == null) return "";
            int room = maxLen - (currentLen - replacingLen);
            StringBuilder out = new StringBuilder(Math.min(text.length(), Math.max(0, room)));
            for (int i = 0; i < text.length() && out.length() < room; i++) {
                char c = text.charAt(i);
                if (c >= '0' && c <= '9') out.append(c);
            }
            return out.toString();
        }
    }

}
