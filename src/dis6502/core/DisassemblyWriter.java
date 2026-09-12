package dis6502.core;

/**
 * Direct Java 6 translation of DisassemblyWriter.h / DisassemblyWriter.cpp: accumulates runs
 * of consecutive .BYTE/.SBYTE/.WORD/.DS directives (and their string-literal packing) while
 * Pass4 walks the code, flushing them into a single disassembly line whenever the run ends.
 *
 * This class reaches directly into a few of Disassembly's fields/methods (absoluteAddress,
 * markSize, addLine/addLineWriter), mirroring the C++ source's "friend class DisassemblyWriter"
 * relationship in Disassembly.h -- approximated here via package-private access, since both
 * classes live in the same package.
 */
public final class DisassemblyWriter {

    private final Disassembly disassembly;
    private final Profile profile;
    private final int returnCharacter;
    private final char quoteCharacter;

    private int disNbBytes = 0;
    private MemoryType memoryType = MemoryType.UNKNOWN;
    private final StringBuilder bytes = new StringBuilder();

    public DisassemblyWriter(Disassembly disassembly, Workspace workspace) {
        this.disassembly = disassembly;
        this.returnCharacter = workspace.getComputerSystem().getReturnCharacter();
        this.profile = workspace.getConstProfile();
        this.quoteCharacter = profile.quoteForASCIIStrings.charAt(0);
    }

    /**
     * Returns true if a character can be inserted in a string.
     * Returns false if the character should appear as a hex byte instead.
     */
    public boolean isByteAllowedInString(int value) {
        if ((value == 0) || (value == returnCharacter) || (value == quoteCharacter)) {
            return false;
        }

        // TODO (same as the C++ source): this actually depends on the character set of the computer system.
        if (profile.showNonASCIIChararactersAsBytes && ((value < 0x20) || (value >= 0x7D) || (value == 0x60) || (value == 0x7B))) {
            return false;
        }

        return true;
    }

    public void flushAndAddLine(DisassemblyLineWriter lineWriter, int wAddr) {
        flushBytes();
        disassembly.absoluteAddress = wAddr;
        disassembly.addLineWriter();
    }

    public void flushAndAddLineWithComment(DisassemblyLineWriter lineWriter, DisassemblyOpcodeBuffer opcodeBuffer, int opcodeSize, int wAddr) {
        if (profile.showOpcodeAsComment) {
            lineWriter.spaceUntil34();
            lineWriter.string(profile.commentPrefix);
            lineWriter.space();
            opcodeBuffer.write(lineWriter, opcodeSize);
        }
        flushAndAddLine(lineWriter, wAddr);
    }

    /** Flush all collected .DS/.BYTE/.SBYTE/.WORD directives in the disassembly listing. */
    public void flushBytes() {
        if (disNbBytes > 0) {
            int saveSize = disassembly.markSize;

            // MemoryType is a value class, not an enum (see MemoryType.java), so this is an
            // if/else chain rather than a switch.
            if (memoryType == MemoryType.STRING || memoryType == MemoryType.SBYTE) {
                bytes.append(profile.quoteForASCIIStrings);
            } else if (memoryType == MemoryType.STORE) {
                bytes.append(disNbBytes);
            }

            disassembly.markSize = disNbBytes;
            disassembly.addLine(bytes.toString());
            bytes.setLength(0);

            if (saveSize >= disNbBytes) {
                disassembly.markSize = saveSize - disNbBytes;
            } else {
                disassembly.markSize = 0;
            }

            disNbBytes = 0;
            memoryType = MemoryType.UNKNOWN;
        }
    }

    /** Skip another byte with a .DS directive. */
    public void dumpStore() {
        if (memoryType != MemoryType.STORE) {
            flushBytes();
        }

        memoryType = MemoryType.STORE;

        if (disNbBytes == 0) {
            bytes.setLength(0);
            bytes.append(getLineBuffer()).append(profile.directiveDS).append(' ');
        }

        disNbBytes++;
    }

    /** Save a byte in a temporary buffer, written as a .BYTE directive in hexadecimal format. */
    public void dumpByte(int value) {
        int b = value & 0xFF;

        if (disNbBytes >= profile.directiveBYTENumberOfBytesPerLine) {
            flushBytes();
        } else if (memoryType != MemoryType.BYTE) {
            flushBytes();
        }

        memoryType = MemoryType.BYTE;

        if (profile.useHexNotation) {
            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveBYTE).append(' ')
                        .append(profile.hexNotationPrefix).append(String.format("%02X", b));
            } else {
                bytes.append(profile.directiveBYTESeparator).append(profile.hexNotationPrefix).append(String.format("%02X", b));
            }
        } else {
            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveBYTE).append(' ').append(b);
            } else {
                bytes.append(profile.directiveBYTESeparator).append(b);
            }
        }

        disNbBytes++;
    }

    /** Save a word in a temporary buffer, written as a .WORD directive in hexadecimal format. */
    public void dumpWord(int value) {
        int w = value & 0xFFFF;

        if (disNbBytes >= (profile.directiveWORDNumberOfWordsPerLine * 2)) {
            flushBytes();
        } else if (memoryType != MemoryType.WORD) {
            flushBytes();
        }

        memoryType = MemoryType.WORD;

        if (profile.useHexNotation) {
            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveWORD).append(' ')
                        .append(profile.hexNotationPrefix).append(String.format("%04X", w));
            } else {
                bytes.append(profile.directiveBYTESeparator).append(profile.hexNotationPrefix).append(String.format("%04X", w));
            }
        } else {
            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveWORD).append(' ').append(w);
            } else {
                bytes.append(profile.directiveBYTESeparator).append(w);
            }
        }

        disNbBytes += 2;
    }

    /** Save a character in a temporary buffer, written as a .BYTE directive in string format containing ASCII code. */
    public void dumpString(int value) {
        int c = value & 0xFF;

        if (!isByteAllowedInString(c)) {
            dumpByte(c);
        } else {
            if (disNbBytes >= profile.directiveBYTENumberOfCharactersPerString) {
                flushBytes();
            } else if (memoryType != MemoryType.STRING) {
                flushBytes();
            }

            memoryType = MemoryType.STRING;

            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveBYTE).append(' ')
                        .append(profile.quoteForASCIIStrings).append((char) c);
            } else {
                bytes.append((char) c);
            }

            disNbBytes++;
        }
    }

    /** Save a character in a temporary buffer, written as a .SBYTE directive in string format containing internal code. */
    public void dumpSByte(int value) {
        int b = value & 0xFF;
        int internal = b;

        if (internal < 64) {
            internal += 32;
        } else if (internal < 96) {
            internal -= 64;
        } else if ((internal >= 128) && (internal < 128 + 64)) {
            internal += 32;
        } else if ((internal >= 128 + 64) && (internal < 128 + 96)) {
            internal -= 64;
        }

        if ((internal == returnCharacter) || (internal == 0x22) || (internal == 0)) {
            dumpByte(b);
        } else {
            if (disNbBytes >= profile.directiveBYTENumberOfCharactersPerString) {
                flushBytes();
            } else if (memoryType != MemoryType.SBYTE) {
                flushBytes();
            }

            memoryType = MemoryType.SBYTE;

            if (disNbBytes == 0) {
                bytes.setLength(0);
                bytes.append(getLineBuffer()).append(profile.directiveSBYTE).append(' ')
                        .append(profile.quoteForASCIIStrings).append((char) internal);
            } else {
                bytes.append((char) internal);
            }

            disNbBytes++;
        }
    }

    /** Save a label in a temporary buffer, written as a .WORD directive. */
    public void dumpLabel(String label) {
        if (disNbBytes >= (profile.directiveWORDNumberOfWordsPerLine * 2)) {
            flushBytes();
        } else if (memoryType != MemoryType.LABEL) {
            flushBytes();
        }

        memoryType = MemoryType.LABEL;

        if (disNbBytes == 0) {
            bytes.setLength(0);
            bytes.append(getLineBuffer()).append(profile.directiveWORD).append(' ').append(label);
        } else {
            bytes.append(profile.directiveBYTESeparator).append(label);
        }

        disNbBytes += 2;
    }

    private String getLineBuffer() {
        return disassembly.lineWriter.getLineBuffer();
    }
}
