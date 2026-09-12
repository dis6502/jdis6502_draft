package dis6502.core;

/**
 * Direct Java 6 translation of DisassemblyProgressMonitor.h / DisassemblyProgressMonitor.cpp.
 * Drives one disassembly run and reports progress; a Swing UI would subclass this to update a
 * progress dialog and to make IsCancelled() reflect a "Cancel" button.
 *
 * The C++ source reports progress via a global g_Application message bus, which hasn't been
 * translated (see the EquateList/EquateList.java class Javadoc for the same situation). This
 * translation routes SetPass/SetSegmentNumber/SendInfo through Debug.log(...) instead, and
 * still calls through to Disassembly.disassembleInternal() the same way.
 */
public class DisassemblyProgressMonitor {

    protected String pass = "";
    private boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /** Runs a full disassembly and returns the elapsed time in microseconds. */
    public long startDisassembly(Disassembly disassembly) {
        long start = System.nanoTime();
        disassembleInternal(disassembly);
        long stop = System.nanoTime();
        return (stop - start) / 1000L;
    }

    protected void disassembleInternal(Disassembly disassembly) {
        disassembly.disassembleInternal();
    }

    public void setPass(String pass) {
        this.pass = pass;
        Debug.log("DisassemblyProgressMonitor: pass=" + pass);
    }

    public void setSegmentNumber(int segmentNumber) {
        Debug.log("DisassemblyProgressMonitor: segment=" + segmentNumber);
    }

    public void sendInfo(String message) {
        Debug.log("DisassemblyProgressMonitor[" + pass + "]: " + message);
    }

    public boolean isCancelled() {
        return false;
    }
}
