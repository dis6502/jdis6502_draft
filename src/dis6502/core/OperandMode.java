package dis6502.core;

/**
 * Direct Java 6 translation of the OperandMode enum class in InstructionSet.h.
 */
public enum OperandMode {
    Immediate,
    Absolute,
    ZeroPage,
    Accumulator,
    Implied,
    IndexedIndirect,
    IndirectIndexed,
    ZeroPageX,
    ZeroPageY,
    AbsoluteX,
    AbsoluteY,
    Relative,
    Indirect,
    ZeroPageIndirect,
    ZeroPageRelative,
    IndexedIndirectAbsolute,
    ReservedNop1Byte,
    ReservedNop2Byte,
    ReservedNop3Byte
}
