package dis6502.core;

/**
 * Direct Java 6 translation of DisassemblyLineWriter.h / DisassemblyLineWriter.cpp: a small
 * fluent line-text builder used while generating one line of the disassembly listing.
 *
 * The C++ source writes into a fixed-size wchar_t[] (with a "lineBufferSize" constructor
 * parameter and a bounds check on every Char() call, throwing if the line would overflow it).
 * That size limit is a C-buffer safety net, not meaningful behavior; this translation uses a
 * plain StringBuilder that grows as needed and keeps only the *content* validation (Char()
 * still rejects characters outside the printable ASCII range 32-126, same as the original).
 *
 * Method names that would collide with Java keywords are renamed: Char -> charValue,
 * Byte -> byteValue.
 */
public final class DisassemblyLineWriter {

    private final StringBuilder buffer = new StringBuilder();

    private Workspace workspace;
    private Profile profile;
    private String directiveForceAbsolute = "";

    public DisassemblyLineWriter() {
    }

    /** Initialize the state before a disassembly run. */
    public void init(Workspace workspace) {
        this.workspace = workspace;
        this.profile = workspace.getConstProfile();

        // Cache formatter profile infos.
        directiveForceAbsolute = profile.directiveForceAbsolute;
        if (profile.showLowerCaseInstructions) {
            directiveForceAbsolute = Strings.toLower(directiveForceAbsolute);
        }
    }

    public String getLineBuffer() {
        return buffer.toString();
    }

    public DisassemblyLineWriter clear() {
        buffer.setLength(0);
        return this;
    }

    public DisassemblyLineWriter charValue(char value) {
        if (value < 32 || value > 127) {
            throw new RuntimeException("Invalid character");
        }
        buffer.append(value);
        return this;
    }

    public DisassemblyLineWriter cstring(String value) {
        for (int i = 0; i < value.length(); i++) {
            charValue(value.charAt(i));
        }
        return this;
    }

    public DisassemblyLineWriter string(String value) {
        return cstring(value);
    }

    public DisassemblyLineWriter comment() {
        return comment("");
    }

    public DisassemblyLineWriter comment(String comment) {
        string(profile.commentPrefix);
        if (comment.length() != 0) {
            space();
            string(comment);
        }
        return this;
    }

    public DisassemblyLineWriter space() {
        return charValue(' ');
    }

    public DisassemblyLineWriter spaceUntil34() {
        while (buffer.length() < 34) {
            space();
        }
        return this;
    }

    public DisassemblyLineWriter decimal(long value) {
        return cstring(String.valueOf(value));
    }

    public DisassemblyLineWriter number(long value) {
        if (profile.useHexNotation) {
            return cstring(profile.hexNotationPrefix + String.format("%04X", value));
        } else {
            return decimal(value);
        }
    }

    public DisassemblyLineWriter byteNumber(int value) {
        if (profile.useHexNotation) {
            return cstring(profile.hexNotationPrefix + String.format("%02X", value & 0xFF));
        } else {
            return decimal(value & 0xFF);
        }
    }

    public DisassemblyLineWriter byteValue(int value) {
        if (profile.useHexNotation) {
            return cstring(profile.hexNotationPrefix + String.format("%02X", value & 0xFF));
        } else {
            return decimal(value & 0xFF);
        }
    }

    public DisassemblyLineWriter address(int address) {
        return number(address & 0xFFFF);
    }

    public DisassemblyLineWriter label(String label) {
        return cstring(label);
    }

    public DisassemblyLineWriter labelOrZeroPageAddress(SegmentList segmentList, int segmentIndex, int wPC, int wAddr, MemoryType type, int opcode) {
        if (wAddr >= 0x0100) {
            throw new RuntimeException("Address is not on zero page");
        }
        String label = segmentList.getLabelAtAddress(segmentIndex, wPC, wAddr, type, opcode);
        if (label.length() != 0) {
            return label(label);
        }
        return byteValue(wAddr & 0xFF);
    }

    public DisassemblyLineWriter labelOrAddress(SegmentList segmentList, int segmentIndex, int wPC, int wAddr, MemoryType type, int opcode) {
        String label = segmentList.getLabelAtAddress(segmentIndex, wPC, wAddr, type, opcode);
        if (label.length() != 0) {
            return label(label);
        }
        return address(wAddr);
    }

    public DisassemblyLineWriter alignInstructions() {
        if (profile.alignInstructions) {
            int labelLength = buffer.length();
            int maxLength = Math.max(labelLength + 1, 12);
            for (int len = labelLength; len < maxLength; len++) {
                space();
            }
        }
        return this;
    }

    public DisassemblyLineWriter instruction(Segment segment, int opcode) {
        return instruction(segment, opcode, 0xFFFF);
    }

    public DisassemblyLineWriter instruction(Segment segment, int opcode, int wAddr) {
        InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
        boolean isDualMode = (wAddr < 0x100) && instructionSet.isDualAddressingMode(opcode)
                && (!profile.showZPAbsoluteAsByte) && (directiveForceAbsolute.length() != 0);
        String opcodeName = instructionSet.getInstruction(opcode).getName();

        if (profile.showLowerCaseInstructions) {
            for (int i = 0; i < opcodeName.length(); i++) {
                charValue(Character.toLowerCase(opcodeName.charAt(i)));
            }
        } else {
            string(opcodeName);
        }

        if (isDualMode) {
            string(directiveForceAbsolute);
        }
        return this;
    }
}
