package dis6502.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small one-line-at-a-time 6502/65C02 assembler, used by the hex dump pane's "Assemble at
 * selection..." command (see Dis6502Gui.openAssembleDialog). This is NOT a translation of any
 * C++ class -- jDIS6502 has only ever been a disassembler, and the C++ sources contain no
 * assembler to port. It is a genuine, from-scratch implementation, built on top of the
 * InstructionSet opcode table (Instruction/InstructionSetMOS6502/InstructionSetMOS65C02) that
 * *was* ported, via a reverse lookup from (mnemonic, addressing mode) back to an opcode byte.
 *
 * Supported syntax per line (case-insensitive mnemonic, "; comment" and blank lines allowed):
 *   CLC                 Implied
 *   ASL  / ASL A        Accumulator
 *   LDA #$xx            Immediate
 *   LDA $xx             ZeroPage (auto-promoted to Absolute if no zero-page form exists)
 *   LDA $xx,X           ZeroPageX (auto-promoted to AbsoluteX if no zero-page form exists)
 *   LDX $xx,Y           ZeroPageY (auto-promoted to AbsoluteY if no zero-page form exists)
 *   LDA $xxxx           Absolute
 *   LDA $xxxx,X         AbsoluteX
 *   LDA $xxxx,Y         AbsoluteY
 *   JMP ($xxxx)         Indirect
 *   LDA ($xx,X)         IndexedIndirect
 *   LDA ($xx),Y         IndirectIndexed
 *   BEQ $xxxx           Relative (an absolute target address; the branch offset is computed
 *                       from it and the instruction's own address, exactly as a real assembler
 *                       would, and reported as an error if out of the -128..127 range)
 *
 * Values are hex only (a leading '$', matching this whole tool's own display convention); a
 * bare token without '$' is rejected rather than silently guessed at as decimal.
 */
public final class Assembler {

    private Assembler() {
    }

    public static final class AssemblyException extends Exception {
        public AssemblyException(String message) {
            super(message);
        }
    }

    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^([A-Za-z]{3})(?:\\s+(.*))?$");

    /**
     * Assembles zero or more lines of text (one instruction per line, blank lines and
     * "; comment" trailers ignored) starting at startAddress, returning the assembled bytes.
     * Each line's own address (needed for Relative/branch encoding) is startAddress plus the
     * length of every previously-assembled instruction on prior lines.
     */
    public static byte[] assembleLines(InstructionSet instructionSet, int startAddress, String text)
            throws AssemblyException {
        List<Byte> output = new ArrayList<Byte>();
        int address = startAddress & 0xFFFF;

        String[] lines = text.split("\\r?\\n");
        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String rawLine = lines[lineNumber];
            String line = stripComment(rawLine).trim();
            if (line.length() == 0) {
                continue;
            }

            byte[] encoded;
            try {
                encoded = assembleLine(instructionSet, address, line);
            } catch (AssemblyException e) {
                throw new AssemblyException("Line " + (lineNumber + 1) + " (\"" + rawLine.trim() + "\"): " + e.getMessage());
            }

            for (int i = 0; i < encoded.length; i++) {
                output.add(Byte.valueOf(encoded[i]));
            }
            address = (address + encoded.length) & 0xFFFF;
        }

        byte[] result = new byte[output.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = output.get(i).byteValue();
        }
        return result;
    }

    /** Assembles a single instruction line (mnemonic + optional operand) at the given address. */
    public static byte[] assembleLine(InstructionSet instructionSet, int address, String line) throws AssemblyException {
        Matcher matcher = LINE_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            throw new AssemblyException("Expected a 3-letter mnemonic, optionally followed by an operand");
        }
        String mnemonic = matcher.group(1);
        String operand = matcher.group(2) == null ? "" : matcher.group(2).trim();

        ParsedOperand parsed = parseOperand(operand);

        // Relative (branch) instructions: the operand is the absolute target address; resolve
        // the actual branch displacement once we know the instruction's own length (always 2).
        if (parsed.mode == OperandMode.Absolute && isBranchMnemonic(mnemonic)) {
            int opcode = findOpcode(instructionSet, mnemonic, OperandMode.Relative);
            if (opcode < 0) {
                throw new AssemblyException("Unknown mnemonic/addressing mode combination \"" + mnemonic + "\"");
            }
            int nextAddress = (address + 2) & 0xFFFF;
            int displacement = parsed.value - nextAddress;
            if (displacement < -128 || displacement > 127) {
                throw new AssemblyException("Branch target is out of range (-128..127 bytes from the next instruction)");
            }
            return new byte[] { (byte) opcode, (byte) (displacement & 0xFF) };
        }

        int opcode = findOpcode(instructionSet, mnemonic, parsed.mode);
        OperandMode effectiveMode = parsed.mode;

        // Auto-promote zero-page addressing to absolute if this mnemonic has no zero-page form
        // (e.g. many illegal opcodes, or simply an instruction that never had one) -- same
        // fallback a typical assembler applies rather than forcing the user to know the table.
        if (opcode < 0) {
            OperandMode promoted = promoteZeroPage(parsed.mode);
            if (promoted != null) {
                opcode = findOpcode(instructionSet, mnemonic, promoted);
                if (opcode >= 0) {
                    effectiveMode = promoted;
                }
            }
        }

        if (opcode < 0) {
            throw new AssemblyException("Unknown mnemonic/addressing mode combination \"" + mnemonic
                    + (operand.length() > 0 ? " " + operand : "") + "\"");
        }

        return encode(opcode, effectiveMode, parsed.value);
    }

    private static boolean isBranchMnemonic(String mnemonic) {
        String m = mnemonic.toUpperCase();
        return m.equals("BPL") || m.equals("BMI") || m.equals("BVC") || m.equals("BVS")
                || m.equals("BCC") || m.equals("BCS") || m.equals("BNE") || m.equals("BEQ")
                || m.equals("BRA"); // 65C02 unconditional branch
    }

    private static OperandMode promoteZeroPage(OperandMode mode) {
        if (mode == OperandMode.ZeroPage) {
            return OperandMode.Absolute;
        }
        if (mode == OperandMode.ZeroPageX) {
            return OperandMode.AbsoluteX;
        }
        if (mode == OperandMode.ZeroPageY) {
            return OperandMode.AbsoluteY;
        }
        return null;
    }

    private static int findOpcode(InstructionSet instructionSet, String mnemonic, OperandMode mode) {
        Instruction[] instructions = instructionSet.getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            Instruction instruction = instructions[i];
            if (instruction != null && instruction.getName().equalsIgnoreCase(mnemonic) && instruction.getOperandMode() == mode) {
                return instruction.getOpcode();
            }
        }
        return -1;
    }

    private static byte[] encode(int opcode, OperandMode mode, int value) {
        switch (mode) {
            case Implied:
            case Accumulator:
                return new byte[] { (byte) opcode };

            case Immediate:
            case ZeroPage:
            case ZeroPageX:
            case ZeroPageY:
            case IndexedIndirect:
            case IndirectIndexed:
            case ZeroPageIndirect:
                return new byte[] { (byte) opcode, (byte) (value & 0xFF) };

            default: // Absolute, AbsoluteX, AbsoluteY, Indirect
                return new byte[] { (byte) opcode, (byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF) };
        }
    }

    private static String stripComment(String line) {
        int semi = line.indexOf(';');
        return semi < 0 ? line : line.substring(0, semi);
    }

    private static final class ParsedOperand {
        final OperandMode mode;
        final int value;

        ParsedOperand(OperandMode mode, int value) {
            this.mode = mode;
            this.value = value;
        }
    }

    private static final Pattern IMMEDIATE = Pattern.compile("^#\\$([0-9A-Fa-f]{1,2})$");
    private static final Pattern INDEXED_INDIRECT = Pattern.compile("^\\(\\$([0-9A-Fa-f]{1,2}),X\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDIRECT_INDEXED = Pattern.compile("^\\(\\$([0-9A-Fa-f]{1,2})\\),Y$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZP_INDIRECT = Pattern.compile("^\\(\\$([0-9A-Fa-f]{1,2})\\)$");
    private static final Pattern INDIRECT = Pattern.compile("^\\(\\$([0-9A-Fa-f]{3,4})\\)$");
    private static final Pattern ZERO_PAGE_X = Pattern.compile("^\\$([0-9A-Fa-f]{1,2}),X$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZERO_PAGE_Y = Pattern.compile("^\\$([0-9A-Fa-f]{1,2}),Y$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABSOLUTE_X = Pattern.compile("^\\$([0-9A-Fa-f]{3,4}),X$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABSOLUTE_Y = Pattern.compile("^\\$([0-9A-Fa-f]{3,4}),Y$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZERO_PAGE = Pattern.compile("^\\$([0-9A-Fa-f]{1,2})$");
    private static final Pattern ABSOLUTE = Pattern.compile("^\\$([0-9A-Fa-f]{3,4})$");

    private static ParsedOperand parseOperand(String operand) throws AssemblyException {
        if (operand.length() == 0) {
            return new ParsedOperand(OperandMode.Implied, 0);
        }
        if (operand.equalsIgnoreCase("A")) {
            return new ParsedOperand(OperandMode.Accumulator, 0);
        }

        Matcher m;
        if ((m = IMMEDIATE.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.Immediate, hex(m.group(1)));
        }
        if ((m = INDEXED_INDIRECT.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.IndexedIndirect, hex(m.group(1)));
        }
        if ((m = INDIRECT_INDEXED.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.IndirectIndexed, hex(m.group(1)));
        }
        if ((m = INDIRECT.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.Indirect, hex(m.group(1)));
        }
        if ((m = ZP_INDIRECT.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.ZeroPageIndirect, hex(m.group(1)));
        }
        if ((m = ZERO_PAGE_X.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.ZeroPageX, hex(m.group(1)));
        }
        if ((m = ZERO_PAGE_Y.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.ZeroPageY, hex(m.group(1)));
        }
        if ((m = ABSOLUTE_X.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.AbsoluteX, hex(m.group(1)));
        }
        if ((m = ABSOLUTE_Y.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.AbsoluteY, hex(m.group(1)));
        }
        if ((m = ZERO_PAGE.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.ZeroPage, hex(m.group(1)));
        }
        if ((m = ABSOLUTE.matcher(operand)).matches()) {
            return new ParsedOperand(OperandMode.Absolute, hex(m.group(1)));
        }

        throw new AssemblyException("Could not parse operand \"" + operand + "\" (expected e.g. #$xx, $xx, $xxxx, $xx,X, $xxxx,Y, ($xx,X), ($xx),Y or ($xxxx))");
    }

    private static int hex(String digits) throws AssemblyException {
        try {
            return Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            throw new AssemblyException("Invalid hex value \"" + digits + "\"");
        }
    }
}
