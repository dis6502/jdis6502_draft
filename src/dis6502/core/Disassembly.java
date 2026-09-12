package dis6502.core;

import java.util.Iterator;
import java.util.List;

/**
 * Direct Java 6 translation of Disassembly.h / Disassembly.cpp: runs one full disassembly of
 * the current Workspace's segments and populates its DisassemblyResult.
 *
 * The C++ source's macros (DIS_GET_NEXT_BYTE, DIS_GET_BYTE_IN_PASS_2/4, DIS_GET_WORD_IN_PASS_2/4)
 * rely on plain C `break`/`return` inside the macro body, which C's textual macro substitution
 * makes break/return out of whatever switch/loop/method textually encloses that particular call
 * site. Java's break/return have identical scoping rules for switch statements and loops, so
 * every macro call site below is inlined directly (the same `if (...) break;` /
 * `if (isCancelled()) return;` the macro would have produced) rather than factored into one
 * shared method -- Java has no macros, and a shared helper method cannot break out of its
 * caller's switch/loop, so the repetition here intentionally mirrors the C++ source's own
 * macro-expansion repetition instead of trying to paper over it.
 *
 * Package-private fields (absoluteAddress, markSize, lineWriter) are reached into directly by
 * DisassemblyWriter, mirroring the C++ source's "friend class DisassemblyWriter" declaration.
 *
 * A couple of faithfully-preserved oddities from the C++ source, flagged inline where they
 * occur: ReservedNop2Byte/ReservedNop3Byte in Pass4 render a stale `wAddr` instead of the byte
 * they just read (looks like a copy/paste bug upstream), and GenerateUserComment is called with
 * a segment index captured *after* the opcode byte read (which may have crossed a segment
 * boundary) paired with the PC captured *before* it.
 */
public final class Disassembly {

    private static final int NO_DUMP = 0xFFFF; // dis_k::NO_DUMP in Syntax.h

    // Note: the C++ header also declares a "byte returnCharacter;" field, but it is never read
    // or written anywhere in Disassembly.cpp -- dead code, dropped here.

    private Workspace workspace;
    private Profile profile;
    private DisassemblyResult result;

    private DisassemblyProgressMonitor disassemblyProgressMonitor;
    private int pass;

    private MemoryBlockIterator memoryBlockIterator;
    private boolean bDisNewSegment;

    private final DisassemblyOpcodeBuffer opcodeBuffer = new DisassemblyOpcodeBuffer();

    private int markNextSegmentIndex;
    private int markSegmentIndex;
    private int markOffset;
    int markSize; // package-private: read/written directly by DisassemblyWriter

    final DisassemblyLineWriter lineWriter = new DisassemblyLineWriter(); // package-private: read directly by DisassemblyWriter

    int absoluteAddress = 0; // package-private: written directly by DisassemblyWriter
    private int systemAddress = 0x1234;

    public Disassembly() {
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
        this.profile = workspace.getConstProfile();
        this.result = workspace.getDisassemblyResult();
    }

    public void setProgressMonitor(DisassemblyProgressMonitor disassemblyProgressMonitor) {
        this.disassemblyProgressMonitor = disassemblyProgressMonitor;
    }

    public void startDisassembly() {
        disassemblyProgressMonitor.startDisassembly(this);
    }

    // --- Higher level operations. ---

    /** Add a line into a buffer. */
    private void addLineInBuffer(DisassemblyLine template_, String szLine, DisassemblySection section) {
        section.addLine(template_, szLine);

        // Transition to a different segment.
        if (markSegmentIndex != markNextSegmentIndex) {
            if (markSegmentIndex == SegmentList.NO_SEGMENT_INDEX) {
                markOffset = 0;
                markSegmentIndex = markNextSegmentIndex;
            } else {
                Segment markSegment = workspace.getConstSegment(markSegmentIndex);
                int markSegmentSize = markSegment.getSize();

                if (markOffset + markSize >= markSegmentSize) {
                    if ((markNextSegmentIndex != SegmentList.NO_SEGMENT_INDEX) && (markSegmentSize > 0)) {
                        markOffset = markOffset + markSize - markSegmentSize;
                    } else {
                        markOffset = NO_DUMP;
                    }
                    markSegmentIndex = markNextSegmentIndex;
                } else {
                    markOffset += markSize;
                }
            }
        } else {
            markOffset += markSize;
        }

        markSize = 0;
    }

    void addEmptyCommentLine() {
        addEmptyCommentLine(DisassemblySectionType.CODE_LINES);
    }

    void addEmptyCommentLine(DisassemblySectionType type) {
        addLine(profile.commentPrefix, type);
    }

    /**
     * Add a line into a buffer. The following state fields are used: markSegmentIndex,
     * markOffset, markSize, absoluteAddress, systemAddress.
     */
    void addLine(String szLine) {
        addLine(szLine, DisassemblySectionType.CODE_LINES);
    }

    void addLine(String szLine, DisassemblySectionType disassemblySectionType) {
        // Ensure this method is only used in Pass4/5/6.
        if (pass != 4 && pass != 5 && pass != 6) {
            throw new RuntimeException("addLine() must only be called in pass 4/5/6.");
        }

        // Allocate a section if not already done.
        boolean newSection = false;
        DisassemblySection section = result.getSection(disassemblySectionType);
        if (section == null) {
            newSection = true;
            section = result.allocSection(disassemblySectionType);
        }

        DisassemblyLine template_ = new DisassemblyLine(section);
        template_.segmentIndex = markSegmentIndex;
        template_.offset = markOffset;
        template_.size = markSize;
        template_.address = absoluteAddress;
        template_.systemAddress = systemAddress;

        if (markSegmentIndex != SegmentList.NO_SEGMENT_INDEX) {
            Segment markSegment = workspace.getConstSegment(markSegmentIndex);
            int markSegmentSize = markSegment.getSize();
            if (markOffset + markSize > markSegmentSize) {
                template_.size = markSegmentSize - markOffset;
            }
        }

        if (newSection) {
            // Add the comment header.
            int oldAbsoluteAddress = absoluteAddress;
            int oldSystemAddress = systemAddress;
            systemAddress = 0;
            String text = DisassemblySection.getText(disassemblySectionType);

            if (disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES
                    || disassemblySectionType == DisassemblySectionType.USER_EQUATES
                    || disassemblySectionType == DisassemblySectionType.CODE_EQUATES) {
                markNextSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
                markSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
                markOffset = 0xFFFF;
                markSize = 0;
            }

            addLineInBuffer(template_, profile.commentPrefix, section);
            addLineInBuffer(template_, profile.commentPrefix + " " + text, section);
            addLineInBuffer(template_, profile.commentPrefix, section);

            absoluteAddress = oldAbsoluteAddress;
            systemAddress = oldSystemAddress;
        }

        addLineInBuffer(template_, szLine, section);
    }

    void addLineWriter() {
        addLineWriter(DisassemblySectionType.CODE_LINES);
    }

    void addLineWriter(DisassemblySectionType type) {
        addLine(lineWriter.getLineBuffer(), type);
    }

    /** Add a label definition into a buffer. */
    private void addLabelWithAddress(String label, int wAddr, DisassemblySectionType disassemblySectionType, int wSystemAddr, String comment) {
        lineWriter.clear().string(label).alignInstructions().string(profile.directiveEQU).space().address(wAddr);

        if (comment.length() != 0) {
            lineWriter.comment(comment);
        }

        systemAddress = disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES ? wSystemAddr : 0;

        addLineWriter(disassemblySectionType);
    }

    private void addLabel(String label, int wAddr, DisassemblySectionType disassemblySectionType, String comment) {
        addLabelWithAddress(label, wAddr, disassemblySectionType, wAddr, comment);
    }

    /** Add a comment line to a buffer. */
    private void addComment(String comment, DisassemblySectionType disassemblySectionType) {
        lineWriter.clear().comment(comment);

        systemAddress = disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES ? 0xFFFF : 0;
        addLineWriter(disassemblySectionType);
    }

    private void generateUserComment(int segmentIndex, int offset, int size) {
        Segment segment = workspace.getConstSegment(segmentIndex);

        int endOffset = offset + size;
        while (offset < endOffset) {
            String comment = segment.findComment(offset);
            if (comment.length() != 0) {
                addUserComment(comment, DisassemblySectionType.CODE_LINES);
            }
            offset++;
        }
    }

    /** Add a user comment into a buffer. The comment is split into several lines if needed. */
    private void addUserComment(String comment, DisassemblySectionType disassemblySectionType) {
        int oldMarkNextSegmentIndex = markNextSegmentIndex;
        int oldMarkSegmentIndex = markSegmentIndex;
        int oldMarkOffset = markOffset;
        int oldMarkSize = markSize;

        // Split on line breaks (handling "\r\n" the same as a plain "\n"), same as the C++
        // source's manual character scan.
        String normalized = comment.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String fullText = profile.commentPrefix + " " + lines[i];
            markSize = 0;
            systemAddress = 0;
            addLine(fullText, disassemblySectionType);
        }

        markNextSegmentIndex = oldMarkNextSegmentIndex;
        markSegmentIndex = oldMarkSegmentIndex;
        markOffset = oldMarkOffset;
        markSize = oldMarkSize;
    }

    /**
     * Save the position of a new segment in the disassembly listing. Line 1 corresponds to the
     * first line of code.
     */
    private void setSegmentFirstLineNumber(int segmentIndex) {
        int lineCount = 0;
        DisassemblySection section = result.getSection(DisassemblySectionType.CODE_LINES);
        if (section != null) {
            lineCount += section.getLineCount();
        }

        workspace.getSegmentList().getSegment(segmentIndex).setFirstLineNumber(lineCount + 1);
    }

    /**
     * Adjust the position of all the segments to the absolute line number in the disassembly
     * listing. setSegmentFirstLineNumber() saved a line number relative to start of code. We
     * have to add to every segment position the number of lines used by labels.
     */
    private void adjustSegmentFirstLineNumber() {
        int equatesLineCount = 0;
        DisassemblySectionType[] sectionTypes = {
                DisassemblySectionType.SYSTEM_EQUATES, DisassemblySectionType.USER_EQUATES, DisassemblySectionType.CODE_EQUATES
        };
        for (int i = 0; i < sectionTypes.length; i++) {
            DisassemblySection section = result.getSection(sectionTypes[i]);
            if (section != null) {
                equatesLineCount += section.getLineCount();
            }
        }

        SegmentList segmentList = workspace.getSegmentList();
        // Plus the equates for the code section.
        // Plus 3 for the "; Start of code ..." prelude.
        long offset = equatesLineCount + 3;
        for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
            Segment segment = segmentList.getSegment(segmentIndex);

            long localLineNumber = segment.getFirstLineNumber();
            long globalLineNumber = localLineNumber + offset;
            segment.setFirstLineNumber(globalLineNumber);
        }
    }

    private static LabelAccess combineAccess(LabelAccess a, LabelAccess b) {
        int combined = a.getValue() | b.getValue();
        LabelAccess[] all = LabelAccess.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getValue() == combined) {
                return all[i];
            }
        }
        return LabelAccess.UNKNOWN;
    }

    /** Set the referenced flag in lines for SYSTEM_EQUATES without offset (sta LABEL). */
    private void setSystemEquateLinesReferencedBySystemAddress() {
        // TODO (same as the C++ source): currently the referenced check does not distinguish
        // the type of access. Therefore if $80 is referenced, ZP and DL constants are equally
        // referenced.
        LabelAccess anyAccess = combineAccess(LabelAccess.IMMEDIATE, LabelAccess.READ_WRITE);

        EquateList systemEquateList = workspace.getSystemEquateList();
        Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.SYSTEM_EQUATES);
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            disLine.referenced = systemEquateList.isEquateAddressReferenced(disLine.systemAddress, anyAccess);
        }
    }

    /** Set the reference flag in lines for SYSTEM_EQUATES with an address (sta LABEL+n). */
    private void setNearestSystemEquateLineReferencedByAddress(int address) {
        DisassemblyLine nearestLine = null;

        Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.SYSTEM_EQUATES);
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            if ((disLine.systemAddress <= address) && ((nearestLine == null) || (nearestLine.systemAddress < disLine.systemAddress))) {
                nearestLine = disLine;
            }
        }

        if (nearestLine != null) {
            nearestLine.referenced = true;
        }
    }

    private void debugSection(DisassemblySectionType disassemblySectionType) {
        Debug.log("DumpSectionToDebug: disassemblySectionType=" + disassemblySectionType);
        long count = 0;
        long referencedCount = 0;
        Iterator<DisassemblyLine> i = result.createLineIterator(disassemblySectionType);
        while (i.hasNext()) {
            DisassemblyLine disLine = i.next();
            Debug.log(disLine.toString());
            if (disLine.referenced) {
                referencedCount++;
            }
            count++;
        }
        Debug.logValue("count", String.valueOf(count));
        Debug.logValue("referencedCount", String.valueOf(referencedCount));
    }

    /** Add all symbol definitions in the CODE_EQUATES section for SDX binaries. */
    private void generateSDXSymbolDefinitions() {
        SegmentList segmentList = workspace.getConstSegmentList();
        for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
            Segment segment = segmentList.getConstSegment(segmentIndex);
            if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED)) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(segment.szSDXSymbol);

                if (profile.alignInstructions) {
                    for (int i = 12 - segment.szSDXSymbol.length(); i > 0; i--) {
                        buffer.append(" ");
                    }
                } else {
                    buffer.append(" ");
                }

                buffer.append(profile.showLowerCaseInstructions ? "smb" : "SMB").append(" ")
                        .append(profile.quoteForASCIIStrings).append(segment.szSDXSymbol).append(profile.quoteForASCIIStrings);

                addLine(buffer.toString(), DisassemblySectionType.CODE_EQUATES);
            }
        }
    }

    /** Give the user a chance to click on "Cancel" button. */
    private boolean isCancelled() {
        if (disassemblyProgressMonitor.isCancelled()) {
            result.clear();
            return true;
        }
        return false;
    }

    private void createMemoryBlockIterator(Segment segment) {
        memoryBlockIterator = new MemoryBlockIterator(segment.memoryBlock);
    }

    private void clearMemoryBlockIterator() {
        memoryBlockIterator = null;
    }

    /** Mutable cursor shared by Pass2/Pass3/Pass4, corresponding to the C++ macros' by-reference (segmentIndex, pc) parameters. */
    private final class ByteCursor {
        int segmentIndex;
        int pc;
        int value;             // The byte just read (bByte / cLow, depending on call site)
        MemoryType memoryType; // cDisByteType

        /** Corresponds to Disassembly::DisGetNextByte. */
        void readNext() {
            value = memoryBlockIterator.getData();
            memoryType = memoryBlockIterator.getType();
            memoryBlockIterator.next();
            markSize++;
            opcodeBuffer.saveLastOpcode(value);

            if (disassemblyProgressMonitor.isVerbose()) {
                disassemblyProgressMonitor.sendInfo("segmentIndex=" + segmentIndex + ", wPC=" + Memory.addressToHexString(pc)
                        + ", bByte=" + Memory.byteToHexString(value) + ", memoryType=" + Memory.byteToHexString(memoryType.toByte()));
            }

            // End of segment reached?
            if (!memoryBlockIterator.hasNext()) {
                SegmentList segmentList = workspace.getConstSegmentList();
                Segment segment = segmentList.getConstSegment(segmentIndex);
                int savedSegmentIndex = segmentIndex;
                int lastEndAddress = segment.wEnd;
                FileHeader lastHeader = segment.getHeader();

                clearMemoryBlockIterator();

                do {
                    segmentIndex++;
                    if (segmentIndex >= segmentList.getCount()) {
                        clearMemoryBlockIterator();
                        segmentIndex = savedSegmentIndex;
                        pc++; // TODO (same as the C++ source): why?
                        break;
                    }

                    segment = segmentList.getConstSegment(segmentIndex);
                    if (segment.bBinary && !segment.isEmpty()) {
                        disassemblyProgressMonitor.setSegmentNumber(segmentIndex + 1);

                        createMemoryBlockIterator(segment);
                        pc = segment.wBegin;

                        if ((lastEndAddress + 1 != pc)
                                || (!segment.isHeader(lastHeader))
                                || (!segment.isHeader(FileHeader.ATARI_BINARY) && !segment.isHeader(FileHeader.SDX_FIXED_BLK))) {
                            bDisNewSegment = true;
                        }

                        setSegmentFirstLineNumber(segmentIndex); // TODO (same as the C++ source): why is this called here/so often?
                        markNextSegmentIndex = segmentIndex;
                    }
                } while (memoryBlockIterator == null);
            } else {
                pc++;
            }
        }
    }

    /**
     * Corresponds to the DIS_GET_NEXT_BYTE(bByte, segmentIndex, wPC) macro: reads the next
     * byte into cursor and returns true if the disassembly was cancelled meanwhile (callers
     * must then `return;` immediately, exactly like the macro's `if (IsCancelled()) return;`).
     */
    private boolean getNextByte(ByteCursor cursor) {
        cursor.readNext();
        return isCancelled();
    }

    /** Initialize global variables to start of code. */
    private boolean disInit(ByteCursor cursor) {
        cursor.pc = 0;
        absoluteAddress = 0;
        systemAddress = 0;

        bDisNewSegment = true;
        memoryBlockIterator = null;

        SegmentList segmentList = workspace.getConstSegmentList();

        for (cursor.segmentIndex = 0; cursor.segmentIndex < segmentList.getCount(); cursor.segmentIndex++) {
            Segment segment = segmentList.getConstSegment(cursor.segmentIndex);

            if (segment.bBinary && segment.getSize() > 0) {
                memoryBlockIterator = new MemoryBlockIterator(segment.memoryBlock);
                cursor.pc = segment.wBegin;
                bDisNewSegment = true;
                setSegmentFirstLineNumber(cursor.segmentIndex);
                markNextSegmentIndex = cursor.segmentIndex;
                disassemblyProgressMonitor.setSegmentNumber(cursor.segmentIndex + 1);
                return true;
            }
        }

        return false;
    }

    /** Result holder for isInstructionWithImmediate's out-parameters. */
    public static final class ImmediateResult {
        public int immediateValue;
        public MemoryType immediateMemoryType = MemoryType.UNKNOWN;
    }

    public static boolean isInstructionWithImmediate(Workspace workspace, int segmentIndex, int offset, ImmediateResult result) {
        result.immediateValue = 0;
        result.immediateMemoryType = MemoryType.UNKNOWN;

        SegmentList segmentList = workspace.getConstSegmentList();
        Segment opcodeSegment = segmentList.getConstSegment(segmentIndex);

        // The code below only depends on the segment and the offset.
        if (opcodeSegment.bBinary && !opcodeSegment.isEmpty()) {
            int opc = opcodeSegment.memoryBlock.getDataAt(offset);
            InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

            if (instructionSet.getInstruction(opc).getOperandMode() == OperandMode.Immediate) {
                result.immediateValue = opcodeSegment.memoryBlock.getDataAt(offset + 1);

                // The type of the immediate opcode before the known byte is set to
                // MemoryType.LOBYTE or MemoryType.HIBYTE. The user input for the unknown byte
                // in the assumed word is stored in the type of the operand.
                result.immediateMemoryType = opcodeSegment.getType(offset);
                if (result.immediateMemoryType != MemoryType.LOBYTE && result.immediateMemoryType != MemoryType.HIBYTE) {
                    result.immediateMemoryType = opcodeSegment.getType(offset + 1);
                }
                return true;
            }
        }
        return false;
    }

    void addOrgOrBlock(int segmentIndex, int wPC) {
        SegmentList segmentList = workspace.getSegmentList();
        Segment segment = segmentList.getConstSegment(segmentIndex);

        FileHeader header = segment.getHeader();
        if (header == FileHeader.SDX_FIXED_BLK) {
            lineWriter.cstring("blk sparta").space().address(wPC);

        } else if (header == FileHeader.SDX_RELOC_BLK) {
            String memoryType = segment.getSDXMemoryType();
            if (segment.isSDXRelocBlkWithData()) {
                lineWriter.cstring("blk reloc");
            } else {
                lineWriter.cstring("blk empty").space().number(segment.getSize());
            }
            lineWriter.space().string(memoryType).space().string(profile.commentPrefix).cstring(" num: ")
                    .byteNumber(segment.bSDXBlockNumber).cstring(" mem: ").byteNumber(segment.bSDXControlByte);

        } else if (header == FileHeader.SDX_FIX_UP_BLK) {
            lineWriter.cstring("blk update addresses");

        } else if (header == FileHeader.SDX_SYM_REQUIRED) {
            lineWriter.cstring("blk update symbols");

        } else if (header == FileHeader.SDX_SYM_DEFINED) {
            Equate equate = workspace.getUserEquateList().findEquateByAddress(segment.wBegin, LabelAccess.READ, true);
            String label;
            if (equate == null) {
                Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);
                label = Segment.formatDefaultLabel(segmentList.getSegmentIndex(labelSegment), segment.wBegin);
            } else {
                label = equate.getLabel();
            }
            lineWriter.cstring("blk update new").space().string(label).space().charValue('\'').string(segment.szSDXSymbol).charValue('\'');

        } else {
            lineWriter.string(profile.directiveORG).space().address(wPC);
        }
    }

    // --- Pass 2: reserve all labels. ---

    private void pass2() {
        ByteCursor cursor = new ByteCursor();

        if (!disInit(cursor)) {
            return;
        }

        SegmentList segmentList = workspace.getSegmentList();

        while (memoryBlockIterator != null) {
            bDisNewSegment = false;
            int oldSegmentIndex = cursor.segmentIndex;
            int wOldPC = cursor.pc;
            Segment opcodeSegment = segmentList.getConstSegment(cursor.segmentIndex);
            InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

            if (getNextByte(cursor)) {
                return;
            }
            int bByte = cursor.value;
            MemoryType cDisByteType = cursor.memoryType;

            if (cDisByteType == MemoryType.BYTE || cDisByteType == MemoryType.STRING
                    || cDisByteType == MemoryType.SBYTE || cDisByteType == MemoryType.STORE) {
                // No label: this is a byte.

            } else if (cDisByteType == MemoryType.WORD || cDisByteType == MemoryType.SYMBOL) {
                // No label: this is a word.
                if (bDisNewSegment || memoryBlockIterator == null) {
                    continue;
                }
                if (getNextByte(cursor)) {
                    return;
                }

            } else if (cDisByteType == MemoryType.FIXUP || cDisByteType == MemoryType.LABEL) {
                // This is an address fix-up, or a label.
                if (bDisNewSegment || memoryBlockIterator == null) {
                    continue;
                }
                if (getNextByte(cursor)) {
                    return;
                }
                int wAddr = Memory.toAddress(bByte, cursor.value);
                segmentList.allocateAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, OperandMode.Accumulator, LabelAccess.READ);

            } else if (cDisByteType == MemoryType.DLIST) {
                // There may be a label: this is the display list.
                switch (bByte & 0x0F) {
                    case 0:
                        // Empty lines.
                        break;
                    case 1: {
                        // Jump.
                        int jSegmentIndex = cursor.segmentIndex;
                        int jPC = cursor.pc;
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        int cL1 = cursor.value;
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        int wAddr1 = Memory.toAddress(cL1, cursor.value);
                        segmentList.allocateAddress(jSegmentIndex, jPC, wAddr1, cDisByteType, OperandMode.Accumulator, LabelAccess.READ);
                        break;
                    }
                    default:
                        // Graphic lines: load memory scan.
                        if ((bByte & 0x40) != 0) {
                            int gSegmentIndex = cursor.segmentIndex;
                            int gPC = cursor.pc;
                            if (bDisNewSegment || memoryBlockIterator == null) {
                                break;
                            }
                            if (getNextByte(cursor)) {
                                return;
                            }
                            int cL2 = cursor.value;
                            if (bDisNewSegment || memoryBlockIterator == null) {
                                break;
                            }
                            if (getNextByte(cursor)) {
                                return;
                            }
                            int wAddr2 = Memory.toAddress(cL2, cursor.value);
                            segmentList.allocateAddress(gSegmentIndex, gPC, wAddr2, cDisByteType, OperandMode.Accumulator, LabelAccess.READ);
                        }
                        break;
                }

            } else if (cDisByteType == MemoryType.LOBYTE || cDisByteType == MemoryType.HIBYTE
                    || cDisByteType == MemoryType.UNKNOWN || cDisByteType == MemoryType.CODE) {
                // Code.
                if (instructionSet.getInstruction(bByte).isUnsupportedInstruction() && !profile.useIllegalOpcodes) {
                    continue;
                }

                OperandMode mode = instructionSet.getInstruction(bByte).getOperandMode();

                switch (mode) {
                    case Accumulator:
                        // No additional operands.
                        break;

                    case Absolute:
                    case Indirect:
                    case AbsoluteX:
                    case AbsoluteY:
                    case IndexedIndirectAbsolute: {
                        int aSegmentIndex = cursor.segmentIndex;
                        int aPC = cursor.pc;
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        int cL = cursor.value;
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        int wAddr = Memory.toAddress(cL, cursor.value);
                        segmentList.allocateAddress(aSegmentIndex, aPC, wAddr, cDisByteType, instructionSet.getInstruction(bByte));
                        break;
                    }

                    case ZeroPageX:
                    case ZeroPageY:
                    case ZeroPage:
                    case IndexedIndirect:
                    case IndirectIndexed:
                    case ZeroPageIndirect:
                    case ZeroPageRelative:
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        segmentList.allocateAddress(oldSegmentIndex, wOldPC, cursor.value, cDisByteType, instructionSet.getInstruction(bByte));
                        break;

                    case Relative: {
                        int rSegmentIndex = cursor.segmentIndex;
                        int rOldPC = cursor.pc;
                        int wAddr = cursor.pc + 1;
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        if (cursor.value > 127) {
                            wAddr += cursor.value - 256;
                        } else {
                            wAddr += cursor.value;
                        }
                        segmentList.allocateAddress(rSegmentIndex, rOldPC, wAddr & 0xFFFF, cDisByteType, instructionSet.getInstruction(bByte));
                        break;
                    }

                    case Immediate:
                        if (cDisByteType == MemoryType.LOBYTE) {
                            if (bDisNewSegment || memoryBlockIterator == null) {
                                break;
                            }
                            if (getNextByte(cursor)) {
                                return;
                            }
                            int wAddr = Memory.toAddress(cursor.value, cDisByteType.toByte());
                            segmentList.allocateAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, instructionSet.getInstruction(bByte));
                        } else if (cDisByteType == MemoryType.HIBYTE) {
                            if (bDisNewSegment || memoryBlockIterator == null) {
                                break;
                            }
                            if (getNextByte(cursor)) {
                                return;
                            }
                            int wAddr = Memory.toAddress(cDisByteType.toByte(), cursor.value);
                            segmentList.allocateAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, instructionSet.getInstruction(bByte));
                        } else {
                            if (bDisNewSegment || memoryBlockIterator == null) {
                                break;
                            }
                            if (getNextByte(cursor)) {
                                return;
                            }
                        }
                        break;

                    case Implied:
                    case ReservedNop1Byte:
                        // No additional operands.
                        break;

                    case ReservedNop2Byte:
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        break;

                    case ReservedNop3Byte:
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByte(cursor)) {
                            return;
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    // --- Pass 3: update labels to generate relative labels (L2222+1 instead of L2223 if L2223 is inside an instruction). ---

    private void pass3() {
        ByteCursor cursor = new ByteCursor();
        SegmentList segmentList = workspace.getSegmentList();

        // Determine which label address is at the start of an instruction (aligned).
        if (!disInit(cursor)) {
            return;
        }

        while (memoryBlockIterator != null) {
            Segment opcodeSegment = segmentList.getSegment(cursor.segmentIndex);
            InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);
            bDisNewSegment = false;
            opcodeSegment.defineAddressLabel(cursor.pc);
            if (getNextByte(cursor)) {
                return;
            }
            int bByte = cursor.value;
            MemoryType cDisByteType = cursor.memoryType;

            pass3Body(cursor, bByte, cDisByteType, instructionSet);
        }

        // For those non-aligned label addresses, find the immediate previous one.
        segmentList.alignRamBlkLabelAddresses();
        if (!disInit(cursor)) {
            return;
        }

        while (memoryBlockIterator != null) {
            Segment opcodeSegment = segmentList.getSegment(cursor.segmentIndex);
            InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

            bDisNewSegment = false;
            opcodeSegment.alignAddressLabels(cursor.pc);
            if (getNextByte(cursor)) {
                return;
            }
            int bByte = cursor.value;
            MemoryType cDisByteType = cursor.memoryType;

            pass3Body(cursor, bByte, cDisByteType, instructionSet);
        }
    }

    /** Shared byte-walking body of Pass3's two near-identical loops (see pass3()). */
    private void pass3Body(ByteCursor cursor, int bByte, MemoryType cDisByteType, InstructionSet instructionSet) {
        if (cDisByteType == MemoryType.BYTE || cDisByteType == MemoryType.STRING
                || cDisByteType == MemoryType.SBYTE || cDisByteType == MemoryType.STORE) {
            // No label: this is a byte.

        } else if (cDisByteType == MemoryType.WORD || cDisByteType == MemoryType.FIXUP
                || cDisByteType == MemoryType.SYMBOL || cDisByteType == MemoryType.LABEL) {
            // No label: this is a word.
            if (bDisNewSegment || memoryBlockIterator == null) {
                return;
            }
            getNextByteIgnoringCancellation(cursor);

        } else if (cDisByteType == MemoryType.DLIST) {
            switch (bByte & 0x0F) {
                case 0:
                    break;
                case 1:
                    if (bDisNewSegment || memoryBlockIterator == null) {
                        break;
                    }
                    if (getNextByteIgnoringCancellation(cursor)) {
                        return;
                    }
                    if (bDisNewSegment || memoryBlockIterator == null) {
                        break;
                    }
                    getNextByteIgnoringCancellation(cursor);
                    break;
                default:
                    if ((bByte & 0x40) != 0) {
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        if (getNextByteIgnoringCancellation(cursor)) {
                            return;
                        }
                        if (bDisNewSegment || memoryBlockIterator == null) {
                            break;
                        }
                        getNextByteIgnoringCancellation(cursor);
                    }
                    break;
            }

        } else if (cDisByteType == MemoryType.LOBYTE || cDisByteType == MemoryType.HIBYTE
                || cDisByteType == MemoryType.UNKNOWN || cDisByteType == MemoryType.CODE) {
            if (instructionSet.getInstruction(bByte).isUnsupportedInstruction() && !profile.useIllegalOpcodes) {
                return;
            }

            OperandMode mode = instructionSet.getInstruction(bByte).getOperandMode();
            switch (mode) {
                case Accumulator:
                case Implied:
                case ReservedNop1Byte:
                    break;

                case Absolute:
                case Indirect:
                case AbsoluteX:
                case AbsoluteY:
                case IndexedIndirectAbsolute:
                case ReservedNop3Byte:
                    if (bDisNewSegment || memoryBlockIterator == null) {
                        break;
                    }
                    if (getNextByteIgnoringCancellation(cursor)) {
                        return;
                    }
                    if (bDisNewSegment || memoryBlockIterator == null) {
                        break;
                    }
                    getNextByteIgnoringCancellation(cursor);
                    break;

                case ZeroPageX:
                case ZeroPageY:
                case ZeroPage:
                case IndexedIndirect:
                case IndirectIndexed:
                case Relative:
                case Immediate:
                case ZeroPageIndirect:
                case ZeroPageRelative:
                case ReservedNop2Byte:
                    if (bDisNewSegment || memoryBlockIterator == null) {
                        break;
                    }
                    getNextByteIgnoringCancellation(cursor);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Pass3-specific variant of getNextByte(): Pass3 doesn't use its read bytes for anything
     * (only for advancing the cursor/segment state), but it must still stop immediately if the
     * disassembly was cancelled, same as every other DIS_GET_NEXT_BYTE call site.
     */
    private boolean getNextByteIgnoringCancellation(ByteCursor cursor) {
        cursor.readNext();
        return isCancelled();
    }

    // --- Pass 4: generate listing. ---

    /** Returns true if a segment boundary is about to be hit (matches the DIS_GET_*_IN_PASS_4 macros' guard). */
    private boolean pass4BoundaryHit() {
        return bDisNewSegment || memoryBlockIterator == null;
    }

    private void pass4() {
        ByteCursor cursor = new ByteCursor();
        int bByte;
        int cLow = 0;
        int wAddr = 0;
        int wOldPC = 0;
        boolean bAnticLabel = false;

        SegmentList segmentList = workspace.getSegmentList();
        DisassemblyWriter disassemblyWriter = new DisassemblyWriter(this, workspace);

        markSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
        markNextSegmentIndex = SegmentList.NO_SEGMENT_INDEX;

        if (!disInit(cursor)) {
            return;
        }

        bAnticLabel = false;
        while (memoryBlockIterator != null) {
            if (bDisNewSegment) {
                bDisNewSegment = false;
                lineWriter.clear();
                disassemblyWriter.flushAndAddLine(lineWriter, 0);

                lineWriter.clear().alignInstructions();
                addOrgOrBlock(cursor.segmentIndex, cursor.pc);
                addLineWriter();
                addEmptyCommentLine();
            }

            lineWriter.clear();
            SegmentList.DefinedLabelResult labelResult = new SegmentList.DefinedLabelResult();
            segmentList.defineLabelAtAddress(cursor.segmentIndex, cursor.pc, labelResult);

            if (labelResult.defined && !Equate.isLabelWithOffset(labelResult.label)) {
                disassemblyWriter.flushBytes();
                lineWriter.string(labelResult.label);

                if (profile.showColonAfterLabel) {
                    lineWriter.charValue(':');
                }
            }
            lineWriter.alignInstructions();

            int oldSegmentIndex = cursor.segmentIndex;
            wOldPC = cursor.pc;
            Segment opcodeSegment = segmentList.getConstSegment(cursor.segmentIndex);
            InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

            if (getNextByte(cursor)) {
                return;
            }
            bByte = cursor.value;
            MemoryType cDisByteType = cursor.memoryType;

            generateUserComment(oldSegmentIndex, wOldPC - workspace.getConstSegment(cursor.segmentIndex).wBegin, 1); // TODO (same as the C++ source): why was this GetOpcodeLength(bByte)?

            if (cDisByteType != MemoryType.DLIST) {
                bAnticLabel = false;
            }

            if (cDisByteType == MemoryType.STORE) {
                if (profile.directiveDSAllowed) {
                    disassemblyWriter.dumpStore();
                } else {
                    disassemblyWriter.dumpByte(bByte);
                }

            } else if (cDisByteType == MemoryType.BYTE) {
                disassemblyWriter.dumpByte(bByte);

            } else if (cDisByteType == MemoryType.STRING) {
                disassemblyWriter.dumpString(bByte);

            } else if (cDisByteType == MemoryType.SBYTE) {
                if (profile.directiveSBYTEAllowed) {
                    disassemblyWriter.dumpSByte(bByte);
                } else {
                    disassemblyWriter.dumpByte(bByte);
                }

            } else if (cDisByteType == MemoryType.WORD) {
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    cursor.readNext();
                    if (isCancelled()) {
                        return;
                    }
                    cLow = cursor.value;
                    if (profile.directiveWORDAllowed) {
                        wAddr = Memory.toAddress(bByte, cLow);
                        disassemblyWriter.dumpWord(wAddr);
                        staleWAddr = wAddr;
                    } else {
                        disassemblyWriter.dumpByte(bByte);
                        disassemblyWriter.dumpByte(cLow);
                    }
                }

            } else if (cDisByteType == MemoryType.SYMBOL) {
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    cursor.readNext();
                    if (isCancelled()) {
                        return;
                    }
                    cLow = cursor.value;
                    wAddr = Memory.toAddress(bByte, cLow);
                    String szLabel = (!profile.directiveBYTEOnlyNumbersAllowed)
                            ? workspace.findSymbolByAddress(oldSegmentIndex, wOldPC, wAddr) : null;
                    if (szLabel != null) {
                        disassemblyWriter.dumpLabel(szLabel);
                    } else {
                        disassemblyWriter.dumpWord(wAddr);
                    }
                    staleWAddr = wAddr;
                }

            } else if (cDisByteType == MemoryType.FIXUP) {
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    cursor.readNext();
                    if (isCancelled()) {
                        return;
                    }
                    cLow = cursor.value;
                    wAddr = Memory.toAddress(bByte, cLow);
                    String label = segmentList.getLabelAtAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, OperandMode.Accumulator, LabelAccess.READ);
                    if (label.length() != 0) {
                        disassemblyWriter.dumpLabel(label);
                    } else {
                        if (profile.directiveWORDAllowed) {
                            disassemblyWriter.dumpWord(wAddr);
                        } else {
                            disassemblyWriter.dumpByte((wAddr >> 8) & 0xFF);
                            disassemblyWriter.dumpByte(cLow & 0xFF);
                        }
                    }
                    staleWAddr = wAddr;
                }

            } else if (cDisByteType == MemoryType.LABEL) {
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    cursor.readNext();
                    if (isCancelled()) {
                        return;
                    }
                    cLow = cursor.value;
                    if (profile.directiveWORDAllowed) {
                        wAddr = Memory.toAddress(bByte, cLow);
                        String label = segmentList.getLabelAtAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, OperandMode.Accumulator, LabelAccess.READ_WRITE);
                        if (!profile.directiveBYTEOnlyNumbersAllowed && label.length() != 0) {
                            disassemblyWriter.dumpLabel(label);
                        } else {
                            disassemblyWriter.dumpWord(wAddr);
                        }
                        staleWAddr = wAddr;
                    } else {
                        disassemblyWriter.dumpByte(bByte);
                        disassemblyWriter.dumpByte(cLow);
                    }
                }

            } else if (cDisByteType == MemoryType.DLIST) {
                bAnticLabel = pass4DList(cursor, disassemblyWriter, segmentList, bByte, cDisByteType, oldSegmentIndex, wOldPC, bAnticLabel);

            } else if (cDisByteType == MemoryType.LOBYTE || cDisByteType == MemoryType.HIBYTE
                    || cDisByteType == MemoryType.UNKNOWN || cDisByteType == MemoryType.CODE) {
                if (instructionSet.getInstruction(bByte).isUnsupportedInstruction() && !profile.useIllegalOpcodes) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    int newWAddr = pass4Instruction(cursor, disassemblyWriter, segmentList, instructionSet, opcodeSegment, bByte, cDisByteType, oldSegmentIndex, wOldPC);
                    if (newWAddr >= 0) {
                        staleWAddr = newWAddr;
                    }
                }

            } else {
                // Default is an error.
                disassemblyWriter.dumpByte(bByte);
            }
        }

        disassemblyWriter.flushBytes();
    }

    private boolean pass4DList(ByteCursor cursor, DisassemblyWriter disassemblyWriter, SegmentList segmentList,
                                int bByte, MemoryType cDisByteType, int oldSegmentIndex, int wOldPC, boolean bAnticLabelIn) {
        boolean bAnticLabel = bAnticLabelIn;

        if (profile.directiveBYTEOnlyNumbersAllowed) {
            disassemblyWriter.dumpByte(bByte);
            return bAnticLabel;
        }

        if (bAnticLabel) {
            if (pass4BoundaryHit()) {
                disassemblyWriter.dumpByte(bByte);
                return false;
            }
            cursor.readNext();
            if (isCancelled()) {
                return false;
            }
            int cLow = cursor.value;

            if (profile.directiveWORDAllowed) {
                int wAddr = Memory.toAddress(bByte, cLow);
                String label = segmentList.getLabelAtAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, OperandMode.Accumulator, LabelAccess.READ_WRITE);
                if (label.length() != 0) {
                    disassemblyWriter.dumpLabel(label);
                } else {
                    disassemblyWriter.dumpWord(wAddr);
                }
            } else {
                disassemblyWriter.dumpByte(bByte);
                disassemblyWriter.dumpByte(cLow);
            }

            return false;
        }

        switch (bByte & 0x0F) {
            case 0:
                lineWriter.string(profile.directiveBYTE).space();
                if ((bByte & 0x80) != 0) {
                    lineWriter.cstring("ADLI+");
                }
                lineWriter.cstring("AEMPTY").decimal(((bByte & 0x70) >> 4) + 1);
                disassemblyWriter.flushAndAddLine(lineWriter, 0);
                break;

            case 1:
                lineWriter.string(profile.directiveBYTE).space();
                if ((bByte & 0x80) != 0) {
                    lineWriter.cstring("ADLI+");
                }
                if ((bByte & 0x40) != 0) {
                    lineWriter.cstring("AVB+");
                }
                lineWriter.cstring("AJMP");
                disassemblyWriter.flushAndAddLine(lineWriter, 0);
                bAnticLabel = true;
                break;

            default:
                if ((bByte & 0xF0) != 0) {
                    lineWriter.string(profile.directiveBYTE).space();
                    if ((bByte & 0x80) != 0) {
                        lineWriter.cstring("ADLI+");
                    }
                    if ((bByte & 0x20) != 0) {
                        lineWriter.cstring("AVSCR+");
                    }
                    if ((bByte & 0x10) != 0) {
                        lineWriter.cstring("AHSCR+");
                    }
                    if ((bByte & 0x40) != 0) {
                        lineWriter.cstring("ALMS+");
                        bAnticLabel = true;
                    }
                    lineWriter.number(bByte & 0x0F);
                    disassemblyWriter.flushAndAddLine(lineWriter, 0);
                } else {
                    disassemblyWriter.dumpByte(bByte);
                }
                break;
        }

        return bAnticLabel;
    }

    /**
     * Handles the LOBYTE/HIBYTE/UNKNOWN/CODE "actual instruction" branch of Pass4. Returns the
     * resolved wAddr for the instruction (used to update Pass4's "stale wAddr" tracking, see
     * ReservedNop2Byte/ReservedNop3Byte below), or -1 if this instruction mode doesn't produce
     * one (Pass4's outer wAddr is simply left unchanged in that case, same as the C++ source
     * where modes that don't touch wAddr leave whatever the previous instruction set it to).
     */
    private int pass4Instruction(ByteCursor cursor, DisassemblyWriter disassemblyWriter, SegmentList segmentList,
                                  InstructionSet instructionSet, Segment opcodeSegment, int bByte, MemoryType cDisByteType,
                                  int oldSegmentIndex, int wOldPC) {
        OperandMode mode = instructionSet.getInstruction(bByte).getOperandMode();

        switch (mode) {
            case Immediate: {
                int wAddr;
                if (cDisByteType == MemoryType.LOBYTE) {
                    if (pass4BoundaryHit()) {
                        disassemblyWriter.dumpByte(bByte);
                        return -1;
                    }
                    cursor.readNext();
                    if (isCancelled()) {
                        return -1;
                    }
                    int cLow = cursor.value; // bByte stays the opcode; cLow is the freshly-read operand.
                    wAddr = Memory.toAddress(cLow, cDisByteType.toByte());

                    lineWriter.instruction(opcodeSegment, bByte).cstring(" #");
                    String label = segmentList.getLabelAtAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    if (label.length() != 0) {
                        lineWriter.string(profile.directiveLOWHead).label(label).string(profile.directiveLOWTail);
                    } else {
                        lineWriter.byteValue(cLow);
                    }
                } else if (cDisByteType == MemoryType.HIBYTE) {
                    if (pass4BoundaryHit()) {
                        disassemblyWriter.dumpByte(bByte);
                        return -1;
                    }
                    cursor.readNext();
                    if (isCancelled()) {
                        return -1;
                    }
                    int cLow = cursor.value;
                    wAddr = Memory.toAddress(cDisByteType.toByte(), cLow);

                    lineWriter.instruction(opcodeSegment, bByte).cstring(" #");
                    String label = segmentList.getLabelAtAddress(oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    if (label.length() != 0) {
                        lineWriter.string(profile.directiveHIGHHead).label(label).string(profile.directiveHIGHTail);
                    } else {
                        lineWriter.byteValue(cLow);
                    }
                } else {
                    if (pass4BoundaryHit()) {
                        disassemblyWriter.dumpByte(bByte);
                        return -1;
                    }
                    cursor.readNext();
                    if (isCancelled()) {
                        return -1;
                    }
                    int cLow = cursor.value;
                    wAddr = 0;
                    lineWriter.instruction(opcodeSegment, bByte).cstring(" #");
                    if (cDisByteType == MemoryType.STRING && disassemblyWriter.isByteAllowedInString(cLow)) {
                        lineWriter.string(profile.quoteForASCIIStrings).charValue((char) cLow).string(profile.quoteForASCIIStrings);
                    } else {
                        lineWriter.byteValue(cLow);
                    }
                }

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, 0);
                return wAddr;
            }

            case Absolute: {
                int aOldPC = cursor.pc;
                int aOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int wAddr = Memory.toAddress(cL, cursor.value);

                String szLabel = (cDisByteType == MemoryType.SYMBOL) ? workspace.findSymbolByAddress(aOldSegmentIndex, aOldPC, wAddr) : null;
                if (szLabel != null) {
                    lineWriter.instruction(opcodeSegment, bByte).space().label(szLabel);
                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                    return wAddr;
                }

                if ((wAddr < 0x100) && profile.showZPAbsoluteAsByte && (bByte != 0x20) && (bByte != 0x4C)) {
                    disassemblyWriter.flushBytes();
                    lineWriter.string(profile.directiveBYTE).space().number(bByte).string(profile.directiveBYTESeparator)
                            .number(wAddr & 0xFF).string(profile.directiveBYTESeparator).number((wAddr >> 8) & 0xFF);
                    lineWriter.spaceUntil34();
                    lineWriter.space().string(profile.commentPrefix).space().instruction(opcodeSegment, bByte).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);

                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                } else {
                    lineWriter.instruction(opcodeSegment, bByte, wAddr).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);

                    disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, wAddr);
                }
                return wAddr;
            }

            case ZeroPage: {
                int zOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                lineWriter.instruction(opcodeSegment, bByte).space();
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, zOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case Accumulator: {
                lineWriter.instruction(opcodeSegment, bByte);
                if (profile.showAInAccumulatorMode) {
                    lineWriter.cstring(" A");
                }
                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 1, 0);
                return -1;
            }

            case Implied: {
                if ((bByte == 0) && profile.showBRKAsByte0) {
                    disassemblyWriter.dumpByte(bByte);
                } else {
                    lineWriter.instruction(opcodeSegment, bByte);
                    disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 1, 0);
                }
                return -1;
            }

            case IndexedIndirect: {
                int iOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).cstring(" (");
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, iOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring(",X)");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case IndirectIndexed: {
                int iOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).cstring(" (");
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, iOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring("),Y");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case ZeroPageX: {
                int zOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).space();
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, zOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring(",X");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case ZeroPageY: {
                int zOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).space();
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, zOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring(",Y");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case AbsoluteX: {
                int aOldPC = cursor.pc;
                int aOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int wAddr = Memory.toAddress(cL, cursor.value);

                String szLabel = (cDisByteType == MemoryType.SYMBOL)
                        ? workspace.findSymbolByAddress(cursor.segmentIndex, cursor.pc - 2, wAddr) : null;
                if (szLabel != null) {
                    lineWriter.instruction(opcodeSegment, bByte).space().label(szLabel).cstring(",X");
                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                    return wAddr;
                }

                if ((wAddr < 0x100) && profile.showZPAbsoluteAsByte && (bByte != 0x20) && (bByte != 0x4C)) {
                    disassemblyWriter.flushBytes();
                    lineWriter.string(profile.directiveBYTE).space();
                    lineWriter.byteValue(bByte).string(profile.directiveBYTESeparator);
                    lineWriter.byteValue(wAddr & 0xFF).string(profile.directiveBYTESeparator);
                    lineWriter.byteValue((wAddr >> 8) & 0xFF);
                    lineWriter.spaceUntil34();
                    lineWriter.space().string(profile.commentPrefix).space().instruction(opcodeSegment, bByte).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    lineWriter.cstring(",X");

                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                } else {
                    lineWriter.instruction(opcodeSegment, bByte, wAddr).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    lineWriter.cstring(",X");
                    disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, wAddr);
                }
                return wAddr;
            }

            case AbsoluteY: {
                int aOldPC = cursor.pc;
                int aOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int wAddr = Memory.toAddress(cL, cursor.value);

                String szLabel = (cDisByteType == MemoryType.SYMBOL)
                        ? workspace.findSymbolByAddress(cursor.segmentIndex, cursor.pc - 2, wAddr) : null;
                if (szLabel != null) {
                    lineWriter.instruction(opcodeSegment, bByte).space().label(szLabel).cstring(",Y");
                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                    return wAddr;
                }

                if ((wAddr < 0x100) && profile.showZPAbsoluteAsByte && (bByte != 0x20) && (bByte != 0x4C)) {
                    disassemblyWriter.flushBytes();
                    lineWriter.string(profile.directiveBYTE).space();
                    lineWriter.byteValue(bByte).string(profile.directiveBYTESeparator);
                    lineWriter.byteValue(wAddr & 0xFF).string(profile.directiveBYTESeparator);
                    lineWriter.byteValue((wAddr >> 8) & 0xFF);
                    lineWriter.spaceUntil34();
                    lineWriter.space().string(profile.commentPrefix).space().instruction(opcodeSegment, bByte).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    lineWriter.cstring(",Y");
                    disassemblyWriter.flushAndAddLine(lineWriter, wAddr);
                } else {
                    lineWriter.instruction(opcodeSegment, bByte, wAddr).space();
                    lineWriter.labelOrAddress(segmentList, oldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                    lineWriter.cstring(",Y");

                    disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, wAddr);
                }
                return wAddr;
            }

            case Relative: {
                int rOldPC = cursor.pc;
                int rOldSegmentIndex = cursor.segmentIndex;
                int wAddr = cursor.pc + 1;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                if (cL > 127) {
                    wAddr += cL - 256;
                } else {
                    wAddr += cL;
                }

                lineWriter.instruction(opcodeSegment, bByte).space();
                lineWriter.labelOrAddress(segmentList, rOldSegmentIndex, rOldPC, wAddr, cDisByteType, bByte);

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, wAddr);
                return wAddr;
            }

            case Indirect: {
                int iOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int wAddr = Memory.toAddress(cL, cursor.value);

                lineWriter.instruction(opcodeSegment, bByte).cstring(" (");
                lineWriter.labelOrAddress(segmentList, iOldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                lineWriter.cstring(")");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, wAddr);
                return wAddr;
            }

            case ZeroPageIndirect: {
                int zOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).cstring(" (");
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, zOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring(")");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case ZeroPageRelative: {
                int zOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;

                lineWriter.instruction(opcodeSegment, bByte).space();
                int addr = Memory.toAddress(cL, 0);
                lineWriter.labelOrZeroPageAddress(segmentList, zOldSegmentIndex, wOldPC, addr, cDisByteType, bByte);
                lineWriter.cstring(",X");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, addr);
                return addr;
            }

            case IndexedIndirectAbsolute: {
                int iOldSegmentIndex = cursor.segmentIndex;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int wAddr = Memory.toAddress(cL, cursor.value);

                lineWriter.instruction(opcodeSegment, bByte).cstring(" (");
                lineWriter.labelOrAddress(segmentList, iOldSegmentIndex, wOldPC, wAddr, cDisByteType, bByte);
                lineWriter.cstring(",X)");

                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, wAddr);
                return wAddr;
            }

            case ReservedNop1Byte: {
                lineWriter.string(profile.directiveBYTE).space();
                lineWriter.byteValue(bByte).space();
                lineWriter.string(profile.commentPrefix).cstring(" NOP");
                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 1, 0);
                return -1;
            }

            case ReservedNop2Byte: {
                // Faithful translation of a bug in the C++ source: DIS_GET_BYTE_IN_PASS_4 reads
                // and consumes one byte, but the rendered value below is the STALE outer
                // `wAddr` variable (whatever a previous instruction last set it to via
                // staleWAddr), not the byte that was just read. Preserved as-is.
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                lineWriter.string(profile.directiveBYTE).space();
                lineWriter.byteValue(bByte).string(profile.directiveBYTESeparator);
                lineWriter.byteValue(staleWAddr & 0xFF).space();
                lineWriter.string(profile.commentPrefix).cstring(" NOP");
                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 2, 0);
                return -1;
            }

            case ReservedNop3Byte: {
                // Faithful translation of a bug in the C++ source: DIS_GET_WORD_IN_PASS_4 reads
                // two bytes but assigns the resulting word into a byte-sized "cLow" out-param,
                // silently truncating/discarding both freshly-read bytes; the rendered value
                // below is again the STALE outer `wAddr` variable. Preserved as-is.
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                int cL = cursor.value;
                if (pass4BoundaryHit()) {
                    disassemblyWriter.dumpByte(bByte);
                    disassemblyWriter.dumpByte(cL);
                    return -1;
                }
                cursor.readNext();
                if (isCancelled()) {
                    return -1;
                }
                lineWriter.string(profile.directiveBYTE).space();
                lineWriter.byteValue(bByte).string(profile.directiveBYTESeparator);
                lineWriter.byteValue(staleWAddr & 0xFF).string(profile.directiveBYTESeparator);
                lineWriter.byteValue((staleWAddr >> 8) & 0xFF).space();
                lineWriter.string(profile.commentPrefix).cstring(" NOP");
                disassemblyWriter.flushAndAddLineWithComment(lineWriter, opcodeBuffer, 3, 0);
                return -1;
            }

            default:
                return -1;
        }
    }

    // Tracks Pass4's outer `wAddr` local across the instruction switch, purely to faithfully
    // reproduce the ReservedNop2Byte/ReservedNop3Byte staleness bug documented above (the C++
    // source reads this same still-in-scope local). Only ever meaningfully non-zero if an
    // earlier instruction/branch in the same Pass4 run set it.
    private int staleWAddr = 0;

    // --- Pass 5: generate SDX special directive for symbols or fix-ups. ---

    private void pass5() {
        SegmentList lpSegmentList = workspace.getSegmentList();

        boolean bFixups = false;
        boolean bSymReq = false;
        FileHeader wLastHeader = FileHeader.RAW;

        for (int segmentIndex = 0; segmentIndex < lpSegmentList.getCount(); segmentIndex++) {
            Segment segment = lpSegmentList.getConstSegment(segmentIndex);
            markSegmentIndex = segmentIndex;
            markOffset = markSize = 0;

            lineWriter.clear().alignInstructions();

            FileHeader header = segment.getHeader();
            if (header == FileHeader.SDX_FIX_UP_BLK) {
                if (!bFixups) {
                    bFixups = true;
                    addEmptyCommentLine();
                    addOrgOrBlock(segmentIndex, 0);
                    addLineWriter();
                }

            } else if (header == FileHeader.SDX_SYM_REQUIRED) {
                if (!bSymReq) {
                    bSymReq = true;
                    addEmptyCommentLine();
                    addOrgOrBlock(segmentIndex, 0);
                    addLineWriter();
                }

            } else if (header == FileHeader.SDX_RELOC_BLK) {
                if (segment.isSDXRelocBlkWithoutData()) {
                    addEmptyCommentLine();
                    addOrgOrBlock(segmentIndex, 0);
                    addLineWriter();
                    addEmptyCommentLine();

                    String endLabel = String.format("S%03XEND", segmentIndex);
                    lineWriter.clear().string(endLabel);
                    if (profile.showColonAfterLabel) {
                        lineWriter.charValue(':');
                    }
                    addLineWriter();

                    for (int wIndex = segment.wBegin; wIndex <= segment.wEnd; wIndex++) {
                        SegmentList.DefinedLabelResult labelResult = new SegmentList.DefinedLabelResult();
                        lpSegmentList.defineLabelAtAddress(segmentIndex, wIndex, labelResult);

                        if (labelResult.defined && !Equate.isLabelWithOffset(labelResult.label)) {
                            lineWriter.clear().string(labelResult.label).alignInstructions();
                            lineWriter.string(profile.directiveEQU).space().string(endLabel).charValue('-').decimal(segment.wEnd - wIndex + 1);
                            addLineWriter();
                        }
                    }
                }

            } else if (header == FileHeader.SDX_SYM_DEFINED) {
                if (!segment.isHeader(wLastHeader)) {
                    addEmptyCommentLine();
                }

                addOrgOrBlock(segmentIndex, 0);
                addLineWriter();
            }

            wLastHeader = segment.getHeader();
        }
    }

    private void setPass(int pass, String text) {
        this.pass = pass;
        disassemblyProgressMonitor.setPass(pass + " - " + text);
    }

    /** Runs one full disassembly. Called by DisassemblyProgressMonitor.disassembleInternal(). */
    public void disassembleInternal() {
        this.pass = 0;
        workspace.getDisassemblyResult().clear();
        workspace.clearEquateFlags();
        SegmentList segmentList = workspace.getSegmentList();
        segmentList.freeAllSymbols();
        segmentList.freeAllFixups();
        segmentList.freeAllFixupAddressLabels();
        segmentList.freeAllAddresses();

        opcodeBuffer.clear();

        lineWriter.init(workspace);

        // Pass 1: Find all labels.
        setPass(1, "Find Labels");
        Pass1.execute(workspace);

        // Pass 2: Reserve all labels.
        setPass(2, "Reserve Labels");
        pass2();

        // Pass 3: Update labels to generate relative labels.
        setPass(3, "Update Labels");
        pass3();

        // Pass 4: Generate listing.
        setPass(4, "Generate Listing");
        pass4();

        // Pass 5: Add non-code segments (RamBlk, FixUps, ...).
        setPass(5, "Add Non-Code Segments");
        pass5();

        setPass(6, "Cleanup");

        // Mark the base labels of ranges as referenced.
        workspace.getSystemEquateList().setBaseLabelsReferenced();
        workspace.getUserEquateList().setBaseLabelsReferenced();

        // Start creating the result.
        generateEquates(DisassemblySectionType.SYSTEM_EQUATES, workspace.getSystemEquateList());
        generateEquates(DisassemblySectionType.USER_EQUATES, workspace.getUserEquateList());

        // Generate labels that have not been defined in the code listing.
        generateCodeEquates();
        generateSDXSymbolDefinitions();

        Debug.log(">> START");
        debugSection(DisassemblySectionType.SYSTEM_EQUATES);

        // Fill referenced flag in lines for SYSTEM labels without offset (sta LABEL).
        setSystemEquateLinesReferencedBySystemAddress();
        Debug.log(">> AFTER CheckSystemLabelReferences");
        debugSection(DisassemblySectionType.SYSTEM_EQUATES);

        // Fill referenced flag in lines for system equates with address (sta LABEL+n).
        List<Equate> equates = workspace.getSystemEquateList().getEquates();
        for (int i = 0; i < equates.size(); i++) {
            Equate equate = equates.get(i);
            if (equate.hasReferences()) {
                setNearestSystemEquateLineReferencedByAddress(equate.getLabelValue());
            }
        }
        Debug.log(">> AFTER SetSystemEquateReferenceFlag");
        debugSection(DisassemblySectionType.SYSTEM_EQUATES);

        adjustSegmentFirstLineNumber();

        // segmentList.freeAllSymbols();            // Keep symbols for code trace
        // segmentList.freeAllFixups();              // Keep for error analysis
        // segmentList.freeAllFixupAddressLabels();  // Keep for error analysis
        // segmentList.freeAllAddresses();           // Keep for error analysis
    }

    /** Generate lines for system or user equates. */
    private void generateEquates(DisassemblySectionType disassemblySectionType, EquateList equateList) {
        List<Equate> equates = equateList.getEquates();
        for (int i = 0; i < equates.size(); i++) {
            Equate equate = equates.get(i);
            EquateType type = equate.getType();

            if (type == EquateType.UNKNOWN) {
                throw new RuntimeException("Invalid access");
            } else if (type == EquateType.EMPTY) {
                addLine("", disassemblySectionType);
            } else if (type == EquateType.COMMENT) {
                addComment(equate.getComment(), disassemblySectionType);
            } else if (type == EquateType.LABEL) {
                // Ignoring those relative to a base label.
                if (!equate.isRange()) {
                    addLabel(equate.getLabel(), equate.getLabelValue(), disassemblySectionType, equate.getComment());
                }
            }
        }
    }

    /** Generate lines for all code equates. */
    private void generateCodeEquates() {
        SegmentList segmentList = workspace.getSegmentList();
        Segment segment = segmentList.getGlobalSegment();
        AddressLabelList.AddressLabelVector addressLabelsVector = new AddressLabelList.AddressLabelVector();
        segment.getAddressLabels().enumerate(addressLabelsVector);
        for (int i = 0; i < addressLabelsVector.size(); i++) {
            AddressLabel addressLabel = addressLabelsVector.get(i);
            String label = String.format("L%04X", addressLabel.getAddress());
            addLabel(label, addressLabel.getAddress(), DisassemblySectionType.CODE_EQUATES, Strings.empty());
        }
    }
}
