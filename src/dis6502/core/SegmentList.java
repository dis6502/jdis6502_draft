package dis6502.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of SegmentList.h / SegmentList.cpp.
 *
 * Owns the ordered list of Segments that make up a disassembly workspace, plus the single
 * "global segment" used to hold address labels that aren't tied to any particular segment.
 * Provides segment CRUD (insert/delete/split/merge/move), address-label allocation used while
 * walking the disassembly, and label-text resolution (GetLabelAtAddress) that consults user
 * equates, system equates, fixups, and address labels, in the same precedence order as the
 * original C++ logic.
 *
 * Deferred to later passes:
 * - XML::Serializable::SerializeTo / DeserializeFrom (needs the XML wrapper layer)
 * - CreateInserter() / SegmentListInserter (a separate, not-yet-translated helper class)
 * - Workspace / EquateList are backed here by the placeholder classes in Workspace.java /
 *   EquateList.java / Equate.java -- swap those out once the real classes are translated.
 */
public final class SegmentList {

    public static final int NO_SEGMENT_INDEX = -1;
    public static final int MAX_SEGMENTS = 4096;

    /** Direct translation of "enum class Property". */
    public enum Property {
        SEGMENTS,
        SEGMENT_CONTENT,
        SELECTED_INDEX;

        /** Direct translation of the free function "ToString(SegmentList::Property value)". */
        public String toDisplayString() {
            switch (this) {
                case SEGMENTS:
                    return "SEGMENTS";
                case SEGMENT_CONTENT:
                    return "SEGMENT_CONTENT";
                case SELECTED_INDEX:
                    return "SELECTED_INDEX";
                default:
                    return "";
            }
        }
    }

    private Workspace workspace; // Can be null: no equate handling is performed in that case.
    private final List<Segment> segmentList = new ArrayList<Segment>();
    private final Segment globalSegment = new Segment();

    private int selectedIndex;

    // Event handling.
    private int updateCounter = 0;
    private final List<Property> propertyChangeEvents = new ArrayList<Property>();
    private final List<SegmentListChangedListener> listeners = new ArrayList<SegmentListChangedListener>();

    /** Workspace can be null; no equate handling is performed later in that case. */
    public SegmentList(Workspace workspace) {
        this.workspace = workspace;
        this.selectedIndex = NO_SEGMENT_INDEX;
    }

    public void clear() {
        beginUpdate();
        setSelectedIndex(NO_SEGMENT_INDEX);
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).clear();
        }
        segmentList.clear();
        globalSegment.clear();
        notifyListeners(Property.SEGMENTS);
        endUpdate();
    }

    public boolean isEmpty() {
        return segmentList.isEmpty();
    }

    /** Returns the number of used segments. */
    public int getCount() {
        return segmentList.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int segmentIndex) {
        selectedIndex = segmentIndex;
        notifyListeners(Property.SELECTED_INDEX);
    }

    // NOTE: CreateInserter() / SegmentListInserter is deferred -- that helper class has not
    // been translated yet.

    public Segment getGlobalSegment() {
        return globalSegment;
    }

    public int getSegmentIndex(Segment segment) {
        if (segment == null) {
            return NO_SEGMENT_INDEX;
        }

        int segmentCount = getCount();
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            if (segmentList.get(segmentIndex) == segment) {
                return segmentIndex;
            }
        }

        return NO_SEGMENT_INDEX;
    }

    public Segment getSegment(int segmentIndex) {
        return segmentList.get(segmentIndex);
    }

    public Segment getConstSegment(int segmentIndex) {
        return segmentList.get(segmentIndex);
    }

    private Segment addSegment() {
        return insertSegmentAt(getCount());
    }

    public Segment insertSegmentAt(int segmentIndex) {
        if (segmentIndex > getCount()) {
            throw new RuntimeException("Invalid index larger than the current size of the list");
        }

        Segment segment = new Segment();
        segmentList.add(segmentIndex, segment);

        return segment;
    }

    private void deleteSegment(int segmentIndex) {
        segmentList.remove(segmentIndex);
        notifyListeners(Property.SEGMENTS);
    }

    /** Checks if a fixup exists for this address. */
    private Fixup getFixup(int segmentIndex, MemoryType cDisByteType, int wPC, int wAddr) {
        return segmentList.get(segmentIndex).findFixup(wPC);
    }

    /** Returns the segment whose block number is given in parameter. */
    public Segment findBySDXBlockNumber(int sdxBlockNumber) {
        // Try to find a block with the same id.
        for (int i = 0; i < segmentList.size(); i++) {
            Segment segment = segmentList.get(i);
            if (segment.isHeader(FileHeader.SDX_RELOC_BLK)) {
                if (segment.bSDXBlockNumber == sdxBlockNumber) {
                    return segment;
                }
            }
        }

        // No block id match: get the block of code.
        int blockNumber = 0;

        for (int i = 0; i < segmentList.size(); i++) {
            Segment segment = segmentList.get(i);
            if (segment.isHeader(FileHeader.ATARI_BINARY)
                    || segment.isHeader(FileHeader.SDX_FIXED_BLK)
                    || segment.isHeader(FileHeader.SDX_RELOC_BLK)) {
                blockNumber++;
                if (blockNumber == sdxBlockNumber) {
                    return segment;
                }
            }
        }

        return null;
    }

    /** Returns the index of the segment that contains the given address. */
    public int findByAddr(int wAddr) {
        int segmentCount = getCount();
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            Segment segment = segmentList.get(segmentIndex);
            if (segment.isHeader(FileHeader.ATARI_BINARY)
                    || segment.isHeader(FileHeader.SDX_FIXED_BLK)
                    || segment.isHeader(FileHeader.SDX_RELOC_BLK)) {
                if ((segment.wBegin <= wAddr) && (segment.wEnd >= wAddr)) {
                    return segmentIndex;
                }
            }
        }
        return NO_SEGMENT_INDEX;
    }

    /** Returns the index of the segment which contains the address (ATARI_BINARY / SDX_FIXED_BLK only). */
    private int findSegmentByFixedAddr(int segmentIndex, int address) {
        // Try to find it in the current segment first.
        Segment segment = segmentList.get(segmentIndex);
        if (segment.containsAddress(address)) {
            return segmentIndex;
        }

        // Not found: try to find it in all segments.
        int segmentCount = getCount();
        for (int i = 0; i < segmentCount; i++) {
            Segment other = segmentList.get(i);
            if (other.isHeader(FileHeader.ATARI_BINARY) || other.isHeader(FileHeader.SDX_FIXED_BLK)) {
                if (other.containsAddress(address)) {
                    return i;
                }
            }
        }
        return NO_SEGMENT_INDEX;
    }

    /** Returns the index of the segment for the target address but with the same kind of segment (file header). */
    private int findSegmentWithSameFileHeaderByAddr(int segmentIndex, int address) {
        // Try to find it in the current segment first.
        Segment sourceSegment = segmentList.get(segmentIndex);
        if (sourceSegment.containsAddress(address)) {
            return segmentIndex;
        }

        // Not found: try to find it in all other segments.
        int segmentCount = getCount();
        for (int otherSegmentIndex = 0; otherSegmentIndex < segmentCount; otherSegmentIndex++) {
            if (otherSegmentIndex != segmentIndex) {
                Segment otherSegment = segmentList.get(otherSegmentIndex);
                if (otherSegment.isHeader(sourceSegment.getHeader()) && otherSegment.containsAddress(address)) {
                    // Note: faithfully reproduces the C++ source, which returns the *original*
                    // segmentIndex here rather than otherSegmentIndex -- that looks like it
                    // may be a bug in the original code, but this translation preserves it.
                    return segmentIndex;
                }
            }
        }
        return NO_SEGMENT_INDEX;
    }

    public int mergeSegments() {
        beginUpdate();

        int mergedCount = 0;
        int count = getCount();
        for (int segmentIndex = 0; segmentIndex < count - 1; segmentIndex++) {
            Segment segment = segmentList.get(segmentIndex);
            Segment nextSegment = segmentList.get(segmentIndex + 1);

            if (segment.canMergeWith(nextSegment)) {
                segment.mergeWith(nextSegment);
                deleteSegment(segmentIndex + 1);
                mergedCount++;
            }
        }
        if (mergedCount > 0) {
            setSelectedIndex(0);
        }
        endUpdate();
        return mergedCount;
    }

    public void splitSelectedSegment(int offset) {
        int segmentIndex = getSelectedIndex();
        if (segmentIndex == NO_SEGMENT_INDEX) {
            return;
        }
        Segment segment = segmentList.get(segmentIndex);

        beginUpdate();

        if (!segment.memoryBlock.isEmpty()) {
            Segment newSegment = insertSegmentAt(segmentIndex + 1);
            segment.splitAt(offset, newSegment);

            setSelectedIndex(segmentIndex + 1);
        }
        endUpdate();
    }

    public void deleteSelectedSegment() {
        int segmentIndex = getSelectedIndex();
        if (segmentIndex == NO_SEGMENT_INDEX) {
            return;
        }

        beginUpdate();
        deleteSegment(segmentIndex);

        // Select the previous segment, if there is any.
        if (segmentIndex > 0) {
            segmentIndex--;
        } else {
            segmentIndex = NO_SEGMENT_INDEX;
        }
        setSelectedIndex(segmentIndex);
        endUpdate();
    }

    public void moveSelectedSegmentUp() {
        int segmentIndex = getSelectedIndex();
        if (segmentIndex > 0) {
            int newSegmentIndex = segmentIndex - 1;
            Segment old = segmentList.get(segmentIndex);
            segmentList.set(segmentIndex, segmentList.get(newSegmentIndex));
            segmentList.set(newSegmentIndex, old);
            notifyListeners(Property.SEGMENTS);
            setSelectedIndex(newSegmentIndex);
        }
    }

    public void moveSelectedSegmentDown() {
        int segmentIndex = getSelectedIndex();
        beginUpdate();
        if (segmentIndex < getCount() - 1) {
            int newSegmentIndex = segmentIndex + 1;
            Segment old = segmentList.get(segmentIndex);
            segmentList.set(segmentIndex, segmentList.get(newSegmentIndex));
            segmentList.set(newSegmentIndex, old);
            notifyListeners(Property.SEGMENTS);
            setSelectedIndex(newSegmentIndex);
        }
        endUpdate();
    }

    public void freeAllSymbols() {
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).symbols.clear();
        }
    }

    public void freeAllFixups() {
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).clearFixups();
        }
    }

    public void freeAllFixupAddressLabels() {
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).getFixupAddressLabels().clear();
        }
    }

    public boolean allocateAddress(int segmentIndex, int wPC, int wAddr, MemoryType cType, Instruction instruction) {
        return allocateAddress(segmentIndex, wPC, wAddr, cType, instruction.getOperandMode(), instruction.getLabelAccess());
    }

    public boolean allocateAddress(int segmentIndex, int wPC, int wAddr, MemoryType cType, OperandMode wMode, LabelAccess labelAccess) {
        if (cType == MemoryType.SYMBOL) {
            return false;
        }
        if (workspace.getUserEquateList().findEquateByAddress(wAddr, labelAccess, true) != null) { // TODO: Why not check system equates, too?
            return false;
        }
        if (getFixup(segmentIndex, cType, wPC, wAddr) != null) {
            return false;
        }
        if (wMode != OperandMode.Relative) {
            Segment segment = segmentList.get(segmentIndex);
            if (workspace.getSystemEquateList().findEquateByAddress(wAddr, labelAccess, segment.isSDX()) != null) {
                return false;
            }
        } else {
            // For a relative branch, find in the same kind of segment first.
            int otherSegmentIndex = findSegmentWithSameFileHeaderByAddr(segmentIndex, wAddr);
            if (otherSegmentIndex != NO_SEGMENT_INDEX) {
                allocateSegmentAddress(otherSegmentIndex, wAddr);
                return true;
            }
        }
        int otherSegmentIndex = findSegmentByFixedAddr(segmentIndex, wAddr);
        if (otherSegmentIndex == NO_SEGMENT_INDEX) {
            globalSegment.getAddressLabels().allocateAddressLabel(wAddr);
        } else {
            allocateSegmentAddress(otherSegmentIndex, wAddr);
        }
        return true;
    }

    private void allocateSegmentAddress(int segmentIndex, int wAddr) {
        Segment segment = getSegment(segmentIndex);
        segment.getAddressLabels().allocateAddressLabel(wAddr);
    }

    public void freeAllAddresses() {
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).getAddressLabels().clear();
        }
        globalSegment.getAddressLabels().clear();
    }

    public void alignRamBlkLabelAddresses() {
        for (int i = 0; i < segmentList.size(); i++) {
            segmentList.get(i).alignRamBlkAddresses();
        }
    }

    /** Result holder for the "wstring DefineLabelAtAddress(..., bool& defined)" out-parameter. */
    public static final class DefinedLabelResult {
        public boolean defined;
        public String label;
    }

    public void defineLabelAtAddress(int segmentIndex, int address, DefinedLabelResult result) {
        result.defined = false;

        Equate equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
        if (equate != null) {
            result.defined = true;
            result.label = equate.getLabel();
            return;
        }
        Segment segment = segmentList.get(segmentIndex);
        AddressLabel addressLabel = segment.getFixupAddressLabels().findNearestAddressLabel(address); // TODO: redundant with the addressLabels case below, same as the C++ source
        boolean found = false;
        if (addressLabel != null) {
            if (addressLabel.isAligned()) {
                address = addressLabel.getAddress();
            } else {
                address = addressLabel.getNearestAddress();
            }
            equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
            if (equate != null) {
                result.defined = true;
                result.label = equate.getLabel();
                return;
            }
            found = true;
        } else {
            AddressLabel otherAddressLabel = segment.getAddressLabels().findNearestAddressLabel(address);
            if (otherAddressLabel != null) {
                if (otherAddressLabel.isAligned()) {
                    address = otherAddressLabel.getAddress();
                } else {
                    address = otherAddressLabel.getNearestAddress();
                }
                equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
                if (equate != null) {
                    result.defined = true; // Note: the original C++ source omits "defined = true"
                                            // on this exact branch (likely oversight); this
                                            // translation sets it for internal consistency
                                            // with every other branch that finds an equate.
                    result.label = equate.getLabel();
                    return;
                }
                found = true;
            }
        }
        if (found) {
            String label;
            if (segment.szLabelPrefix.length() == 0 && segment.isSDX()) {
                label = Segment.formatDefaultLabel(segmentIndex, address);
            } else {
                label = Segment.formatLabel(segment.szLabelPrefix, address);
            }
            result.defined = true;
            result.label = label;
            return;
        }
        result.label = "";
    }

    private String buildAddress(int segmentIndex, int wAddr, LabelAccess labelAccess, boolean bNoNearest) {
        Segment segment = segmentList.get(segmentIndex);
        AddressLabel addressLabel = segment.getAddressLabels().findAddressLabel(wAddr);
        if (addressLabel != null) {
            int wNearestAddr = addressLabel.getNearestAddress();
            if ((addressLabel.isAligned()) || (wAddr == wNearestAddr) || (wNearestAddr == 0) || (bNoNearest)) {
                wAddr = addressLabel.getAddress();
                if (segment.isSDX()) {
                    return Segment.formatDefaultLabel(segmentIndex, wAddr);
                } else {
                    return String.format("L%04X", wAddr);
                }
            } else {
                wAddr = addressLabel.getAddress();
                Equate equate = workspace.getUserEquateList().findEquateByAddress(wNearestAddr, labelAccess, true);
                if (equate != null) {
                    equate.addLabelReference(labelAccess);
                    return String.format("%s+%d", equate.getLabel(), wAddr - wNearestAddr);
                } else {
                    if (segment.isSDX()) {
                        return Segment.formatDefaultLabelOffset(segmentIndex, wNearestAddr, wAddr - wNearestAddr);
                    } else {
                        return String.format("L%04X+%d", wNearestAddr, wAddr - wNearestAddr);
                    }
                }
            }
        }
        addressLabel = globalSegment.getAddressLabels().findAddressLabel(wAddr);
        if (addressLabel != null) {
            return String.format("L%04X", wAddr);
        }
        return Strings.empty();
    }

    public String getLabelAtAddress(int segmentIndex, int wPC, int wAddr, MemoryType cType, int opcode) {
        Segment segment = segmentList.get(segmentIndex);
        InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
        Instruction instruction = instructionSet.getInstruction(opcode);
        return getLabelAtAddress(segmentIndex, wPC, wAddr, cType, instruction.getOperandMode(), instruction.getLabelAccess());
    }

    public String getLabelAtAddress(int segmentIndex, int wPC, int wAddr, MemoryType cType, OperandMode wMode, LabelAccess labelAccess) {
        // Private delegation method to place central breakpoints, same as the C++ source.
        return getLabelAtAddressInternal(segmentIndex, wPC, wAddr, cType, wMode, labelAccess);
    }

    private String getLabelAtAddressInternal(int segmentIndex, int wPC, int wAddr, MemoryType cType, OperandMode wMode, LabelAccess labelAccess) {
        Equate equate = workspace.getUserEquateList().findEquateByAddress(wAddr, labelAccess, true);
        if (equate != null) {
            equate.addLabelReference(labelAccess);
            return equate.getLabel();
        }

        boolean sdx = getSegment(segmentIndex).isSDX();
        if ((wMode == OperandMode.ZeroPageX) || (wMode == OperandMode.ZeroPageY) || (wMode == OperandMode.ZeroPage)
                || (wMode == OperandMode.IndexedIndirect) || (wMode == OperandMode.IndirectIndexed)) {
            equate = workspace.getSystemEquateList().findEquateByAddress(wAddr, labelAccess, sdx);
            if (equate != null) {
                return equate.getLabel();
            }
            return buildAddress(segmentIndex, wAddr, labelAccess, true);
        }
        Fixup fixup = getFixup(segmentIndex, cType, wPC, wAddr);
        if (fixup != null) {
            segmentIndex = fixup.getLabelSegmentIndex();
            Segment segment = segmentList.get(segmentIndex);
            AddressLabel addressLabel = segment.getFixupAddressLabels().findAddressLabel(wAddr);
            if (addressLabel != null) {
                int wNearestAddr = addressLabel.getNearestAddress();
                if ((addressLabel.isAligned()) || (wAddr == wNearestAddr)) {
                    wAddr = addressLabel.getAddress();
                    return Segment.formatDefaultLabel(segmentIndex, wAddr);
                } else {
                    wAddr = addressLabel.getAddress();
                    equate = workspace.getUserEquateList().findEquateByAddress(wNearestAddr, labelAccess, true);
                    int offset = wAddr - wNearestAddr;
                    if (equate != null) {
                        equate.addLabelReference(labelAccess);
                        return String.format("%s+%d", equate.getLabel(), offset);
                    } else {
                        return Segment.formatDefaultLabelOffset(segmentIndex, wNearestAddr, offset);
                    }
                }
            }
            return Strings.empty();
        }
        if (wMode == OperandMode.Relative) {
            // For a relative branch, find in the same kind of segment first.
            int otherSegmentIndex = findSegmentWithSameFileHeaderByAddr(segmentIndex, wAddr);
            if (otherSegmentIndex != NO_SEGMENT_INDEX) {
                String label = buildAddress(otherSegmentIndex, wAddr, labelAccess, false);
                if (label.length() != 0) {
                    return label;
                }
            }
        }
        int otherSegmentIndex = findSegmentByFixedAddr(segmentIndex, wAddr);
        if (otherSegmentIndex != NO_SEGMENT_INDEX) {
            segmentIndex = otherSegmentIndex;
        }
        String label = buildAddress(segmentIndex, wAddr, labelAccess, false);
        if (label.length() != 0) {
            return label;
        }
        equate = workspace.getSystemEquateList().findEquateByAddress(wAddr, labelAccess, sdx);
        if (equate != null) {
            return equate.getLabel();
        }
        return Strings.empty();
    }

    public String getUserComment(int segmentIndex, int offset, int size) {
        String result = "";

        Segment segment = segmentList.get(segmentIndex);

        if (size != 0xFFFF) {
            if (size == 0) {
                size = 1;
            }

            int relativeOffset = 0;
            while (relativeOffset < size) { // TODO: Is this correct? (kept as in the C++ source)
                String comment = segment.findComment(offset + relativeOffset);

                if (comment.length() != 0) {
                    if (result.length() == 0) {
                        result = comment;
                    } else {
                        result = result + "\n" + comment;
                    }
                }

                relativeOffset++;
            }
        }

        return result;
    }

    public void setUserComment(int segmentIndex, int offset, int size, String text) {
        if (size != 0xFFFF) {
            Segment segment = segmentList.get(segmentIndex);

            segment.deleteComments(offset, size);
            if (text != null && text.length() != 0) {
                Comment comment = segment.allocateComment();
                comment.setOffset(offset);
                comment.setText(text);
            }
        }
    }

    public void addListener(SegmentListChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    public void beginUpdate() {
        updateCounter++;
    }

    public void endUpdate() {
        if (updateCounter <= 0) {
            throw new IllegalStateException("endUpdate() called without a matching beginUpdate()");
        }
        updateCounter--;
        if (updateCounter == 0) {
            flushEvents();
        }
    }

    public void notifySegmentContentChanged() {
        notifyListeners(Property.SEGMENT_CONTENT);
    }

    private void notifyListeners(Property property) {
        Debug.logValue("SegmentList::NotifyListeners: Property", property.toDisplayString());

        // Add each event only once.
        if (!propertyChangeEvents.contains(property)) {
            propertyChangeEvents.add(property);
            if (updateCounter == 0) {
                flushEvents();
            }
        }
    }

    private void flushEvents() {
        if (propertyChangeEvents.size() == 0) {
            return;
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < propertyChangeEvents.size(); i++) {
            text.append(propertyChangeEvents.get(i).toDisplayString()).append(" ");
        }
        Debug.logValue("SegmentList::FlushEvents: Properties", text.toString());

        // Copy of the events list handed to listeners, and cleared before dispatch, mirroring
        // the C++ source's propertyChangeEvents.clear() at the end of FlushEvents. Listeners
        // are expected to be anonymous inner classes, per the Java 6 event-listener idiom.
        List<Property> eventsForListeners = new ArrayList<Property>(propertyChangeEvents);
        propertyChangeEvents.clear();

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).handleSegmentListChanged(this, eventsForListeners);
        }
    }

    /** Direct translation of SegmentList::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttribute(element, "Count", getCount());

        for (int i = 0; i < segmentList.size(); i++) {
            org.w3c.dom.Element segmentElement = Xml.addChildElement(element, "Segment");
            segmentList.get(i).serializeTo(segmentElement);
        }
    }

    /** Direct translation of SegmentList::DeserializeFrom. */
    public void deserializeFrom(org.w3c.dom.Element element) {
        beginUpdate();
        clear();

        int count = Xml.getWordAttribute(element, "Count", 0);

        if (count > 0) {
            org.w3c.dom.Element segmentElement = Xml.firstChildElement(element, "Segment");

            for (int segmentIndex = 0; (segmentElement != null) && (segmentIndex < count); segmentIndex++) {
                Segment segment = addSegment();
                segment.deserializeFrom(segmentElement);
                segmentElement = Xml.nextSiblingElement(segmentElement, "Segment");
            }

            setSelectedIndex(0);
        }
        endUpdate();
    }
}
