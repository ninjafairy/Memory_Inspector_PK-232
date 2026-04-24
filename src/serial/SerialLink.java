package serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thin wrapper around jSerialComm for the Memory Inspector. Hard-codes
 * 8-N-1 and no flow control per truenorth D2; the only configurable
 * parameter is the baud rate (D1). No framing / parsing logic lives here —
 * that belongs to the {@code protocol} package.
 *
 * <p>Reads are SEMI_BLOCKING with a caller-settable timeout so a protocol
 * reader can loop one byte at a time without hanging forever. Writes are
 * BLOCKING.
 */
public final class SerialLink implements AutoCloseable {

    /** Immutable description of an enumerable COM port. */
    public static final class PortInfo {
        private final String systemName;
        private final String descriptiveName;

        public PortInfo(String systemName, String descriptiveName) {
            this.systemName = Objects.requireNonNull(systemName, "systemName");
            this.descriptiveName = descriptiveName == null ? systemName : descriptiveName;
        }

        public String systemName() { return systemName; }
        public String descriptiveName() { return descriptiveName; }

        @Override
        public String toString() {
            return systemName.equals(descriptiveName)
                    ? systemName
                    : systemName + " — " + descriptiveName;
        }
    }

    public static final int DEFAULT_READ_TIMEOUT_MS  = 1000;
    public static final int DEFAULT_WRITE_TIMEOUT_MS = 1000;

    private final String portName;
    private int baud;
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
    private int writeTimeoutMs = DEFAULT_WRITE_TIMEOUT_MS;

    private SerialPort port;

    public SerialLink(String portName, int baud) {
        this.portName = Objects.requireNonNull(portName, "portName");
        if (baud <= 0) {
            throw new IllegalArgumentException("baud must be positive: " + baud);
        }
        this.baud = baud;
    }

    /**
     * Enumerate the serial ports the OS is currently advertising. Hot-plug
     * refresh is a single call away: the UI's Refresh button should just
     * re-invoke this method.
     */
    public static List<PortInfo> listPorts() {
        SerialPort[] raw = SerialPort.getCommPorts();
        if (raw == null || raw.length == 0) {
            return Collections.emptyList();
        }
        List<PortInfo> out = new ArrayList<>(raw.length);
        for (SerialPort sp : raw) {
            out.add(new PortInfo(sp.getSystemPortName(), sp.getDescriptivePortName()));
        }
        return Collections.unmodifiableList(out);
    }

    public String getPortName()     { return portName; }
    public int getBaud()            { return baud; }
    public int getReadTimeoutMs()   { return readTimeoutMs; }
    public int getWriteTimeoutMs()  { return writeTimeoutMs; }
    public boolean isOpen()         { return port != null && port.isOpen(); }

    public void setReadTimeoutMs(int ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("read timeout must be >= 0");
        }
        this.readTimeoutMs = ms;
        if (isOpen()) {
            applyTimeouts();
        }
    }

    public void setWriteTimeoutMs(int ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("write timeout must be >= 0");
        }
        this.writeTimeoutMs = ms;
        if (isOpen()) {
            applyTimeouts();
        }
    }

    /**
     * Open the port at the configured baud, 8-N-1, no flow control.
     * Idempotent: a no-op if already open.
     */
    public void open() throws IOException {
        if (isOpen()) {
            return;
        }
        SerialPort sp;
        try {
            sp = SerialPort.getCommPort(portName);
        } catch (SerialPortInvalidPortException ex) {
            throw new IOException("No such port: " + portName, ex);
        }
        boolean paramsOk = sp.setComPortParameters(
                baud, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!paramsOk) {
            throw new IOException("Failed to set 8-N-1 @ " + baud + " on " + portName);
        }
        boolean flowOk = sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!flowOk) {
            throw new IOException("Failed to disable flow control on " + portName);
        }
        this.port = sp;
        applyTimeouts();
        if (!sp.openPort()) {
            this.port = null;
            throw new IOException("Failed to open " + portName + " @ " + baud);
        }
    }

    @Override
    public void close() {
        SerialPort sp = this.port;
        this.port = null;
        if (sp != null && sp.isOpen()) {
            sp.closePort();
        }
    }

    /**
     * Read up to {@code length} bytes into {@code buf} starting at
     * {@code offset}. Returns the number actually read (may be 0 if the
     * read timed out with no data). Never returns negative.
     */
    public int read(byte[] buf, int offset, int length) throws IOException {
        requireOpen();
        Objects.checkFromIndexSize(offset, length, buf.length);
        if (length == 0) {
            return 0;
        }
        int n = port.readBytes(buf, length, offset);
        return Math.max(n, 0);
    }

    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    /**
     * Write the full byte range. Blocks until the UART driver accepts all
     * bytes or the configured write timeout elapses. Throws on short writes.
     */
    public void write(byte[] data, int offset, int length) throws IOException {
        requireOpen();
        Objects.checkFromIndexSize(offset, length, data.length);
        if (length == 0) {
            return;
        }
        int n = port.writeBytes(data, length, offset);
        if (n != length) {
            throw new IOException("Short write on " + portName
                    + ": wrote " + n + " of " + length);
        }
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    /**
     * Assert a serial BREAK condition for {@code millis} milliseconds, then
     * release. Used as the fallback path on app exit when {@code HO N} fails
     * (truenorth E3).
     */
    public void sendBreak(int millis) throws IOException {
        requireOpen();
        if (millis < 0) {
            throw new IllegalArgumentException("millis must be >= 0");
        }
        if (!port.setBreak()) {
            throw new IOException("setBreak failed on " + portName);
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            port.clearBreak();
        }
    }

    private void applyTimeouts() {
        int mode = SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING;
        port.setComPortTimeouts(mode, readTimeoutMs, writeTimeoutMs);
    }

    private void requireOpen() throws IOException {
        if (!isOpen()) {
            throw new IOException("port not open: " + portName);
        }
    }
}
