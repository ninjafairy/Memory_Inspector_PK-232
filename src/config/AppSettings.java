package config;

import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * User preferences backed by {@link java.util.prefs.Preferences} at
 * {@code /BFSoft/MemoryInspector} (truenorth §5.8). Every field from the spec
 * lives here; settings survive across launches.
 *
 * <p>{@code comPort} and {@code lastAddress} default to the empty string
 * (meaning "no saved value"). All other fields have concrete defaults aligned
 * with the truenorth defaults (baud 9600, detectionTimeoutMs 8000, view mode
 * ASCII, don't skip startup dialog, lastBytes 0).
 */
public final class AppSettings {

    public enum ViewMode { ASCII, HEX }

    public static final String PREFS_PATH = "/BFSoft/MemoryInspector";

    private static final String KEY_COM_PORT              = "comPort";
    private static final String KEY_BAUD                  = "baud";
    private static final String KEY_DETECTION_TIMEOUT_MS  = "detectionTimeoutMs";
    private static final String KEY_SKIP_STARTUP_DIALOG   = "skipStartupDialog";
    private static final String KEY_VIEW_MODE             = "viewMode";
    private static final String KEY_LAST_ADDRESS          = "lastAddress";
    private static final String KEY_LAST_BYTES            = "lastBytes";
    private static final String KEY_LAST_SAVE_DIR         = "lastSaveDir";

    public static final int      DEFAULT_BAUD                 = 9600;
    public static final int      DEFAULT_DETECTION_TIMEOUT_MS = 8000;
    public static final boolean  DEFAULT_SKIP_STARTUP_DIALOG  = false;
    public static final ViewMode DEFAULT_VIEW_MODE            = ViewMode.ASCII;
    public static final int      DEFAULT_LAST_BYTES           = 0;

    private final Preferences prefs;

    public AppSettings() {
        this(Preferences.userRoot().node(PREFS_PATH));
    }

    /** Test seam: pass a specific Preferences node (e.g. an in-memory stub). */
    public AppSettings(Preferences prefs) {
        this.prefs = Objects.requireNonNull(prefs, "prefs");
    }

    public String getComPort() {
        return prefs.get(KEY_COM_PORT, "");
    }

    public void setComPort(String portName) {
        if (portName == null) {
            prefs.remove(KEY_COM_PORT);
        } else {
            prefs.put(KEY_COM_PORT, portName);
        }
    }

    public int getBaud() {
        return prefs.getInt(KEY_BAUD, DEFAULT_BAUD);
    }

    public void setBaud(int baud) {
        prefs.putInt(KEY_BAUD, baud);
    }

    public int getDetectionTimeoutMs() {
        return prefs.getInt(KEY_DETECTION_TIMEOUT_MS, DEFAULT_DETECTION_TIMEOUT_MS);
    }

    public void setDetectionTimeoutMs(int ms) {
        prefs.putInt(KEY_DETECTION_TIMEOUT_MS, ms);
    }

    public boolean isSkipStartupDialog() {
        return prefs.getBoolean(KEY_SKIP_STARTUP_DIALOG, DEFAULT_SKIP_STARTUP_DIALOG);
    }

    public void setSkipStartupDialog(boolean skip) {
        prefs.putBoolean(KEY_SKIP_STARTUP_DIALOG, skip);
    }

    public ViewMode getViewMode() {
        String raw = prefs.get(KEY_VIEW_MODE, DEFAULT_VIEW_MODE.name());
        try {
            return ViewMode.valueOf(raw);
        } catch (IllegalArgumentException iae) {
            return DEFAULT_VIEW_MODE;
        }
    }

    public void setViewMode(ViewMode mode) {
        prefs.put(KEY_VIEW_MODE, mode == null ? DEFAULT_VIEW_MODE.name() : mode.name());
    }

    /**
     * Returns the last-used ADDRESS as a 4-hex uppercase string, or the empty
     * string if none has been saved. Callers are responsible for validating
     * the value before use (per truenorth C8).
     */
    public String getLastAddress() {
        String raw = prefs.get(KEY_LAST_ADDRESS, "");
        return raw == null ? "" : raw;
    }

    public void setLastAddress(String hex4) {
        if (hex4 == null || hex4.isEmpty()) {
            prefs.remove(KEY_LAST_ADDRESS);
        } else {
            prefs.put(KEY_LAST_ADDRESS, hex4.toUpperCase());
        }
    }

    public int getLastBytes() {
        return prefs.getInt(KEY_LAST_BYTES, DEFAULT_LAST_BYTES);
    }

    public void setLastBytes(int bytes) {
        prefs.putInt(KEY_LAST_BYTES, bytes);
    }

    /**
     * Directory last used for {@code File → Save Dump…}, or the empty
     * string if none has been saved. Callers should fall back to
     * {@code user.home} on empty or when the directory no longer exists.
     */
    public String getLastSaveDir() {
        String raw = prefs.get(KEY_LAST_SAVE_DIR, "");
        return raw == null ? "" : raw;
    }

    public void setLastSaveDir(String path) {
        if (path == null || path.isEmpty()) {
            prefs.remove(KEY_LAST_SAVE_DIR);
        } else {
            prefs.put(KEY_LAST_SAVE_DIR, path);
        }
    }

    /**
     * Force a write-back of any pending preferences changes. Swallows the
     * checked {@link BackingStoreException} so callers in UI code don't have
     * to; persistence failures are not fatal to the app.
     */
    public void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException bse) {
            System.err.println("[AppSettings] flush failed: " + bse.getMessage());
        }
    }
}
