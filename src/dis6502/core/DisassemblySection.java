package dis6502.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of DisassemblySection.h / DisassemblySection.cpp.
 *
 * Simplification note: the C++ source stores lines in a linked list of fixed-size DIS_BUFFER
 * arenas (see DisassemblyLine.java for why); this translation stores them in a plain
 * ArrayList&lt;DisassemblyLine&gt; instead. GetLineCount() and line iteration order are
 * unchanged, which is what Disassembly.java actually depends on -- the arena/paging structure
 * itself was purely a native-list-control memory-layout optimization.
 */
public final class DisassemblySection {

    public static int getIndex(DisassemblySectionType type) {
        switch (type) {
            case SYSTEM_EQUATES:
                return 0;
            case USER_EQUATES:
                return 1;
            case CODE_EQUATES:
                return 2;
            case CODE_LINES:
                return 3;
            default:
                throw new RuntimeException("Invalid disassemblySectionType");
        }
    }

    public static DisassemblySectionType getType(int index) {
        switch (index) {
            case 0:
                return DisassemblySectionType.SYSTEM_EQUATES;
            case 1:
                return DisassemblySectionType.USER_EQUATES;
            case 2:
                return DisassemblySectionType.CODE_EQUATES;
            case 3:
                return DisassemblySectionType.CODE_LINES;
            default:
                throw new RuntimeException("Invalid index");
        }
    }

    public static String getText(DisassemblySectionType type) {
        switch (type) {
            case SYSTEM_EQUATES:
                return "System equates";
            case USER_EQUATES:
                return "User equates";
            case CODE_EQUATES:
                return "Code equates";
            case CODE_LINES:
                return "Start of code";
            default:
                throw new RuntimeException("Undefined disassemblySectionType");
        }
    }

    private final DisassemblyResult disassemblyResult;
    private final DisassemblySectionType disassemblySectionType;
    private final List<DisassemblyLine> lines = new ArrayList<DisassemblyLine>();

    public DisassemblySection(DisassemblyResult disassemblyResult, DisassemblySectionType disassemblySectionType) {
        this.disassemblyResult = disassemblyResult;
        this.disassemblySectionType = disassemblySectionType;
    }

    public DisassemblyResult getDisassemblyResult() {
        return disassemblyResult;
    }

    public DisassemblySectionType getType() {
        return disassemblySectionType;
    }

    public void clearLines() {
        lines.clear();
    }

    public int getLineCount() {
        return lines.size();
    }

    /** Corresponds to appending a new line via a DIS_BUFFER in the C++ source. */
    public DisassemblyLine addLine(DisassemblyLine template_, String text) {
        DisassemblyLine line = new DisassemblyLine(this);
        line.segmentIndex = template_.segmentIndex;
        line.offset = template_.offset;
        line.size = template_.size;
        line.address = template_.address;
        line.systemAddress = template_.systemAddress;
        line.setLine(text);
        lines.add(line);
        return line;
    }

    /** Direct index access, corresponding to DIS_BUFFER::GetDisLine()-style traversal. */
    public DisassemblyLine getLine(int index) {
        return lines.get(index);
    }

    public List<DisassemblyLine> getLines() {
        return lines;
    }
}
