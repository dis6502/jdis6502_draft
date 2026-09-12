package dis6502.core;

/**
 * Direct Java 6 translation of Profile.h / Profile.cpp.
 *
 * Holds all of the user-configurable disassembly output formatting settings (directive
 * spelling, comment prefix, number/string formatting, etc). Fields are public, matching the
 * C++ class's public data members (kept that way so other classes reaching into
 * profile.directiveBYTE etc, as the C++ source does throughout, translate directly).
 *
 * XML serialization (SerializeTo/DeserializeFrom) is deferred to the persistence-layer pass,
 * once the XML wrapper classes have been translated.
 */
public final class Profile implements Xml.Serializable {

    /* Source Layout */
    public boolean useLineNumbers; // since 1.0
    public boolean alignInstructions; // since 1.0
    public boolean showLowerCaseInstructions; // since 1.6

    /* Opcodes */
    public boolean useIllegalOpcodes; // since 1.0
    public boolean showAInAccumulatorMode; // since 1.0
    public boolean showColonAfterLabel; // since 1.6
    public boolean showOpcodeAsComment; // since 3.0
    public boolean showBRKAsByte0; // since 1.6
    public boolean showZPAbsoluteAsByte; // since 1.7
    public String directiveForceAbsolute; // since 3.6

    /* Comments */
    public String commentPrefix; // since 1.0

    /* Numbers */
    public boolean useHexNotation; // since 1.0
    public String hexNotationPrefix; // since 1.0

    /* Strings */
    public boolean showNonASCIIChararactersAsBytes; // since 3.0
    public String quoteForASCIIStrings; // since 3.0, string where only the first character is used

    /* Directives */
    public String directiveLOWHead; // since 1.0
    public String directiveLOWTail; // since 1.0
    public String directiveHIGHHead; // since 1.0
    public String directiveHIGHTail; // since 1.0

    public String directiveBYTE; // since 1.0
    public String directiveBYTESeparator; // since 1.0
    public int directiveBYTENumberOfBytesPerLine; // since 3.0 (word, 0-65535)
    public int directiveBYTENumberOfCharactersPerString; // since 3.0 (word, 0-65535)
    public boolean directiveBYTEOnlyNumbersAllowed; // since 1.0
    public boolean directiveSBYTEAllowed; // since 1.0
    public String directiveSBYTE; // since 1.0
    public boolean directiveWORDAllowed; // since 1.0
    public String directiveWORD; // since 1.0
    public int directiveWORDNumberOfWordsPerLine; // since 3.0 (word, 0-65535)
    public boolean directiveDSAllowed; // since 1.3
    public String directiveDS; // since 1.3

    /* Source Structure */
    public String directiveORG; // since 1.0
    public String directiveEQU; // since 1.0
    public String directiveENDHead; // since 1.0
    public boolean directiveENDNeedsFilename;
    public String directiveENDTail; // since 1.0

    /* Disassembly Listing */
    public Encoding outputEncoding; // since 4.0
    public boolean omitUnreferencedSystemLabels; // since 3.0
    public boolean directiveINCLUDEAllowed; // since 1.0
    public String directiveINCLUDEHead; // since 1.0
    public String directiveINCLUDETail; // since 1.0
    public boolean directiveINCLUDEAllEquatesInOneIncludeFile; // since 3.0
    public boolean directiveINCLUDEAllIncludesInMainFile; // since 1.0
    public int directiveINCLUDEMaximumNumberOfLinesPerFile; // since 1.0 (word, 0-65535)

    public Profile() {
        clear();
    }

    public void clear() {
        // Preset default this with MADS settings.
        commentPrefix = ";";
        hexNotationPrefix = "$";
        useIllegalOpcodes = false;
        useLineNumbers = false;
        alignInstructions = true;
        useHexNotation = true;
        showAInAccumulatorMode = false;
        showBRKAsByte0 = true;
        showLowerCaseInstructions = true;
        showColonAfterLabel = false;
        showZPAbsoluteAsByte = false;
        directiveForceAbsolute = ".w";
        quoteForASCIIStrings = "\'";
        directiveWORDNumberOfWordsPerLine = 1; // Because WORDs often mean labels and labels now have "SnnnLxxxx" format
        directiveBYTENumberOfBytesPerLine = 16;
        directiveBYTENumberOfCharactersPerString = 40;
        showOpcodeAsComment = false;

        // Directive syntax.
        directiveBYTE = ".byte";
        directiveWORD = ".word";
        directiveSBYTE = ".sb";
        directiveORG = "org";
        directiveEQU = "equ";
        directiveENDHead = "";
        directiveENDTail = "";
        directiveLOWHead = "<";
        directiveLOWTail = "";
        directiveHIGHHead = ">";
        directiveHIGHTail = "";
        directiveBYTESeparator = ",";
        directiveBYTEOnlyNumbersAllowed = false;
        directiveWORDAllowed = true;
        directiveSBYTEAllowed = true;
        directiveENDNeedsFilename = false;
        showNonASCIIChararactersAsBytes = true;

        directiveDSAllowed = true;
        directiveDS = ".ds";

        // Disassembly listing.
        outputEncoding = Encoding.ASCII;
        omitUnreferencedSystemLabels = true;

        // Include files.
        directiveINCLUDEAllowed = true;
        directiveINCLUDEHead = "icl '";
        directiveINCLUDETail = "'";
        directiveINCLUDEAllEquatesInOneIncludeFile = true;
        directiveINCLUDEAllIncludesInMainFile = false;
        directiveINCLUDEMaximumNumberOfLinesPerFile = 0;
    }

    /** Direct translation of Profile::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        // Source Layout
        Xml.setBoolAttribute(element, "UseLineNumbers", useLineNumbers);
        Xml.setBoolAttribute(element, "AlignInstructions", alignInstructions);
        Xml.setBoolAttribute(element, "ShowLowerCaseInstructions", showLowerCaseInstructions);

        // Opcodes
        Xml.setBoolAttribute(element, "UseIllegalOpcodes", useIllegalOpcodes);
        Xml.setBoolAttribute(element, "ShowAInAccumulatorMode", showAInAccumulatorMode);
        Xml.setBoolAttribute(element, "ShowColonAfterLabel", showColonAfterLabel);
        Xml.setBoolAttribute(element, "ShowOpcodeAsComment", showOpcodeAsComment);
        Xml.setBoolAttribute(element, "ShowBRKAsByte0", showBRKAsByte0);
        Xml.setBoolAttribute(element, "ShowZPAbsoluteAsByte", showZPAbsoluteAsByte);
        Xml.setStringAttribute(element, "DirectiveForceAbsolute", directiveForceAbsolute);

        // Comments
        Xml.setStringAttribute(element, "CommentPrefix", commentPrefix);

        // Numbers
        Xml.setBoolAttribute(element, "UseHexNotation", useHexNotation);
        Xml.setStringAttribute(element, "HexNotationPrefix", hexNotationPrefix);

        // Strings
        Xml.setBoolAttribute(element, "ShowNonASCIIChararactersAsBytes", showNonASCIIChararactersAsBytes);
        Xml.setStringAttribute(element, "QuoteForASCIIStrings", quoteForASCIIStrings);

        // Directives
        Xml.setStringAttribute(element, "DirectiveLOWHead", directiveLOWHead);
        Xml.setStringAttribute(element, "DirectiveLOWTail", directiveLOWTail);
        Xml.setStringAttribute(element, "DirectiveHIGHHead", directiveHIGHHead);
        Xml.setStringAttribute(element, "DirectiveHIGHTail", directiveHIGHTail);

        Xml.setStringAttribute(element, "DirectiveBYTE", directiveBYTE);
        Xml.setStringAttribute(element, "DirectiveBYTESeparator", directiveBYTESeparator);
        Xml.setWordAttribute(element, "DirectiveBYTENumberOfBytesPerLine", directiveBYTENumberOfBytesPerLine);
        Xml.setWordAttribute(element, "DirectiveBYTENumberOfCharactersPerString", directiveBYTENumberOfCharactersPerString);
        Xml.setBoolAttribute(element, "DirectiveBYTEOnlyNumbersAllowed", directiveBYTEOnlyNumbersAllowed);
        Xml.setBoolAttribute(element, "DirectiveSBYTEAllowed", directiveSBYTEAllowed);
        Xml.setStringAttribute(element, "DirectiveSBYTE", directiveSBYTE);
        Xml.setBoolAttribute(element, "DirectiveWORDAllowed", directiveWORDAllowed);
        Xml.setStringAttribute(element, "DirectiveWORD", directiveWORD);
        Xml.setWordAttribute(element, "DirectiveWORDNumberOfWordsPerLine", directiveWORDNumberOfWordsPerLine);
        Xml.setBoolAttribute(element, "DirectiveDSAllowed", directiveDSAllowed);
        Xml.setStringAttribute(element, "DirectiveDS", directiveDS);

        // Source structure
        Xml.setStringAttribute(element, "DirectiveORG", directiveORG);
        Xml.setStringAttribute(element, "DirectiveEQU", directiveEQU);
        Xml.setStringAttribute(element, "DirectiveENDHead", directiveENDHead);
        Xml.setStringAttribute(element, "DirectiveENDTail", directiveENDTail);
        Xml.setBoolAttribute(element, "DirectiveENDNeedsFilename", directiveENDNeedsFilename);

        // Disassembly listing
        Xml.setStringAttribute(element, "OutputEncoding", Encoding.EncodingFactory.getInfo(outputEncoding).getKey());
        Xml.setBoolAttribute(element, "OmitUnreferencedSystemLabels", omitUnreferencedSystemLabels);

        // Include files
        Xml.setBoolAttribute(element, "DirectiveINCLUDEAllowed", directiveINCLUDEAllowed);
        Xml.setStringAttribute(element, "DirectiveINCLUDEHead", directiveINCLUDEHead);
        Xml.setStringAttribute(element, "DirectiveINCLUDETail", directiveINCLUDETail);
        Xml.setBoolAttribute(element, "DirectiveINCLUDEAllEquatesInOneIncludeFile", directiveINCLUDEAllEquatesInOneIncludeFile);
        Xml.setBoolAttribute(element, "DirectiveINCLUDEAllIncludesInMainFile", directiveINCLUDEAllIncludesInMainFile);
        Xml.setWordAttribute(element, "DirectiveINCLUDEMaximumNumberOfLinesPerFile", directiveINCLUDEMaximumNumberOfLinesPerFile);
    }

    /** Direct translation of Profile::DeserializeFrom. */
    public void deserializeFrom(org.w3c.dom.Element element) {
        clear();

        // Source Layout
        useLineNumbers = Xml.getBoolAttribute(element, "UseLineNumbers", useLineNumbers);
        alignInstructions = Xml.getBoolAttribute(element, "AlignInstructions", alignInstructions);
        showLowerCaseInstructions = Xml.getBoolAttribute(element, "ShowLowerCaseInstructions", showLowerCaseInstructions);

        // Opcodes
        useIllegalOpcodes = Xml.getBoolAttribute(element, "UseIllegalOpcodes", useIllegalOpcodes);
        showAInAccumulatorMode = Xml.getBoolAttribute(element, "ShowAInAccumulatorMode", showAInAccumulatorMode);
        showColonAfterLabel = Xml.getBoolAttribute(element, "ShowColonAfterLabel", showColonAfterLabel);
        showOpcodeAsComment = Xml.getBoolAttribute(element, "ShowOpcodeAsComment", showOpcodeAsComment);
        showBRKAsByte0 = Xml.getBoolAttribute(element, "ShowBRKAsByte0", showBRKAsByte0);
        showZPAbsoluteAsByte = Xml.getBoolAttribute(element, "ShowZPAbsoluteAsByte", showZPAbsoluteAsByte);
        directiveForceAbsolute = Xml.getStringAttribute(element, "DirectiveForceAbsolute", directiveForceAbsolute);

        // Comments
        commentPrefix = Xml.getStringAttribute(element, "CommentPrefix", commentPrefix);

        // Numbers
        useHexNotation = Xml.getBoolAttribute(element, "UseHexNotation", useHexNotation);
        hexNotationPrefix = Xml.getStringAttribute(element, "HexNotationPrefix", hexNotationPrefix);

        // Strings
        showNonASCIIChararactersAsBytes = Xml.getBoolAttribute(element, "ShowNonASCIIChararactersAsBytes", showNonASCIIChararactersAsBytes);
        quoteForASCIIStrings = Xml.getStringAttribute(element, "QuoteForASCIIStrings", quoteForASCIIStrings);

        // Directives
        directiveLOWHead = Xml.getStringAttribute(element, "DirectiveLOWHead", directiveLOWHead);
        directiveLOWTail = Xml.getStringAttribute(element, "DirectiveLOWTail", directiveLOWTail);
        directiveHIGHHead = Xml.getStringAttribute(element, "DirectiveHIGHHead", directiveHIGHHead);
        directiveHIGHTail = Xml.getStringAttribute(element, "DirectiveHIGHTail", directiveHIGHTail);

        directiveBYTE = Xml.getStringAttribute(element, "DirectiveBYTE", directiveBYTE);
        directiveBYTESeparator = Xml.getStringAttribute(element, "DirectiveBYTESeparator", directiveBYTESeparator);
        directiveBYTENumberOfBytesPerLine = Xml.getWordAttribute(element, "DirectiveBYTENumberOfBytesPerLine", directiveBYTENumberOfBytesPerLine);
        directiveBYTENumberOfCharactersPerString = Xml.getWordAttribute(element, "DirectiveBYTENumberOfCharactersPerString", directiveBYTENumberOfCharactersPerString);
        directiveBYTEOnlyNumbersAllowed = Xml.getBoolAttribute(element, "DirectiveBYTEOnlyNumbersAllowed", directiveBYTEOnlyNumbersAllowed);
        directiveSBYTEAllowed = Xml.getBoolAttribute(element, "DirectiveSBYTEAllowed", directiveSBYTEAllowed);
        directiveSBYTE = Xml.getStringAttribute(element, "DirectiveSBYTE", directiveSBYTE);
        directiveWORDAllowed = Xml.getBoolAttribute(element, "DirectiveWORDAllowed", directiveWORDAllowed);
        directiveWORD = Xml.getStringAttribute(element, "DirectiveWORD", directiveWORD);
        directiveWORDNumberOfWordsPerLine = Xml.getWordAttribute(element, "DirectiveWORDNumberOfWordsPerLine", directiveWORDNumberOfWordsPerLine);
        directiveDSAllowed = Xml.getBoolAttribute(element, "DirectiveDSAllowed", directiveDSAllowed);
        directiveDS = Xml.getStringAttribute(element, "DirectiveDS", directiveDS);

        // Source Structure
        directiveORG = Xml.getStringAttribute(element, "DirectiveORG", directiveORG);
        directiveEQU = Xml.getStringAttribute(element, "DirectiveEQU", directiveEQU);
        directiveENDHead = Xml.getStringAttribute(element, "DirectiveENDHead", directiveENDHead);
        directiveENDTail = Xml.getStringAttribute(element, "DirectiveENDTail", directiveENDTail);
        directiveENDNeedsFilename = Xml.getBoolAttribute(element, "DirectiveENDNeedsFilename", directiveENDNeedsFilename);

        // Disassembly listing
        String outputEncodingString = Xml.getStringAttribute(element, "OutputEncoding", "");
        outputEncoding = Encoding.EncodingFactory.getInfo(outputEncodingString).getEncoding();

        // Ignore unsuitable encodings.
        if (outputEncoding != Encoding.ASCII && outputEncoding != Encoding.ATASCII && outputEncoding != Encoding.UTF8) {
            outputEncoding = Encoding.ASCII;
        }
        omitUnreferencedSystemLabels = Xml.getBoolAttribute(element, "OmitUnreferencedSystemLabels", omitUnreferencedSystemLabels);

        // Include files
        directiveINCLUDEAllowed = Xml.getBoolAttribute(element, "DirectiveINCLUDEAllowed", directiveINCLUDEAllowed);
        directiveINCLUDEHead = Xml.getStringAttribute(element, "DirectiveINCLUDEHead", directiveINCLUDEHead);
        directiveINCLUDETail = Xml.getStringAttribute(element, "DirectiveINCLUDETail", directiveINCLUDETail);
        directiveINCLUDEAllEquatesInOneIncludeFile = Xml.getBoolAttribute(element, "DirectiveINCLUDEAllEquatesInOneIncludeFile", directiveINCLUDEAllEquatesInOneIncludeFile);
        directiveINCLUDEAllIncludesInMainFile = Xml.getBoolAttribute(element, "DirectiveINCLUDEAllIncludesInMainFile", directiveINCLUDEAllIncludesInMainFile);
        directiveINCLUDEMaximumNumberOfLinesPerFile = Xml.getWordAttribute(element, "DirectiveINCLUDEMaximumNumberOfLinesPerFile", directiveINCLUDEMaximumNumberOfLinesPerFile);
    }

    /**
     * Convenience wrapper around Xml.load/Xml.save with this project's root element name,
     * "Profile" -- matching the standalone *.prf profile files this project ships (see
     * ProfileDialog's "Load Profile.../Save Profile..." buttons). Not a direct C++ translation:
     * in the original, the equivalent call sites live in the not-yet-ported UI-controller layer
     * (same situation as Workspace.load/save, which this mirrors).
     */
    public void load(String filePath) throws java.io.IOException {
        Xml.load(this, "Profile", filePath);
    }

    public void save(String filePath) throws java.io.IOException {
        Xml.save(this, "Profile", filePath);
    }
}
