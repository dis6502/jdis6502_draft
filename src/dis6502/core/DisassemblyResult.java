package dis6502.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Direct Java 6 translation of DisassemblyResult.h / DisassemblyResult.cpp: the generated
 * disassembly listing, organized into four ordered sections (see DisassemblySectionType) each
 * holding an ordered list of lines (see DisassemblySection / DisassemblyLine).
 *
 * Line-numbering note: in the C++ source, DIS_LINE::lineNumber is a mutable field lazily
 * (re)assigned as a side effect of iteration -- walking with CreateLineIterator() (all four
 * sections, in order) numbers lines 1..N globally, which is the only usage in Disassembly.cpp
 * that reads GetLineNumber() back (in SelectLine/ExtendSelectionTo). The single-section
 * iterator (CreateLineIterator(sectionType), used by
 * SetSystemEquateLinesReferencedBySystemAddress/SetNearestSystemEquateLineReferencedByAddress/
 * DebugSection) never has its lines' line numbers read back in the C++ source, only
 * `referenced`/`systemAddress`/text -- so this translation only assigns global line numbers
 * when iterating all sections, matching every place the number is actually consumed.
 */
public final class DisassemblyResult {

    private static final List<DisassemblySectionType> SECTION_TYPES;
    static {
        List<DisassemblySectionType> types = new ArrayList<DisassemblySectionType>();
        types.add(DisassemblySectionType.SYSTEM_EQUATES);
        types.add(DisassemblySectionType.USER_EQUATES);
        types.add(DisassemblySectionType.CODE_EQUATES);
        types.add(DisassemblySectionType.CODE_LINES);
        SECTION_TYPES = java.util.Collections.unmodifiableList(types);
    }

    private final DisassemblySection[] sections = new DisassemblySection[SECTION_TYPES.size()];

    public DisassemblyResult() {
    }

    public List<DisassemblySectionType> getSectionTypes() {
        return SECTION_TYPES;
    }

    public int getSectionCount() {
        return sections.length;
    }

    public DisassemblySection getSection(int index) {
        return sections[index];
    }

    public DisassemblySection getSection(DisassemblySectionType type) {
        return getSection(DisassemblySection.getIndex(type));
    }

    /** Allocate a section and add it at the given slot. */
    public DisassemblySection allocSection(DisassemblySectionType type) {
        int index = DisassemblySection.getIndex(type);
        sections[index] = new DisassemblySection(this, type);
        return sections[index];
    }

    /** Clear a section, i.e. free all its lines. */
    public void clearSection(DisassemblySectionType type) {
        sections[DisassemblySection.getIndex(type)] = null;
    }

    public void clearEquateSections() {
        clearSection(DisassemblySectionType.SYSTEM_EQUATES);
        clearSection(DisassemblySectionType.USER_EQUATES);
    }

    public void clear() {
        for (int i = 0; i < sections.length; i++) {
            sections[i] = null;
        }
    }

    public int getLineCount() {
        int lineCount = 0;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null) {
                lineCount += sections[i].getLineCount();
            }
        }
        return lineCount;
    }

    /** Iterates every line across all four sections, in section order, assigning global 1-based line numbers. */
    public Iterator<DisassemblyLine> createLineIterator() {
        return new GlobalLineIterator();
    }

    /** Iterates every line of a single section, in order. Does not (re-)assign line numbers; see the class Javadoc. */
    public Iterator<DisassemblyLine> createLineIterator(DisassemblySectionType type) {
        DisassemblySection section = getSection(type);
        if (section == null) {
            return java.util.Collections.<DisassemblyLine>emptyList().iterator();
        }
        return section.getLines().iterator();
    }

    private final class GlobalLineIterator implements Iterator<DisassemblyLine> {
        private int sectionIndex = 0;
        private int lineIndex = 0;
        private long lineNumber = 0;

        GlobalLineIterator() {
            advanceToNextAvailableLine();
        }

        private void advanceToNextAvailableLine() {
            while (sectionIndex < sections.length
                    && (sections[sectionIndex] == null || lineIndex >= sections[sectionIndex].getLineCount())) {
                sectionIndex++;
                lineIndex = 0;
            }
        }

        public boolean hasNext() {
            return sectionIndex < sections.length;
        }

        public DisassemblyLine next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No next line");
            }
            DisassemblyLine line = sections[sectionIndex].getLine(lineIndex);
            lineIndex++;
            lineNumber++;
            line.setLineNumber(lineNumber);
            advanceToNextAvailableLine();
            return line;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** Direct translation of DisassemblyResult::SelectLine(LineNumber). */
    public DisassemblyLine selectLine(long lineNumber) {
        DisassemblyLine selectedDisLine = null;

        Iterator<DisassemblyLine> i = createLineIterator();
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            if (disLine.getLineNumber() == lineNumber) {
                selectedDisLine = disLine;
                disLine.selected = true;
            } else {
                disLine.selected = false;
            }
        }
        return selectedDisLine;
    }

    /** Direct translation of DisassemblyResult::SelectLine(SEGMENT_INDEX, Memory::offset). */
    public long selectLine(int segmentIndex, int offset) {
        long selectedLineNumber = 0;

        Iterator<DisassemblyLine> i = createLineIterator();
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            if ((selectedLineNumber == 0) && (disLine.segmentIndex == segmentIndex) && (disLine.offset == offset)) {
                selectedLineNumber = disLine.getLineNumber();
                disLine.selected = true;
            } else {
                disLine.selected = false;
            }
        }

        return selectedLineNumber;
    }

    /** Direct translation of DisassemblyResult::ExtendSelectionTo. */
    public boolean extendSelectionTo(int segmentIndex, int offset) {
        long selectedLineNumber = 0;

        Iterator<DisassemblyLine> i = createLineIterator();
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            if ((disLine.segmentIndex == segmentIndex) && (disLine.offset == offset)) {
                selectedLineNumber = disLine.getLineNumber();
            }
        }

        if (selectedLineNumber != 0) {
            boolean selected = false;
            Iterator<DisassemblyLine> j = createLineIterator();
            while (j.hasNext()) {
                DisassemblyLine disLine = j.next();
                if (disLine.getLineNumber() > selectedLineNumber) {
                    break;
                }
                if (disLine.selected) {
                    selected = true;
                }
                disLine.selected = selected;
            }
            return true;
        }
        return false;
    }

    /** Result holder for the "LineNumber& findFirstLineNumber" in/out parameter. */
    public static final class FindResult {
        public long findFirstLineNumber;
    }

    /** Direct translation of DisassemblyResult::FindAndSelectLines. */
    public boolean findAndSelectLines(boolean first, FindResult result, String findString) {
        boolean found = false;
        long xrefLineNumber = 1;

        Iterator<DisassemblyLine> i = createLineIterator();
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();

            // Reset selection flag on the current line.
            disLine.selected = false;

            if (first) {
                disLine.xrefLineNumber = 0;

                // Do we have the substring in the line?
                if (disLine.getLine().indexOf(findString) >= 0) {
                    // This is the good line. Mark it as selected.
                    disLine.xrefLineNumber = xrefLineNumber++;

                    // Keep the first line for the next search operation.
                    if (result.findFirstLineNumber == 0) {
                        disLine.selected = true;
                        result.findFirstLineNumber = disLine.getLineNumber();
                    }

                    // Continue all the loop to reset other selection flags on other lines.
                    found = true;
                }
            } else {
                // Ignore all the lines before the line found in a previous search and after the line has been found.
                if ((disLine.getLineNumber() >= result.findFirstLineNumber) && (!found)) {
                    // Do we have the substring in the line?
                    if (disLine.getLine().indexOf(findString) >= 0) {
                        // This is the good line. Mark it as selected.
                        disLine.selected = true;
                        // Keep the first line for the next search operation.
                        result.findFirstLineNumber = disLine.getLineNumber();
                        // Continue all the loop to reset other selection flags on other lines.
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    /** Result holder for the "offset, size" out-parameters. */
    public static final class OffsetAtStartOfInstructionResult {
        public int offset;
        public int size;
    }

    /** Direct translation of DisassemblyResult::FindOffsetAtStartOfInstruction. */
    public void findOffsetAtStartOfInstruction(int segmentIndex, OffsetAtStartOfInstructionResult result) {
        result.offset = 0;
        result.size = 0;

        Iterator<DisassemblyLine> i = createLineIterator(DisassemblySectionType.CODE_LINES);
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();

            if ((disLine.segmentIndex == segmentIndex)
                    && (disLine.size > 0)
                    && (disLine.offset <= result.offset)
                    && ((disLine.offset + disLine.size) > result.offset)) {
                result.offset = disLine.offset;
                result.size = disLine.size;
                return;
            }
        }
    }
}
