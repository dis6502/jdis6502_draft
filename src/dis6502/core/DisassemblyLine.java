package dis6502.core;

/**
 * Direct Java 6 translation of the DIS_LINE class (DisassemblyLine.h), covering every field
 * and accessor the Disassembly engine itself reads or writes.
 *
 * Simplification note: in the C++ source, DIS_LINE is a fixed-layout struct allocated inside
 * a DIS_BUFFER -- a hand-rolled 32500-byte memory arena (a linked list of such arenas per
 * section) with the line's text stored as a variable-length wchar_t[] immediately following
 * the struct in the same block. That arrangement is a Win32-native-list-control performance
 * optimization with no semantic content of its own; this translation stores the line text as
 * a plain String field on the object instead (see DisassemblySection.java for the
 * corresponding simplification of the buffer-of-buffers into a plain List). Every
 * *observable* field and behavior Disassembly.java depends on is preserved.
 */
public final class DisassemblyLine {

    private final DisassemblySection section;

    public int segmentIndex;      // Segment index where the instruction starts
    public int offset;            // Offset of the instruction in the segment
    public int size;              // Size of the instruction
    public long xrefLineNumber;   // Line number of this line in the XRef search list window (XRef window not yet ported; always 0)
    public boolean selected;      // Displayed in yellow background
    public boolean referenced;    // Displayed in grey if not referenced
    public int address;           // Absolute address to display as comment (in addition to the label)
    public int systemAddress;     // Address is filled only for system equates

    private String text = "";
    private long lineNumber; // 1-based; 0 means not yet assigned

    public DisassemblyLine(DisassemblySection section) {
        this.section = section;
    }

    public DisassemblySection getSection() {
        return section;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLine() {
        return text;
    }

    void setLine(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "segmentIndex=" + segmentIndex + " offset=" + offset + " size=" + size
                + " address=" + Integer.toHexString(address) + " systemAddress=" + Integer.toHexString(systemAddress)
                + " referenced=" + referenced + " line=\"" + text + "\"";
    }
}
