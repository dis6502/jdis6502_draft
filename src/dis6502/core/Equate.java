package dis6502.core;

/**
 * Direct Java 6 translation of Equate.h / Equate.cpp.
 *
 * An Equate is either an empty line, a comment line, or a label definition line from a user-
 * or system-equates definition file (e.g. "RTCLOK = $0014" or "RTCLOK+1 < $0015"). Instances
 * are only ever created by EquateList (mirroring the C++ "friend EquateList" + private
 * constructor); the constructor and init() are therefore package-private.
 *
 * XML serialization (SerializeTo/DeserializeFrom) is deferred to the persistence-layer pass,
 * once the XML wrapper classes have been translated.
 */
public final class Equate {

    public static final int NO_LABEL_VALUE = 0; // Address of 0 is not allowed as a label value.

    private EquateType equateType;
    private String label;
    private LabelAccess labelAccess; // Supported types of access
    private int labelValue;          // word (0-65535)
    private String comment;

    private String baseLabel;                    // Transient
    private boolean defined;                     // Transient
    private LabelAccess referencedLabelAccess;    // Transient, referenced types of access

    /** Package-private: only EquateList may construct an Equate, as in the C++ source. */
    Equate() {
        this.baseLabel = "";
        clear();
    }

    private void clear() {
        equateType = EquateType.UNKNOWN;
        label = "";
        labelAccess = LabelAccess.UNKNOWN;
        labelValue = 0;
        comment = "";

        initTransientFields();
    }

    private void initTransientFields() {
        // Transient information.
        switch (equateType) {
            case UNKNOWN:
            case EMPTY:
            case COMMENT:
                if (label.length() != 0) {
                    throw new RuntimeException("Label specified");
                }
                break;

            case LABEL: {
                if (labelAccess == LabelAccess.UNKNOWN) {
                    throw new RuntimeException("Invalid access");
                }
                if (label.length() == 0) {
                    throw new RuntimeException("No label specified");
                }

                // Faithful translation of the C++ source: it looks for '+' first, and only
                // falls back to looking for '-' if no '+' was found at all (not "whichever
                // of + or - comes first"), so e.g. "AB+CD-EF" bases itself at the '+'.
                int index = label.indexOf('+');
                if (index < 0) {
                    index = label.indexOf('-');
                }
                if (index >= 0) {
                    this.baseLabel = label.substring(0, index);
                }
                break;
            }

            default:
                throw new RuntimeException("Invalid equate type");
        }

        defined = false;
        referencedLabelAccess = LabelAccess.UNKNOWN;
    }

    /** Package-private: only EquateList may (re-)initialize an Equate, as in the C++ source. */
    void init(EquateType equateType, String label, LabelAccess labelAccess, int labelValue, String comment) {
        this.equateType = equateType;
        this.label = label;
        this.labelAccess = labelAccess;
        this.labelValue = labelValue;
        this.comment = comment;

        initTransientFields();
    }

    public EquateType getType() {
        return equateType;
    }

    public String getLabel() {
        return label;
    }

    public boolean equalsLabel(String label) {
        return this.label.equals(label);
    }

    public String getBaseLabel() {
        return baseLabel;
    }

    public boolean isRange() {
        return baseLabel.length() != 0;
    }

    public LabelAccess getLabelAccess() {
        return labelAccess;
    }

    private static boolean isLabelAccessSupported(LabelAccess supportedLabelAccess, LabelAccess labelAccess) {
        int a = supportedLabelAccess.getValue();
        int b = labelAccess.getValue();
        return (a & b) != 0;
    }

    public boolean isLabelAccessSupported(LabelAccess labelAccess) {
        return isLabelAccessSupported(this.labelAccess, labelAccess);
    }

    public int getLabelValue() {
        return labelValue;
    }

    public String getComment() {
        return comment;
    }

    public void clearDefinition() {
        defined = false;
    }

    public boolean hasDefinition() {
        return defined;
    }

    public void addDefinition() {
        defined = true;
    }

    public void clearReferences() {
        referencedLabelAccess = LabelAccess.UNKNOWN;
    }

    public void addLabelReference(LabelAccess labelAccess) {
        int combined = referencedLabelAccess.getValue() | labelAccess.getValue();
        referencedLabelAccess = labelAccessFromValue(combined);
        // TODO (same as the C++ source): log which access type was added, once a logging
        // facility with ToString() support for this is available.
    }

    public LabelAccess getReferencedLabelAccess() {
        return referencedLabelAccess;
    }

    public boolean hasReferences() {
        return referencedLabelAccess != LabelAccess.UNKNOWN;
    }

    public boolean hasReferencedLabelAccess(LabelAccess labelAccess) {
        return isLabelAccessSupported(this.referencedLabelAccess, labelAccess);
    }

    private static LabelAccess labelAccessFromValue(int value) {
        LabelAccess[] all = LabelAccess.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getValue() == value) {
                return all[i];
            }
        }
        return LabelAccess.UNKNOWN;
    }

    /**
     * Extracts the 4-character hexadecimal address from an automatic label name "Lnnnn",
     * or "" otherwise. Direct translation of Equate::ExtractAddress. Note the original (and
     * this translation) only accepts uppercase hex digits (0-9, A-F) -- lowercase a-f is
     * rejected, since automatic labels are always generated in uppercase.
     */
    public static String extractAddress(String label) {
        if (label.length() < 5) {
            return "";
        }
        int start = label.length() - 5;
        if (label.charAt(start) != 'L') {
            return "";
        }
        start++;
        for (int i = start; i < start + 4; i++) {
            char c = label.charAt(i);
            if (((c < '0') || (c > '9')) && ((c < 'A') || (c > 'F'))) {
                return "";
            }
        }
        return label.substring(start);
    }

    public static boolean isAutomaticLabel(String label) {
        return extractAddress(label).length() != 0;
    }

    public static boolean isLabelWithOffset(String label) {
        return label.indexOf('+') >= 0 || label.indexOf('-') >= 0;
    }

    /**
     * Direct translation of "bool Equate::SkipBlanks(wstring_view string, size_t& index)".
     * indexHolder[0] is both input and output (the C++ signature takes index by reference).
     * Returns true if the end of the string was reached while skipping.
     */
    private static boolean skipBlanks(String string, int[] indexHolder) {
        int index = indexHolder[0];
        while (index < string.length() && Characters.isSpace(string.charAt(index))) {
            index++;
        }
        indexHolder[0] = index;
        return index == string.length();
    }

    /** Result holder for the many out-parameters of Equate::ReadFrom. */
    public static final class ReadResult {
        public EquateType equateType = EquateType.UNKNOWN;
        public String label = "";
        public LabelAccess labelAccess = LabelAccess.UNKNOWN;
        public int address = 0;
        public String comment = "";
        public String error = ""; // Empty means no error.
    }

    /**
     * Direct translation of Equate::ReadFrom: parses one line of an equate definition file,
     * e.g. "RTCLOK = $0014", "RTCLOK+1 < $0015 ; comment", or a "; comment"-only line, or a
     * blank line.
     */
    public static ReadResult readFrom(String line) {
        ReadResult result = new ReadResult();

        int[] indexHolder = new int[] { 0 };

        if (skipBlanks(line, indexHolder)) {
            result.equateType = EquateType.EMPTY;
            return result;
        }
        int index = indexHolder[0];

        // If we have a comment line, we add it to the disassembly listing.
        if (line.charAt(index) == ';') {
            result.equateType = EquateType.COMMENT;

            // Skip blanks.
            index++;
            indexHolder[0] = index;
            if (!skipBlanks(line, indexHolder)) {
                result.comment = Strings.trim(line.substring(indexHolder[0]));
            }
            return result;
        }

        // We must have a label name starting with a letter or "_".
        char c = line.charAt(index);
        // NOTE: faithfully reproduces the C++ source's condition, which looks inverted versus
        // its own comment above -- as written, it *rejects* labels starting with '_' (the
        // comment claims '_' should be allowed). Preserved here rather than "fixed", per the
        // instruction to translate strictly and flag such discrepancies rather than silently
        // changing behavior.
        if (!Characters.isAlpha(c) || c == '_') {
            result.error = "Character '" + c + "' at position " + index + " is not a valid start character for a label name.";
            return result;
        }
        result.equateType = EquateType.LABEL;

        // Compose the label name. Label names can contain offsets, e.g. "RTCLOK+1 = $13".
        StringBuilder label = new StringBuilder();
        while ((index < line.length()) && (Characters.isAlphaNumeric(c) || (c == '_') || (c == '+') || (c == '-'))) {
            label.append(c);
            index++;
            if (index < line.length()) {
                c = line.charAt(index);
            }
        }
        result.label = label.toString();

        indexHolder[0] = index;
        if (skipBlanks(line, indexHolder)) {
            result.error = "No access qualifier specified.";
            return result;
        }
        index = indexHolder[0];

        c = line.charAt(index);
        // Now we must have an equal (=, <, > or #) sign.
        switch (c) {
            case '=':
                result.labelAccess = LabelAccess.READ_WRITE;
                break;
            case '<':
                result.labelAccess = LabelAccess.READ;
                break;
            case '>':
                result.labelAccess = LabelAccess.WRITE;
                break;
            case '#':
                result.labelAccess = LabelAccess.IMMEDIATE;
                break;
            default:
                result.error = "Character '" + c + "' at position " + (index + 1) + " is not an access qualifier. Use '=', '<', '>' or '#'.";
                return result;
        }
        index++;

        // Skip blanks.
        indexHolder[0] = index;
        if (skipBlanks(line, indexHolder)) {
            result.error = "No value specified.";
            return result;
        }
        index = indexHolder[0];

        // Now we must have an address (hex or decimal).
        int count;
        if (line.charAt(index) == '$') {
            index++;
            int[] parsed = scanUnsignedShort(line, index, 16);
            count = parsed[1];
            if (count == 0) {
                result.error = "Characters '" + line.substring(index) + "' at position " + (index + 1)
                        + " cannot be interpreted as a hexadecimal number.";
                return result;
            }
            result.address = parsed[0];
        } else {
            int[] parsed = scanUnsignedShort(line, index, 10);
            count = parsed[1];
            if (count == 0) {
                result.error = "Characters '" + line.substring(index) + "' at position " + (index + 1)
                        + " cannot be interpreted as a decimal number.";
                return result;
            }
            result.address = parsed[0];
        }
        index += count;

        // Skip blanks.
        indexHolder[0] = index;
        if (skipBlanks(line, indexHolder)) {
            return result; // Success, no comment.
        }
        index = indexHolder[0];

        // Scan for a line comment.
        c = line.charAt(index);
        if (c == ';') {
            index++;
            indexHolder[0] = index;
            if (!skipBlanks(line, indexHolder)) {
                result.comment = Strings.trim(line.substring(indexHolder[0]));
            }
            return result; // Success.
        }

        result.error = "Invalid character '" + c + "' after value found. Line end or comment expected";
        return result;
    }

    /**
     * Direct translation of the swscanf(p, L"%hx%n", ...) / swscanf(p, L"%hu%n", ...) calls
     * used by ReadFrom: scans as many hex (radix 16) or decimal (radix 10) digits as possible
     * starting at "start", masks the parsed value to an unsigned short (0-65535, matching the
     * C++ "Memory::address"), and returns {value, digitsConsumed}. digitsConsumed is 0 if no
     * digit was found at all, matching swscanf's "0 items matched" return value.
     */
    private static int[] scanUnsignedShort(String line, int start, int radix) {
        int end = start;
        while (end < line.length() && Character.digit(line.charAt(end), radix) >= 0) {
            end++;
        }
        if (end == start) {
            return new int[] { 0, 0 };
        }
        long value = Long.parseLong(line.substring(start, end), radix);
        return new int[] { (int) (value & 0xFFFF), end - start };
    }

    /**
     * Direct translation of Equate::ToString(). The C++ version builds the text into a shared
     * static wchar_t buffer via wsprintf and returns it via String::Format(); this translation
     * uses String.format directly, since Java has no global-buffer idiom (and doesn't need
     * one).
     */
    public String toDisplayString() {
        switch (equateType) {
            case EMPTY:
                return "";

            case COMMENT:
                return String.format("; %s", comment);

            case LABEL:
                if (comment.length() != 0) {
                    return String.format("%s %s $%04X; %s", label,
                            LabelAccess.LabelAccessFactory.getQualifier(labelAccess), labelValue, comment);
                } else {
                    return String.format("%s %s $%04X", label,
                            LabelAccess.LabelAccessFactory.getQualifier(labelAccess), labelValue);
                }

            default:
                throw new RuntimeException("Unsupported equate type");
        }
    }

    /** Direct translation of Equate::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setStringAttribute(element, "EquateType", EquateType.EquateTypeFactory.getInfo(equateType).getKey());

        if (equateType == EquateType.UNKNOWN) {
            throw new RuntimeException("Invalid equate type");
        } else if (equateType == EquateType.EMPTY) {
            // Nothing more to write.
        } else if (equateType == EquateType.COMMENT) {
            Xml.setStringAttribute(element, "Comment", comment);
        } else if (equateType == EquateType.LABEL) {
            Xml.setStringAttribute(element, "Label", label);
            Xml.setStringAttribute(element, "LabelAccess", LabelAccess.LabelAccessFactory.getInfo(labelAccess).getKey());
            if (labelValue < 0x100) {
                Xml.setByteAttributeHex(element, "LabelValue", labelValue);
            } else {
                Xml.setWordAttributeHex(element, "LabelValue", labelValue);
            }
            if (comment.length() != 0) {
                Xml.setStringAttribute(element, "Comment", comment);
            }
        }
    }

    /** Direct translation of Equate::DeserializeFrom. */
    public void deserializeFrom(org.w3c.dom.Element element) {
        clear();

        String equateTypeString = Xml.getStringAttribute(element, "EquateType", "");
        equateType = EquateType.EquateTypeFactory.getInfo(equateTypeString).getEquateType();
        label = Xml.getStringAttribute(element, "Label", label);
        String labelAccessString = Xml.getStringAttribute(element, "LabelAccess", "");
        labelAccess = LabelAccess.LabelAccessFactory.getInfo(labelAccessString).getLabelAccess();
        labelValue = Xml.getWordAttribute(element, "LabelValue", labelValue);
        comment = Xml.getStringAttribute(element, "Comment", comment);
        initTransientFields();
    }
}
