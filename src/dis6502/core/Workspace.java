package dis6502.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of Workspace.h / Workspace.cpp.
 *
 * Owns everything that makes up one open disassembly project: the selected computer system,
 * the output-formatting Profile, the system/user EquateLists, the SegmentList, and a transient
 * (not persisted) DisassemblyResult. Also implements the batched property-change notification
 * system used throughout this codebase (BeginUpdate/EndUpdate/listeners).
 *
 * The C++ class privately inherits from EquateListChangedListener and SegmentListChangedListener
 * so that HandleEquateListChanged/HandleSegmentListChanged are implementation details, not part
 * of the public API. Java has no private inheritance, so this translation instead registers two
 * small anonymous inner class listeners (per the Java 6 event-listener idiom used throughout
 * this port) that delegate to private handleEquateListChanged/handleSegmentListChanged methods,
 * achieving the same encapsulation.
 *
 * Deferred to later passes (once the corresponding layers are translated):
 * - SerializeTo / DeserializeFrom (needs the XML wrapper layer)
 * - GetResizedFont (needs the real ComputerSystem subclasses + Swing font handling)
 * - GetDisassemblyResult's real content (needs the Disassembly engine pass; DisassemblyResult
 *   is currently just a placeholder, see DisassemblyResult.java)
 */
public final class Workspace implements Xml.Serializable {

    public enum Format {
        WORKSPACE14,
        WORKSPACE36
    }

    // Constructor argument.
    private final ComputerSystemFactory computerSystemFactory;

    private String filePath = "";

    private boolean viewDisplayAsScreenCode = false; // MemoryInspector uses internal character set (ANTIC)
    private boolean viewNoDisassembly = false;       // No disassembly launched if byte type is changed
    private boolean viewDoubleHeight = false;        // Double the font height of the display

    private ComputerSystem computerSystem;
    private final InstructionSet instructionSetMOS6502;
    private final InstructionSet instructionSetMOS65C02;
    private final Profile profile;

    private final EquateList systemEquateList;
    private final EquateList userEquateList;
    private final SegmentList segmentList;
    private final DisassemblyResult disassemblyResult;

    // Event handling.
    private int updateCounter = 0;
    private final List<WorkspaceProperty> propertyChangeEvents = new ArrayList<WorkspaceProperty>();
    private final List<WorkspaceChangedListener> listeners = new ArrayList<WorkspaceChangedListener>();

    public Workspace(ComputerSystemFactory computerSystemFactory) {
        this.computerSystemFactory = computerSystemFactory;
        this.computerSystem = computerSystemFactory.getComputerSystem(ComputerSystemType.UNKNOWN);

        instructionSetMOS6502 = new InstructionSetMOS6502("MOS 6502");
        instructionSetMOS65C02 = new InstructionSetMOS65C02("MOS 65C02");
        profile = new Profile();

        systemEquateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
        systemEquateList.addListener(equateListChangedListener);
        userEquateList = new EquateList(WorkspaceProperty.USER_EQUATES);
        userEquateList.addListener(equateListChangedListener);

        segmentList = new SegmentList(this);
        disassemblyResult = new DisassemblyResult();

        init();
        segmentList.addListener(segmentListChangedListener);
    }

    /**
     * Corresponds to the C++ destructor (~Workspace): detaches this Workspace's listeners from
     * the objects it owns and clears everything. Java has no destructors, so callers that want
     * the exact same teardown sequence (e.g. before dropping the last reference) can call this
     * explicitly; it is not required for garbage collection to work correctly.
     */
    public void dispose() {
        removeListeners();
        systemEquateList.removeListeners();
        userEquateList.removeListeners();
        segmentList.removeListeners();
        init();
    }

    public void init() {
        beginUpdate();
        setFilePath("");
        segmentList.clear();
        systemEquateList.clear();
        userEquateList.clear();
        profile.clear();

        viewDisplayAsScreenCode = false;
        viewNoDisassembly = false;
        viewDoubleHeight = true;
        endUpdate();
    }

    public ComputerSystemFactory getComputerSystemFactory() {
        return computerSystemFactory;
    }

    public boolean isViewDisplayAsScreenCode() {
        return viewDisplayAsScreenCode;
    }

    public void setViewDisplayAsScreenCode(boolean value) {
        viewDisplayAsScreenCode = value;
    }

    public boolean isViewNoDisassembly() {
        return viewNoDisassembly;
    }

    public void setViewNoDisassembly(boolean value) {
        viewNoDisassembly = value;
    }

    public boolean isViewDoubleHeight() {
        return viewDoubleHeight;
    }

    public void setViewDoubleHeight(boolean value) {
        viewDoubleHeight = value;
    }

    // --- File. ---

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        notifyListeners(WorkspaceProperty.FILE_PATH);
    }

    // --- Font. ---

    /** Deferred: see the class Javadoc. Throws until the real ComputerSystem subclasses exist. */
    public Object getResizedFont() {
        return computerSystem.getFont(viewDoubleHeight);
    }

    public int getResizedFontWidth() {
        return 8;
    }

    public int getResizedFontHeight() {
        return viewDoubleHeight ? 16 : 8;
    }

    // --- Computer System. ---

    public void setComputerSystemTypeID(String id) {
        ComputerSystemType computerSystemType = computerSystemFactory.getComputerSystemType(id);
        if (computerSystemType == ComputerSystemType.UNKNOWN) {
            computerSystemType = ComputerSystemType.ATARI800;
        }
        setComputerSystemType(computerSystemType);
    }

    public void setComputerSystemType(ComputerSystemType computerSystemType) {
        if (computerSystem == null || computerSystem.getType() != computerSystemType) {
            computerSystem = computerSystemFactory.getComputerSystem(computerSystemType);
            notifyListeners(WorkspaceProperty.COMPUTER_SYSTEM_TYPE);
            notifyListeners(WorkspaceProperty.FONT);
        }
    }

    public ComputerSystem getComputerSystem() {
        return computerSystem;
    }

    // --- Computer System: InstructionSet. ---

    public InstructionSet getInstructionSet(ProcessorType processorType) {
        switch (processorType) {
            case MOS6502:
                return instructionSetMOS6502;
            case MOS65C02:
                return instructionSetMOS65C02;
            default:
                throw new RuntimeException("Invalid processor type");
        }
    }

    // --- Profile. ---

    public Profile getProfile() {
        return profile;
    }

    public Profile getConstProfile() {
        return profile;
    }

    // --- Equates. ---

    public EquateList getSystemEquateList() {
        return systemEquateList;
    }

    public EquateList getConstUserEquateList() {
        return userEquateList;
    }

    public EquateList getUserEquateList() {
        return userEquateList;
    }

    // --- Memory Segments. ---

    public SegmentList getSegmentList() {
        return segmentList;
    }

    public SegmentList getConstSegmentList() {
        return segmentList;
    }

    public Segment getConstSegment(int segmentIndex) {
        return segmentList.getConstSegment(segmentIndex);
    }

    // --- Notification. ---

    public void notifyFontChanged() {
        notifyListeners(WorkspaceProperty.FONT);
    }

    public void notifyProfileChanged() {
        notifyListeners(WorkspaceProperty.PROFILE);
    }

    public void notifySelectedMemoryRangeChanged() {
        notifyListeners(WorkspaceProperty.SELECTED_MEMORY_RANGE);
    }

    public void addListener(WorkspaceChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    public void beginUpdate() {
        updateCounter++;
    }

    public void endUpdate() {
        updateCounter--;
        if (updateCounter == 0) {
            flushEvents();
        }
    }

    // NOTE: SerializeTo(XML::Element&) / DeserializeFrom(const XML::Element&) are deferred --
    // they depend on the XML wrapper layer, which has not been translated yet.

    // --- Find equates in the user equate list, then the system equate list. ---

    public Equate findEquateByAddress(int address, LabelAccess labelAccess) {
        // Address of 0 is not allowed as a label. It is the NO_LABEL value!
        if (address == 0) {
            return null;
        }

        Equate equate = userEquateList.findEquateByAddress(address, labelAccess, true);
        if (equate == null) {
            equate = systemEquateList.findEquateByAddress(address, labelAccess, true);
        }
        return equate;
    }

    public Equate getEquateByLabel(String label) {
        Equate equate = userEquateList.getEquateByLabel(label);
        if (equate == null) {
            equate = systemEquateList.getEquateByLabel(label);
        }
        return equate;
    }

    /** Find the SDX symbol for this PC address. Returns null if there is none. */
    public String findSymbolByAddress(int segmentIndex, int address, int addressOffset) {
        Symbol symbol = getConstSegment(segmentIndex).findSymbol(address);

        if (symbol != null) {
            // The C++ source builds this into a fixed-size shared static buffer
            // (wcsncpy/wcsncat into a "L1234567+32767"-sized wchar_t[]); Java strings have no
            // such length limit to worry about, so this just concatenates directly.
            return symbol.getSymbol() + Memory.addressOffsetToString(addressOffset);
        }

        return null;
    }

    /** Sets (clears) the flag for all labels. Does not fire a NotifyXChanged event. */
    public void clearEquateFlags() {
        systemEquateList.clearFlags();
        userEquateList.clearFlags();
    }

    // --- Disassembly result, transient and not saved as part of the workspace itself. ---

    public DisassemblyResult getDisassemblyResult() {
        return disassemblyResult;
    }

    public DisassemblyResult getConstDisassemblyResult() {
        return disassemblyResult;
    }

    /** Direct translation of Workspace::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setStringAttribute(element, "ComputerSystemTypeID", computerSystem.getTypeInfo().getId());

        org.w3c.dom.Element profileElement = Xml.addChildElement(element, "Profile");
        profile.serializeTo(profileElement);

        org.w3c.dom.Element systemEquatesElement = Xml.addChildElement(element, "SystemEquates");
        systemEquateList.serializeTo(systemEquatesElement);

        org.w3c.dom.Element userEquatesElement = Xml.addChildElement(element, "UserEquates");
        userEquateList.serializeTo(userEquatesElement);

        org.w3c.dom.Element segmentsListElement = Xml.addChildElement(element, "Segments");
        segmentList.serializeTo(segmentsListElement);
    }

    /** Direct translation of Workspace::DeserializeFrom. */
    public void deserializeFrom(org.w3c.dom.Element element) {
        init();

        String computerSystemTypeID = Xml.getStringAttribute(element, "ComputerSystemTypeID", "");
        setComputerSystemTypeID(computerSystemTypeID);

        org.w3c.dom.Element profileElement = Xml.firstChildElement(element, "Profile");
        if (profileElement != null) {
            profile.deserializeFrom(profileElement);
            notifyProfileChanged();
        }

        org.w3c.dom.Element systemEquatesElement = Xml.firstChildElement(element, "SystemEquates");
        if (systemEquatesElement != null) {
            systemEquateList.deserializeFrom(systemEquatesElement); // This will notify listeners.
        }

        org.w3c.dom.Element userEquatesElement = Xml.firstChildElement(element, "UserEquates");
        if (userEquatesElement != null) {
            userEquateList.deserializeFrom(userEquatesElement); // This will notify listeners.
        }

        org.w3c.dom.Element segmentsListElement = Xml.firstChildElement(element, "Segments");
        if (segmentsListElement != null) {
            segmentList.deserializeFrom(segmentsListElement);
        }
    }

    /**
     * Convenience wrapper (not a direct C++ translation -- in the original, the equivalent call
     * sites live in the not-yet-ported WorkspaceLogic/UI-controller layer, e.g.
     * "XML::Load(workspace, L\"Workspace\", filePath)") around Xml.load/Xml.save with this
     * project's root element name, "Workspace".
     */
    public void load(String filePath) throws java.io.IOException {
        Xml.load(this, "Workspace", filePath);
        setFilePath(filePath);
    }

    public void save(String filePath) throws java.io.IOException {
        Xml.save(this, "Workspace", filePath);
        setFilePath(filePath);
    }

    // --- Private event plumbing. ---

    private void notifyListeners(WorkspaceProperty property) {
        Debug.logValue("Workspace::NotifyListeners: Property", property.toDisplayString());

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
        Debug.logValue("Workspace::FlushEvents: Properties", text.toString());

        // Create a local copy of the events to ensure recursion works. The assumption is that
        // the listeners do not change during the recursion, same as the C++ source.
        List<WorkspaceProperty> propertyChangeEventsCopy = new ArrayList<WorkspaceProperty>(propertyChangeEvents);
        propertyChangeEvents.clear();

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).handleWorkspaceChanged(this, propertyChangeEventsCopy);
        }
    }

    private void handleEquateListChanged(EquateList equateList, WorkspaceProperty workspaceProperty) {
        notifyListeners(workspaceProperty);
    }

    private void handleSegmentListChanged(SegmentList segmentList, List<SegmentList.Property> propertyChangeEvents) {
        beginUpdate();
        for (int i = 0; i < propertyChangeEvents.size(); i++) {
            SegmentList.Property property = propertyChangeEvents.get(i);
            switch (property) {
                case SEGMENTS:
                case SEGMENT_CONTENT:
                    notifyListeners(WorkspaceProperty.SEGMENTS);
                    break;
                case SELECTED_INDEX:
                    notifyListeners(WorkspaceProperty.SELECTED_SEGMENT);
                    break;
                default:
                    break;
            }
        }
        endUpdate();
    }

    // Anonymous inner class listeners: these stand in for the C++ source's private inheritance
    // from EquateListChangedListener / SegmentListChangedListener, keeping the handler methods
    // out of Workspace's public API while still satisfying the listener interfaces.

    private final EquateListChangedListener equateListChangedListener = new EquateListChangedListener() {
        public void handleEquateListChanged(EquateList equateList, WorkspaceProperty workspaceProperty) {
            Workspace.this.handleEquateListChanged(equateList, workspaceProperty);
        }
    };

    private final SegmentListChangedListener segmentListChangedListener = new SegmentListChangedListener() {
        public void handleSegmentListChanged(SegmentList segmentList, List<SegmentList.Property> propertyChangeEvents) {
            Workspace.this.handleSegmentListChanged(segmentList, propertyChangeEvents);
        }
    };
}
