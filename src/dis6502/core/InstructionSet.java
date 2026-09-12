package dis6502.core;

/**
 * Direct Java 6 translation of the InstructionSet class (InstructionSet.h / InstructionSet.cpp).
 *
 * The original C++ class is an "abstract" base (never instantiated directly, only through
 * InstructionSetMOS6502 / InstructionSetMOS65C02), is non-copyable and non-movable. In Java
 * this is expressed as an abstract class with package-visible mutation of the instruction
 * table restricted to the constructor.
 */
public abstract class InstructionSet {

    private final String name;
    private final Instruction[] instructionArray; // always length 256

    /**
     * Direct translation of InstructionSet::InstructionSet(wstring_view name, std::array<Instruction, 256>).
     * Subclasses build the 256-entry table and pass it in.
     */
    protected InstructionSet(String name, Instruction[] instructionArray) {
        if (instructionArray == null || instructionArray.length != 256) {
            throw new IllegalArgumentException("instructionArray must contain exactly 256 entries");
        }
        this.name = name;
        this.instructionArray = instructionArray;
    }

    public Instruction[] getInstructions() {
        return instructionArray;
    }

    public Instruction getInstruction(int opcode) {
        return instructionArray[opcode & 0xFF];
    }

    public String getName() {
        return name;
    }

    /**
     * Direct translation of InstructionSet::IsDualAddressingMode(byte opcode) const.
     * True for opcodes whose absolute/absolute-indexed forms both exist and share meaning,
     * used by the disassembler when deciding how to render dual-form instructions.
     */
    public boolean isDualAddressingMode(int opcode) {
        int op = opcode & 0xFF;
        return op == 0xAD || op == 0xBD // LDA
                || op == 0xAE || op == 0xBE // LDX
                || op == 0xAC || op == 0xBC // LDY
                || op == 0x8D || op == 0x9D // STA
                || op == 0x8E                // STX
                || op == 0x8C                // SYY
                || op == 0x2C                // BIT
                || op == 0x6D || op == 0x7D // ADC
                || op == 0xED || op == 0xFD // SBC
                || op == 0x0E || op == 0x1E // ASL
                || op == 0x4E || op == 0x5E // LSR
                || op == 0x2E || op == 0x3E // ROL
                || op == 0x6E || op == 0x7E // ROR
                || op == 0xCE || op == 0xDE // DEC
                || op == 0xEE || op == 0xFE // INC
                || op == 0x0D || op == 0x1D // ORA
                || op == 0x2D || op == 0x3D // AND
                || op == 0x4D || op == 0x5D // EOR
                ;
    }
}
