package dis6502.core;

/**
 * Direct Java 6 translation of DisassemblyOpcodeBuffer.h / DisassemblyOpcodeBuffer.cpp.
 * Saves the last (up to) 3 opcode bytes read, so they can be shown as a comment.
 */
public final class DisassemblyOpcodeBuffer {

    private int opcode0;
    private int opcode1;
    private int opcode2;

    public void clear() {
        opcode0 = opcode1 = opcode2 = 0;
    }

    /** Save the last byte as opcode in a circular 3-byte buffer (to display opcodes as comment). */
    public void saveLastOpcode(int value) {
        opcode0 = opcode1;
        opcode1 = opcode2;
        opcode2 = value & 0xFF;
    }

    public void write(DisassemblyLineWriter lineWriter, int opcodeSize) {
        switch (opcodeSize) {
            case 1:
                lineWriter.byteValue(opcode2);
                break;
            case 2:
                lineWriter.byteValue(opcode1);
                lineWriter.space();
                lineWriter.byteValue(opcode2);
                break;
            case 3:
                lineWriter.byteValue(opcode0);
                lineWriter.space();
                lineWriter.byteValue(opcode1);
                lineWriter.space();
                lineWriter.byteValue(opcode2);
                break;
            default:
                throw new RuntimeException("Invalid opcode size");
        }
    }
}
