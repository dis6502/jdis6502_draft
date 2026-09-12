package dis6502.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Direct Java 6 translation of EquateList.h / EquateList.cpp.
 *
 * Holds an ordered list of Equates (label/comment/empty-line entries) for either the user
 * equates or the system equates of a Workspace (WorkspaceProperty.USER_EQUATES /
 * SYSTEM_EQUATES), plus a small set of transient per-equate flags (defined / referenced)
 * used while generating a disassembly listing.
 *
 * Deferred to later passes (once the corresponding layers are translated):
 * - SerializeTo / DeserializeFrom (needs the XML wrapper layer)
 * - Load1X / Save1X (needs the Workspace1X binary format + InputStream/OutputStream)
 * - Load(filePath) / Save(filePath, xasm) (needs FileIO + the Application message/log layer)
 *
 * The C++ AddEquate(wstring_view line) reports parse errors via a global g_Application
 * message; since that layer isn't translated yet, this version routes parse errors to
 * Debug.log(...) instead (Debug is already a real, working translation) and still returns
 * null, matching the original's return-null-on-error contract.
 */
public final class EquateList {

    private final WorkspaceProperty property;
    private final List<Equate> equateList = new ArrayList<Equate>();

    // Event handling.
    private final List<EquateListChangedListener> listeners = new ArrayList<EquateListChangedListener>();

    public EquateList(WorkspaceProperty property) {
        this.property = property;
    }

    public WorkspaceProperty getProperty() {
        return property;
    }

    public List<Equate> getEquates() {
        return Collections.unmodifiableList(equateList);
    }

    public void addListener(EquateListChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    private void notifyListeners() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).handleEquateListChanged(this, property);
        }
    }

    public void clear() {
        equateList.clear();
        notifyListeners();
    }

    public boolean isEmpty() {
        return equateList.isEmpty();
    }

    public int getCount() {
        return equateList.size();
    }

    public int getLabelCount() {
        int result = 0;
        for (int i = 0; i < equateList.size(); i++) {
            if (equateList.get(i).getType() == EquateType.LABEL) {
                result++;
            }
        }
        return result;
    }

    /** Adds a fresh Equate without firing events. Direct translation of the private AddEquate(). */
    private Equate addEquateInternal() {
        Equate equate = new Equate();
        equateList.add(equate);
        return equate;
    }

    /**
     * Direct translation of EquateList::FindEquateByAddress(address, access, sdx).
     * Note: matching equates additionally get a label reference recorded for "access", same
     * as the C++ source (this method has a side effect despite the "Find" name).
     */
    public Equate findEquateByAddress(int address, LabelAccess access, boolean sdx) {
        // Address of 0 is not allowed as a label. It is the NO_LABEL value!
        if (address == 0) {
            return null;
        }

        // Do not use SDX page 7 for non-SDX segments.
        // TODO FIXME (same as the C++ source): mark system equates properly instead of
        // hardcoding page 7 here, but right now it does the job.
        if ((!sdx) && (address >= 0x0700) && (address <= 0x07FF)) {
            return null;
        }

        for (int i = 0; i < equateList.size(); i++) {
            Equate equate = equateList.get(i);
            if ((equate.getLabelValue() == address) && equate.isLabelAccessSupported(access)) {
                equate.addLabelReference(access);
                return equate;
            }
        }
        return null;
    }

    public Equate findAndMarkEquateByAddress(int address, LabelAccess access) {
        Equate equate = findEquateByAddress(address, access, true);
        if (equate != null) {
            equate.addDefinition();
        }
        return equate;
    }

    public Equate getEquateByLabel(String label) {
        for (int i = 0; i < equateList.size(); i++) {
            Equate equate = equateList.get(i);
            if (equate.equalsLabel(label)) {
                return equate;
            }
        }
        return null;
    }

    public void setRange(String label, int labelAddress, int startAddress, int endAddress) {
        removeRange(startAddress, endAddress);
        addRange(label, labelAddress, startAddress, endAddress);
        notifyListeners();
    }

    // TODO (same as the C++ source): Add labelAccess as a parameter.
    private void addRange(String label, int labelAddress, int startAddress, int endAddress) {
        for (int address = startAddress; address <= endAddress; address++) {
            String sign = (address > labelAddress) ? "+" : "-";
            int distance = Math.abs(address - labelAddress);
            // Note: the C++ source builds this same string via a wsprintf call that passes
            // narrow "+"/"-" string literals into a %s placeholder of a wide-character format
            // string -- a type mismatch that's an artifact of the Windows wsprintf API rather
            // than meaningful behavior, so this translation just builds the intended text
            // directly instead of reproducing that mismatch.
            String generatedLabel = label + sign + "$" + String.format("%04X", distance);
            Equate equate = addEquateInternal();
            equate.init(EquateType.LABEL, generatedLabel, LabelAccess.READ_WRITE, address, "");
        }
    }

    private void removeRange(int startAddress, int endAddress) {
        // Note: the C++ source removes matching entries with
        // "for (auto i = ...; i != end; i++) { ... equateList.erase(i); }", which invalidates
        // the iterator on erase() and then still increments it -- undefined behavior in C++,
        // and not something meaningful to reproduce in Java (it would just be a
        // ConcurrentModificationException or arbitrarily-skipped entries). This translation
        // implements the evidently-intended behavior instead: remove every equate whose label
        // value falls within [startAddress, endAddress].
        java.util.Iterator<Equate> it = equateList.iterator();
        while (it.hasNext()) {
            Equate equate = it.next();
            int labelAddress = equate.getLabelValue();
            if ((labelAddress >= startAddress) && (labelAddress <= endAddress)) {
                it.remove();
            }
        }
    }

    /** Sets (clears) the flags for all labels. */
    public void clearFlags() {
        for (int i = 0; i < equateList.size(); i++) {
            Equate equate = equateList.get(i);
            equate.clearDefinition();
            equate.clearReferences();
        }
    }

    /**
     * Detects labels of the form EXAMPLE+$xxxx. When found, label EXAMPLE is marked as
     * referenced. TODO (same as the C++ source): consider a parent equate list, and recursion.
     */
    public void setBaseLabelsReferenced() {
        for (int i = 0; i < equateList.size(); i++) {
            Equate equate = equateList.get(i);
            LabelAccess referencedAccess = equate.getReferencedLabelAccess();
            if (equate.isRange() && referencedAccess != LabelAccess.UNKNOWN) {
                Equate baseEquate = getEquateByLabel(equate.getBaseLabel());
                // Be tolerant of inconsistent label definitions.
                if (baseEquate != null) {
                    baseEquate.addLabelReference(referencedAccess);
                }
            }
        }
    }

    /** Checks if a label is referenced with a certain type of access. */
    public boolean isEquateAddressReferenced(int address, LabelAccess labelAccess) {
        if (address == 0) {
            return false;
        }

        for (int i = 0; i < equateList.size(); i++) {
            Equate equate = equateList.get(i);
            if (equate.getLabelValue() == address && equate.hasReferencedLabelAccess(labelAccess)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses an equate from a line in a definition file. Returns null (and logs a message via
     * Debug.log, see the class Javadoc) if the line cannot be parsed.
     */
    public Equate addEquate(String line) {
        Equate.ReadResult parsed = Equate.readFrom(line);

        if (parsed.error.length() != 0) {
            // Substitute for: g_Application->SendErrorMessageWithID(IDS_ERR_CANNOT_PARSE_EQUATE_LINE, line, errorString);
            Debug.log("Cannot parse equate line '" + line + "'. Error: " + parsed.error);
            return null;
        }

        if (parsed.equateType == EquateType.UNKNOWN) {
            return null;
        }
        Equate equate = addEquateInternal();
        equate.init(parsed.equateType, parsed.label, parsed.labelAccess, parsed.address, parsed.comment);
        return equate;
    }

    // NOTE: Load1X(InputStream&) / Save1X(OutputStream&) (the old Workspace14/Workspace1X
    // binary format) and SerializeTo/DeserializeFrom's use from a Workspace1X file are deferred
    // -- that legacy binary format has no active callers in this translation. The current XML
    // format (SerializeTo/DeserializeFrom below) and the plain-text equates file format
    // (Load/Save below) are both implemented.

    /** Direct translation of EquateList::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttribute(element, "Count", getCount());

        for (int i = 0; i < equateList.size(); i++) {
            org.w3c.dom.Element equateElement = Xml.addChildElement(element, "Equate");
            equateList.get(i).serializeTo(equateElement);
        }
    }

    /**
     * Direct translation of EquateList::DeserializeFrom.
     *
     * Faithfully preserves a bug in the C++ source: each equate is deserialized from the
     * *parent* "element" (this EquateList's own &lt;EquateList&gt; element) rather than from
     * its own &lt;Equate&gt; child element ("equate->DeserializeFrom(element)" instead of
     * "equate->DeserializeFrom(*equateElement)"). Since &lt;EquateList&gt; itself never carries
     * Equate/Label/EquateType attributes, every deserialized Equate ends up with
     * equateType == EquateType.UNKNOWN and an empty label -- i.e. loading equates back out of a
     * saved workspace appears to be broken in the original application too. This is preserved
     * as-is rather than silently fixed, per the instruction to translate the code faithfully;
     * see DisassemblyPersistenceIntegrationTest for a test that documents this exact behavior.
     */
    public void deserializeFrom(org.w3c.dom.Element element) {
        // BeginUpdate(); // Commented out in the C++ source too.

        clear();

        int count = Xml.getWordAttribute(element, "Count", 0);

        if (count > 0) {
            // Unfiltered first child (matches element.FirstChildElement(), no tag-name filter),
            // then name-filtered siblings (matches equateElement->NextSiblingElement("Equate")).
            org.w3c.dom.Element equateElement = Xml.firstChildElement(element, null);

            for (int equateIndex = 0; (equateElement != null) && (equateIndex < count); equateIndex++) {
                Equate equate = addEquateInternal();
                equate.deserializeFrom(element); // Bug preserved -- see the method Javadoc.
                equateElement = Xml.nextSiblingElement(equateElement, "Equate");
            }
        }

        // EndUpdate(); // Commented out in the C++ source too.
    }

    /**
     * Direct translation of EquateList::Load(filePath): reads a plain-text equates definition
     * file, one Equate per line (via addEquate/Equate::readFrom). g_Application info messages
     * are routed through Debug.log, per the class Javadoc.
     */
    public boolean load(String filePath) throws java.io.IOException {
        clear();

        Debug.log("Opening equate file: " + filePath);

        java.util.List<String> lines = FileIO.readStrings(filePath);
        for (int i = 0; i < lines.size(); i++) {
            addEquate(lines.get(i));
        }

        Debug.log(getCount() + " equate lines with " + getLabelCount() + " labels loaded.");

        notifyListeners();
        return true;
    }

    /**
     * Direct translation of EquateList::Save(filePath, xasm): writes the equates either as this
     * project's own plain-text equate-definition format (xasm == false, one Equate::ToString()
     * per line), or as an xasm-compatible label-table listing (xasm == true).
     */
    public void save(String filePath, boolean xasm) {
        Debug.log("Saving equate file: " + filePath);

        try {
            OutputStream outputStream;
            if (!xasm) {
                outputStream = OutputStream.openFile(filePath, Encoding.UTF8);
                try {
                    // Faithful translation of a likely bug in the C++ source: no newline is
                    // written between equates here (unlike DisassemblyResultWriter, which does
                    // use Encoding's newline field) -- saved equates run together on one line.
                    // Preserved as-is.
                    for (int i = 0; i < equateList.size(); i++) {
                        outputStream.writeString(equateList.get(i).toDisplayString());
                    }
                } finally {
                    outputStream.close();
                }
            } else {
                StringBuilder table = new StringBuilder();
                for (int i = 0; i < equateList.size(); i++) {
                    Equate equate = equateList.get(i);
                    table.append("        ").append(DatatypeUtility.wordToHexString(equate.getLabelValue(), false))
                            .append(" ").append(equate.getLabel()).append("\n");
                }
                outputStream = OutputStream.openFile(filePath, Encoding.ASCII);
                try {
                    outputStream.writeString("xasm 3.0.0\nLabel table:\n");
                    outputStream.writeString(table.toString());
                } finally {
                    outputStream.close();
                }
            }
        } catch (java.io.IOException e) {
            Debug.log("Error saving equate file '" + filePath + "': " + e.getMessage());
        }
    }
}
