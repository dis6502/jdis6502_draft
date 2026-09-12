package dis6502.core;

import java.util.Iterator;

import dis6502.core.Comment.CommentList;
import dis6502.core.Fixup.FixupList;
import dis6502.core.Symbol.SymbolList;

/**
 * Direct Java 6 translation of Segment.h / Segment.cpp.
 *
 * A Segment represents one contiguous chunk of a disassembled file (a "binary" segment with
 * actual code/data, or a header/fixup/symbol-definition block for the SDX loader format). It
 * owns a MemoryBlock (the raw bytes + per-byte MemoryType classification), the comments
 * attached to it, and (transient, not persisted) the symbols/fixups/address-labels used while
 * resolving cross-references during disassembly.
 *
 * Deferred to later passes (once the corresponding layers are translated):
 * - XML::Serializable::SerializeTo / DeserializeFrom (needs the XML wrapper layer)
 * - Load14(InputStream&) / CreateMemoryBlockFromFile(size, InputStream&) (needs the stream I/O
 *   layer; MemoryBlock's own ReadData/WriteData are deferred for the same reason)
 *
 * Fields that were public data members directly accessed by other classes in the C++ source
 * (e.g. SegmentList reaching into segment-&gt;wBegin) are kept as public fields here too, to
 * keep the translation of those call sites direct and recognizable.
 */
public final class Segment {

    public static final int LABEL_PREFIX_LENGTH = 20;

    private static final int MAX_SEGMENT_SIZE = 0x10000;

    // --- Public fields, matching the C++ class's public data members. ---

    public String szTitle = "";

    public int wBegin;  // Memory::address. Only for ATARI_BINARY and SDX_FIXED_BLK
    public int wEnd;    // Memory::address. End address for ATARI_BINARY/SDX_FIXED_BLK; size of fix-ups for SDX_SYM_REQUIRED

    public boolean bBinary; // Is it a code segment to disassemble?

    public String szLabelPrefix = "";  // Optional prefix for labels, does not contain the separator
    public int wSDXFixUpSize;          // Memory::word. Only for SDX_SYM_REQUIRED and SDX_FIX_UP_BLK
    public int bSDXBlockNumber;        // SDX block number (byte, 0-255)
    public int bSDXControlByte;        // SDX control byte for SDX_RELOC_BLK (byte, 0-255)
    public String szSDXSymbol = "";    // SDX symbol name for SDX_SYM_REQUIRED and SDX_SYM_DEFINED

    public ProcessorType processorType; // Processor type

    public final MemoryBlock memoryBlock = new MemoryBlock();

    public final CommentList comments = new CommentList(); // List of comments in this segment

    // Transient attributes which are not serialized to XML.
    public final SymbolList symbols = new SymbolList(); // List of SDX system symbols to fix-up. DO NOT SAVE IN WORKSPACE
    public final FixupList fixups = new FixupList();    // List of addresses to fix-up. DO NOT SAVE IN WORKSPACE

    private FileHeader wHeader;

    // Transient attributes which are not serialized to XML.
    private final AddressLabelList fixupAddressLabels = new AddressLabelList(); // Address labels defined in this segment through fixup. DO NOT SAVE IN WORKSPACE
    private final AddressLabelList addressLabels = new AddressLabelList();     // Addresses referenced by code. DO NOT SAVE IN WORKSPACE

    private long firstLineNumber; // First line in CODE_SECTION part of listing of this segment. DO NOT SAVE IN WORKSPACE

    /** Local translation of the file-private "enum class PrintfState" used by AllocateSymbol. */
    private enum PrintfState {
        TEXT,
        PERCENT,
        FORMAT
    }

    public Segment() {
        clear();
    }

    public void clear() {
        szTitle = "";

        wHeader = FileHeader.RAW;
        wBegin = 0;
        wEnd = 0;

        bBinary = false;
        processorType = ProcessorType.MOS6502;
        szLabelPrefix = "";
        wSDXFixUpSize = 0;
        bSDXBlockNumber = 0;
        bSDXControlByte = 0;
        szSDXSymbol = "";

        clearMemoryBlock();
        clearComments();
        symbols.clear();
        clearFixups();
        fixupAddressLabels.clear();
        addressLabels.clear();

        firstLineNumber = 0;
    }

    public FileHeader getHeader() {
        return wHeader;
    }

    public boolean isHeader(FileHeader header) {
        return this.wHeader == header;
    }

    public void setHeader(FileHeader header) {
        this.wHeader = header;
    }

    public int getSize() {
        if (memoryBlock.isEmpty()) {
            return 0;
        }
        return wEnd - wBegin + 1;
    }

    public boolean isEmpty() {
        return memoryBlock.isEmpty();
    }

    public boolean isSDX() {
        return (wHeader == FileHeader.SDX_FIXED_BLK) || (wHeader == FileHeader.SDX_SYM_REQUIRED)
                || (wHeader == FileHeader.SDX_SYM_DEFINED) || (wHeader == FileHeader.SDX_FIX_UP_BLK)
                || (wHeader == FileHeader.SDX_RELOC_BLK);
    }

    /**
     * Direct translation of Segment::ToString(). The C++ version builds the text into a
     * shared static wchar_t buffer via wsprintf and returns it via String::Format(); this
     * translation uses String.format directly against local variables, since Java has no
     * global-buffer idiom (and doesn't need one).
     */
    public String toDisplayString() {
        int size = getSize();

        switch (wHeader) {
            case SDX_RELOC_BLK:
                return String.format("%s blk %d $%04X-$%04X len %d mem $%02X",
                        getSDXBlockType(), bSDXBlockNumber, wBegin, wEnd, size, bSDXControlByte);

            case SDX_FIX_UP_BLK:
                return String.format("Fixups blk %d len %d", bSDXBlockNumber, size);

            case SDX_SYM_REQUIRED: {
                String symbol = Strings.trim(szSDXSymbol);
                return String.format("SymReq fixup len %-4d %s", size, symbol);
            }

            case SDX_SYM_DEFINED: {
                String symbol = Strings.trim(szSDXSymbol);
                return String.format("SymDef blk %d ofs $%04X %s", bSDXBlockNumber, wBegin, symbol);
            }

            case ATARI_BINARY:
                return String.format("StdBin %04X-$%04X len %04X (%d)", wBegin, wEnd, size, size);

            case SDX_FIXED_BLK:
                return String.format("StdBlk $%04X-$%04X len $%04X (%d)", wBegin, wEnd, size, size);

            default:
                if (bBinary) {
                    return String.format("Binary $%04X-$%04X len $%04X (%d)", wBegin, wEnd, size, size);
                } else {
                    return String.format("Raw    $%04X-$%04X len $%04X (%d)", wBegin, wEnd, size, size);
                }
        }
    }

    public boolean isSplittable() {
        return (isHeader(FileHeader.ATARI_BINARY) || isHeader(FileHeader.SDX_FIXED_BLK)) && !isEmpty();
    }

    public boolean canSplitAt(int offset) {
        return isSplittable() && offset > 0 && offset < getSize();
    }

    public void splitAt(int offset, Segment newSegment) {
        if (!canSplitAt(offset)) {
            throw new RuntimeException("Invalid offset");
        }
        newSegment.wHeader = this.wHeader;
        newSegment.wBegin = this.wBegin + offset;
        newSegment.wEnd = this.wEnd;
        newSegment.createMemoryBlockFromBeginToEnd();

        this.memoryBlock.copyTo(offset, newSegment.getSize(), newSegment.memoryBlock, 0);
        this.wEnd = this.wBegin + offset - 1;

        Iterator<Comment> it = comments.iterator();
        while (it.hasNext()) {
            Comment comment = it.next();
            int currentOffset = comment.getOffset();

            // If the comment offset is equal to or larger than the split offset...
            if (currentOffset >= offset) {
                // Remove it from the current segment.
                it.remove();

                // Align it to the new segment begin.
                comment.setOffset(currentOffset - offset);

                // Add it to the new segment.
                newSegment.comments.add(comment);
            }
        }
    }

    public boolean isMergeable() {
        return isSplittable();
    }

    public boolean canMergeWith(Segment nextSegment) {
        if (isMergeable() && nextSegment.isMergeable() && (wEnd + 1 == nextSegment.wBegin)) {
            int newSize = getSize() + nextSegment.getSize();
            // Do not create segments with more than 64k.
            return newSize < MAX_SEGMENT_SIZE;
        }
        return false;
    }

    public void mergeWith(Segment nextSegment) {
        int size = this.getSize();
        int totalSize = size + nextSegment.getSize();

        if (totalSize > MAX_SEGMENT_SIZE) {
            throw new RuntimeException("Merged segments must not exceed 64kb");
        }

        MemoryBlock combined = new MemoryBlock();
        combined.create(totalSize);

        this.memoryBlock.copyTo(0, size, combined, 0);
        nextSegment.memoryBlock.copyTo(0, nextSegment.getSize(), combined, size);

        createMemoryBlockWithSize(totalSize);
        combined.copyTo(0, totalSize, this.memoryBlock, 0);

        this.wEnd = nextSegment.wEnd;

        for (int i = 0; i < nextSegment.comments.size(); i++) {
            Comment comment = nextSegment.comments.get(i);
            Comment newComment = this.allocateComment();
            newComment.setOffset(size + comment.getOffset());
            newComment.setText(comment.getText());
        }

        // TODO: Transfer additional data (symbols/fixups/labels), same as the C++ source
        // (which leaves this commented out as future work).
    }

    /**
     * Whether [offset, offset+length) is a byte range that can be cut/deleted from this segment
     * without leaving it empty. Not a translation of any C++ method -- Cut/Delete-at-selection
     * is new GUI functionality with no C++ source to port (see Dis6502Gui's context-menu
     * javadoc), implemented here at the model layer alongside the existing splitAt/mergeWith.
     */
    public boolean canDeleteRange(int offset, int length) {
        return isSplittable() && offset >= 0 && length > 0 && (offset + length) <= getSize() && length < getSize();
    }

    /**
     * Removes [offset, offset+length) from this segment, shrinking wEnd by length and shifting
     * all following bytes (and their MemoryType) down. Comments inside the removed range are
     * dropped; comments after it are shifted left by length, mirroring how splitAt/mergeWith
     * re-home comments across a memory-block resize.
     */
    public void deleteRange(int offset, int length) {
        if (!canDeleteRange(offset, length)) {
            throw new RuntimeException("Invalid range to delete");
        }

        int oldSize = getSize();
        int newSize = oldSize - length;

        MemoryBlock trimmed = new MemoryBlock();
        trimmed.create(newSize);
        this.memoryBlock.copyTo(0, offset, trimmed, 0);
        this.memoryBlock.copyTo(offset + length, oldSize - (offset + length), trimmed, offset);

        createMemoryBlockWithSize(newSize);
        trimmed.copyTo(0, newSize, this.memoryBlock, 0);

        this.wEnd -= length;

        java.util.Iterator<Comment> it = comments.iterator();
        while (it.hasNext()) {
            Comment comment = it.next();
            int commentOffset = comment.getOffset();
            if (commentOffset >= offset && commentOffset < offset + length) {
                it.remove();
            } else if (commentOffset >= offset + length) {
                comment.setOffset(commentOffset - length);
            }
        }
    }

    /**
     * Whether insertData.length bytes can be inserted at offset without exceeding the 64k
     * segment-size limit enforced elsewhere (e.g. mergeWith).
     */
    public boolean canInsertBytes(int offset, int insertLength) {
        return isSplittable() && offset >= 0 && offset <= getSize() && insertLength > 0
                && (getSize() + insertLength) <= MAX_SEGMENT_SIZE;
    }

    /**
     * Inserts insertData at offset, growing this segment by insertData.length bytes (all newly
     * inserted bytes get MemoryType.UNKNOWN, same as any other freshly-allocated memory --
     * see MemoryBlock::Create) and shifting wEnd and all following bytes/comments up.
     */
    public void insertBytes(int offset, byte[] insertData) {
        if (!canInsertBytes(offset, insertData.length)) {
            throw new RuntimeException("Invalid range to insert at, or resulting segment would exceed 64kb");
        }

        int oldSize = getSize();
        int newSize = oldSize + insertData.length;

        MemoryBlock grown = new MemoryBlock();
        grown.create(newSize);
        this.memoryBlock.copyTo(0, offset, grown, 0);
        this.memoryBlock.copyTo(offset, oldSize - offset, grown, offset + insertData.length);

        createMemoryBlockWithSize(newSize);
        grown.copyTo(0, newSize, this.memoryBlock, 0);

        for (int i = 0; i < insertData.length; i++) {
            setData(offset + i, insertData[i] & 0xFF);
        }

        this.wEnd += insertData.length;

        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            if (comment.getOffset() >= offset) {
                comment.setOffset(comment.getOffset() + insertData.length);
            }
        }
    }

    public boolean isSDXRelocBlkWithData() {
        return isHeader(FileHeader.SDX_RELOC_BLK) && ((bSDXControlByte & 0x80) == 0x00);
    }

    public boolean isSDXRelocBlkWithoutData() {
        return isHeader(FileHeader.SDX_RELOC_BLK) && ((bSDXControlByte & 0x80) == 0x80);
    }

    public String getSDXBlockType() {
        switch (bSDXControlByte & 0x80) {
            case 0x00:
                return "RelBlk";
            case 0x80:
                return "RamBlk";
            default:
                return "";
        }
    }

    public String getSDXMemoryType() {
        switch (bSDXControlByte & 0x7f) {
            case 0x00:
                return "main";
            case 0x02:
                return "extended";
            default:
                return "";
        }
    }

    public void createMemoryBlockFromBeginToEnd() {
        if (wEnd < wBegin) {
            throw new RuntimeException("End must not be before begin");
        }
        int size = wEnd - wBegin + 1;
        createMemoryBlockWithSize(size);
    }

    public void createMemoryBlockWithSize(int size) {
        // Free anything that is already there.
        clearMemoryBlock();
        memoryBlock.create(size);
    }

    // NOTE: Segment::CreateMemoryBlockFromFile(size, InputStream&) and Segment::Load14
    // (InputStream&) are deferred: they depend on the stream I/O layer (InputStream,
    // MemoryBlock::ReadData/ReadType), which has not been translated yet.

    public void clearMemoryBlock() {
        memoryBlock.clear();
    }

    public int getData(int offset) {
        return memoryBlock.getDataAt(offset);
    }

    public int getWord(int offset) {
        return getData(offset) | (getData(offset + 1) << 8);
    }

    public void setData(int offset, int data) {
        memoryBlock.setDataAt(offset, data);
    }

    public void setData(int offset, ByteSequence data, int dataOffset, int dataSize) {
        memoryBlock.setDataAt(offset, data, dataOffset, dataSize);
    }

    public MemoryType getType(int offset) {
        return memoryBlock.getTypeAt(offset);
    }

    public boolean isType(int offset, MemoryType memoryType) {
        return getType(offset) == memoryType;
    }

    public boolean isUnknown(int offset) {
        if (!isType(offset, MemoryType.UNKNOWN)) {
            return false;
        }

        // Offset 0 is never a low/high byte.
        if (offset == 0) {
            return true;
        }

        // Not a low byte and not a high byte.
        if (!isType(offset - 1, MemoryType.LOBYTE) && !isType(offset - 1, MemoryType.HIBYTE)) {
            return true;
        }

        return false;
    }

    public void setType(int offset, MemoryType memoryType) {
        memoryBlock.setTypeAt(offset, memoryType);
    }

    public void setType(int offset, MemoryType memoryType, int size) {
        memoryBlock.setTypeAt(offset, memoryType, size);
    }

    public boolean containsAddress(int address) {
        return !isEmpty() && (wBegin <= address) && (address <= wEnd);
    }

    public void clearComments() {
        comments.clear();
    }

    public Comment allocateComment() {
        Comment comment = new Comment();
        comments.add(comment);
        return comment;
    }

    public String findComment(int offset) {
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            if (comment.getOffset() == offset) {
                return comment.getText();
            }
        }
        return Strings.empty();
    }

    public void deleteComments(int offset, int size) {
        Iterator<Comment> it = comments.iterator();
        while (it.hasNext()) {
            Comment comment = it.next();
            if ((comment.getOffset() >= offset) && (comment.getOffset() < (offset + size))) {
                it.remove();
            }
        }
    }

    public void allocateSymbol(int address, String name) {
        if (isEmpty()) {
            throw new RuntimeException("Cannot allocate symbol in empty segment");
        }
        symbols.add(new Symbol(address, name));

        if ((address >= wBegin) && (address < wEnd)) {
            int offset = address - wBegin;

            if (isUnknown(offset) || isType(offset, MemoryType.CODE)) {
                setType(offset++, MemoryType.SYMBOL);
                setType(offset++, MemoryType.SYMBOL);

                // Check if the fix-up is for a "jsr PRINTF". If so, set the type of the
                // following bytes to STRING until the end-of-string character ('\0').
                if ((offset > 2) && (getData(offset - 3) == 0x20)) { // JSR opcode
                    if ("PRINTF".equals(name)) {
                        int wBytesAsLabels = 0;
                        PrintfState nPrintState = PrintfState.TEXT;

                        while (offset < getSize()) {
                            if (isUnknown(offset)) {
                                setType(offset, MemoryType.STRING);
                            }

                            int bByte = getData(offset);
                            offset++;
                            if (bByte == 0) {
                                break;
                            }

                            switch (nPrintState) {
                                case TEXT:
                                    if (bByte == '%') {
                                        nPrintState = PrintfState.PERCENT;
                                    }
                                    break;

                                case PERCENT:
                                    if (bByte == '%') {
                                        nPrintState = PrintfState.TEXT;
                                        wBytesAsLabels += 2;
                                    } else {
                                        nPrintState = PrintfState.FORMAT;
                                        wBytesAsLabels += 2;
                                    }
                                    break;

                                case FORMAT:
                                    if (bByte == '*') {
                                        wBytesAsLabels += 2;
                                    } else if (bByte == 'c' || bByte == 's' || bByte == 'p' || bByte == 'x'
                                            || bByte == 'b' || bByte == 'd' || bByte == 'e' || bByte == 'l'
                                            || bByte == 't') {
                                        nPrintState = PrintfState.TEXT;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }

                        while ((wBytesAsLabels > 0) && (offset < getSize())) {
                            if (isType(offset, MemoryType.UNKNOWN)) {
                                setType(offset, MemoryType.LABEL);
                            }
                            offset++;
                            wBytesAsLabels--;
                        }
                    }
                }
            }
        }
    }

    public Symbol findSymbol(int address) {
        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            if (symbol.getAddress() == address) {
                return symbol;
            }
        }
        return null;
    }

    public void clearFixups() {
        fixups.clear();
    }

    /**
     * Direct translation of Segment::AllocateFixup. labelSegment is the Segment that owns the
     * (fixup-)address-label list to register the fixed-up address into.
     */
    public Fixup allocateFixup(int labelSegmentIndex, Segment labelSegment, int address) {
        Fixup fixup = null;

        if (!memoryBlock.isEmpty()) {
            fixup = new Fixup();
            fixup.setAddress(address);
            fixup.setLabelSegmentIndex(labelSegmentIndex);

            // Keep the fixups list sorted by address, same as the C++ insertion-point search.
            int insertAt = fixups.size();
            for (int i = 0; i < fixups.size(); i++) {
                if (fixups.get(i).getAddress() >= address) {
                    insertAt = i;
                    break;
                }
            }
            fixups.add(insertAt, fixup);

            if ((address >= wBegin) && (address < wEnd)) { // "<" because it must be a word at offset/offset+1
                int offset = address - wBegin;

                if (isUnknown(offset) || isType(offset, MemoryType.CODE)) {
                    setType(offset, MemoryType.FIXUP);
                    setType(offset + 1, MemoryType.FIXUP);
                }

                int adjustAddress = getWord(offset);
                labelSegment.fixupAddressLabels.allocateAddressLabel(adjustAddress);
            }
        }
        return fixup;
    }

    public Fixup findFixup(int address) {
        for (int i = 0; i < fixups.size(); i++) {
            Fixup fixup = fixups.get(i);
            if (fixup.getAddress() == address) {
                return fixup;
            }
        }
        return null;
    }

    public AddressLabelList getFixupAddressLabels() {
        return fixupAddressLabels;
    }

    public AddressLabelList getAddressLabels() {
        return addressLabels;
    }

    /**
     * Define an address label. If there is a matching fixup address label, it will be set to
     * aligned. Otherwise a matching code address label will be set to aligned.
     */
    public void defineAddressLabel(int address) {
        AddressLabel addressLabel = fixupAddressLabels.findMutableAddressLabel(address);

        if (addressLabel != null) {
            addressLabel.setAligned(true);
            return;
        }

        addressLabel = addressLabels.findMutableAddressLabel(address);
        if (addressLabel != null) {
            addressLabel.setAligned(true);
        }
    }

    public void alignAddressLabels(int address) {
        fixupAddressLabels.alignAddressLabels(address);
        addressLabels.alignAddressLabels(address);
    }

    public void alignRamBlkAddresses() {
        if (isSDXRelocBlkWithoutData()) {
            fixupAddressLabels.alignNearestAddress();
            addressLabels.alignNearestAddress();
        }
    }

    public void setFirstLineNumber(long firstLineNumber) {
        this.firstLineNumber = firstLineNumber;
    }

    /** 0 means it has not been set at all. */
    public long getFirstLineNumber() {
        return firstLineNumber;
    }

    // --- Label-formatting helpers -------------------------------------------------------
    // Direct translations of the four static wsprintf format strings (szLabelFormat,
    // szDefaultLabelFormat, szLabelOffsetFormat, szDefaultLabelOffsetFormat) used throughout
    // SegmentList, expressed as formatting methods instead of raw pattern strings, since
    // Java has no wsprintf/%hX-style format specifiers.

    /** Corresponds to wsprintf(buffer, Segment::szLabelFormat, prefix, address). */
    public static String formatLabel(String labelPrefix, int address) {
        return String.format("%sL%04X", labelPrefix, address);
    }

    /** Corresponds to wsprintf(buffer, Segment::szDefaultLabelFormat, segmentIndex, address). */
    public static String formatDefaultLabel(int segmentIndex, int address) {
        return String.format("S%03XL%04X", segmentIndex, address);
    }

    /** Corresponds to wsprintf(buffer, Segment::szLabelOffsetFormat, prefix, address, offset). */
    public static String formatLabelOffset(String labelPrefix, int address, int offset) {
        return String.format("%sL%04X+%d", labelPrefix, address, offset);
    }

    /** Corresponds to wsprintf(buffer, Segment::szDefaultLabelOffsetFormat, segmentIndex, address, offset). */
    public static String formatDefaultLabelOffset(int segmentIndex, int address, int offset) {
        return String.format("S%03XL%04X+%d", segmentIndex, address, offset);
    }

    /**
     * Direct translation of Segment::SerializeTo. The C++ source also (de)serializes
     * Symbols/Fixups/FixupAddressLabels/AddressLabels, but that whole block is commented out
     * there ("Sort and serialize transient elements for debugging") and therefore not
     * reproduced here either -- those fields are transient (DO NOT SAVE IN WORKSPACE, per their
     * own field comments in this class) and are never part of the saved file.
     */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setStringAttribute(element, "Title", szTitle);

        Xml.setWordAttributeHex(element, "Header", wHeader.getValue());
        Xml.setWordAttributeHex(element, "Begin", wBegin);
        Xml.setWordAttributeHex(element, "End", wEnd);

        Xml.setBoolAttribute(element, "Binary", bBinary);
        Xml.setStringAttribute(element, "LabelPrefix", szLabelPrefix);

        Xml.setWordAttribute(element, "SDXFixUpSize", wSDXFixUpSize);
        Xml.setByteAttribute(element, "SDXBlockNumber", bSDXBlockNumber);
        Xml.setByteAttributeHex(element, "SDXControlByte", bSDXControlByte);
        Xml.setStringAttribute(element, "SDXSymbol", szSDXSymbol);

        Xml.setStringAttribute(element, "ProcessorType", ProcessorType.ProcessorTypeFactory.getInfo(processorType).getKey());

        org.w3c.dom.Element contentElement = Xml.addChildElement(element, "Content");
        memoryBlock.serializeTo(contentElement);

        org.w3c.dom.Element commentsElement = Xml.addChildElement(element, "Comments");
        for (int i = 0; i < comments.size(); i++) {
            comments.get(i).serializeTo(Xml.addChildElement(commentsElement, "Comment"));
        }
    }

    public void deserializeFrom(org.w3c.dom.Element element) {
        clear();

        szTitle = Xml.getStringAttribute(element, "Title", szTitle);

        wHeader = FileHeader.fromValue(Xml.getWordAttribute(element, "Header", wHeader.getValue()));
        wBegin = Xml.getWordAttribute(element, "Begin", wBegin);
        wEnd = Xml.getWordAttribute(element, "End", wEnd);

        bBinary = Xml.getBoolAttribute(element, "Binary", bBinary);
        szLabelPrefix = Xml.getStringAttribute(element, "LabelPrefix", szLabelPrefix);
        wSDXFixUpSize = Xml.getWordAttribute(element, "SDXFixUpSize", wSDXFixUpSize);
        bSDXBlockNumber = Xml.getByteAttribute(element, "SDXBlockNumber", bSDXBlockNumber);
        bSDXControlByte = Xml.getByteAttribute(element, "SDXControlByte", bSDXControlByte);
        szSDXSymbol = Xml.getStringAttribute(element, "SDXSymbol", szSDXSymbol);

        String processorTypeString = Xml.getStringAttribute(element, "ProcessorType", "");
        processorType = ProcessorType.ProcessorTypeFactory.getInfo(processorTypeString).getProcessorType();
        if (processorType == ProcessorType.UNKNOWN) {
            processorType = ProcessorType.MOS6502;
        }

        org.w3c.dom.Element contentElement = Xml.firstChildElement(element, "Content");
        if (contentElement != null) {
            memoryBlock.deserializeFrom(contentElement);
        }

        org.w3c.dom.Element commentsElement = Xml.firstChildElement(element, "Comments");
        if (commentsElement != null) {
            org.w3c.dom.Element commentElement = Xml.firstChildElement(commentsElement, "Comment");
            while (commentElement != null) {
                Comment comment = allocateComment();
                comment.deserializeFrom(commentElement);
                commentElement = Xml.nextSiblingElement(commentElement, "Comment");
            }
        }
    }
}
