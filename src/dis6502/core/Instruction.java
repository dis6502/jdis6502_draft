package dis6502.core;

/**
 * Direct Java 6 translation of the Instruction class (InstructionSet.h / InstructionSet.cpp).
 * Represents a single 6502/65C02 opcode entry: its mnemonic, addressing mode, and whether
 * it is a documented ("legal") or undocumented ("illegal") instruction.
 *
 * Note: byte/word values (opcode, length) are represented as plain Java int here, following
 * common Java practice, since Java's byte/short types are signed and the C++ types are not.
 * Callers should mask with 0xFF / 0xFFFF where appropriate.
 */
public final class Instruction {

    private final int opcode;       // byte opcode in the C++ source (0-255)
    private final String name;
    private final boolean illegal;
    private final LabelAccess labelAccess;
    private final OperandMode mode;

    public Instruction(int opcode, String name, boolean illegal, LabelAccess labelAccess, OperandMode mode) {
        this.opcode = opcode & 0xFF;
        this.name = name;
        this.illegal = illegal;
        this.labelAccess = labelAccess;
        this.mode = mode;
    }

    public int getOpcode() {
        return opcode;
    }

    public String getName() {
        return name;
    }

    /**
     * Find the length of an opcode, in bytes (1, 2 or 3).
     * Direct translation of Instruction::GetLength().
     */
    public int getLength() {
        switch (mode) {
            case Immediate:
            case ZeroPage:
            case IndexedIndirect:
            case IndirectIndexed:
            case ZeroPageX:
            case ZeroPageY:
            case Relative:
                return 2;

            case Absolute:
            case AbsoluteX:
            case AbsoluteY:
            case Indirect:
                return 3;

            default:
                return 1;
        }
    }

    public LabelAccess getLabelAccess() {
        return labelAccess;
    }

    public OperandMode getOperandMode() {
        return mode;
    }

    public boolean isImmediateMode() {
        return mode == OperandMode.Immediate;
    }

    public boolean isUnsupportedInstruction() {
        return illegal;
    }
}
