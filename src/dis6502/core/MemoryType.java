package dis6502.core;

/**
 * Direct Java 6 translation of MemoryType.h (current/v3.0 version, not the legacy version_22
 * namespace variant).
 *
 * Type of bytes found in the type buffer that qualify the memory-inspector buffer.
 * MemoryType.LOBYTE and MemoryType.HIBYTE are followed by a byte that represents the
 * missing part of the address. For example, if we have $A9 $04 in the source (and it
 * should be disassembled as LDA # &gt;L0480), we will have MemoryType.HIBYTE followed by
 * $80 in the type buffer to say that we have a HIBYTE ($04) in the memory-inspector
 * buffer and the LOBYTE is $80.
 *
 * Implementation note: this was originally translated as a plain Java enum (matching the C++
 * "enum class MemoryType : byte"), but the LOBYTE/HIBYTE scheme described above means the type
 * buffer can legitimately hold *arbitrary* raw byte values (0-255) at the position right after
 * a LOBYTE/HIBYTE tag -- Disassembly.java reads that position back as a MemoryType and then
 * reinterprets it as a raw address byte, exactly as the C++ source's unchecked
 * static_cast&lt;MemoryType&gt;(byte) does. A closed Java enum can't represent that (it would
 * have to throw for anything outside the 13 named constants), so MemoryType is instead a small
 * value class: the 13 named values below are canonical singletons (so existing `==`
 * comparisons against e.g. MemoryType.CODE keep working exactly as with an enum), while
 * fromByte() tolerantly wraps any other raw byte in a lightweight unnamed instance instead of
 * throwing.
 */
public final class MemoryType {

    private final int value;
    private final String name;

    private MemoryType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static final MemoryType UNKNOWN = new MemoryType(0, "UNKNOWN");
    public static final MemoryType LOBYTE = new MemoryType(1, "LOBYTE");
    public static final MemoryType HIBYTE = new MemoryType(2, "HIBYTE");
    public static final MemoryType BYTE = new MemoryType(3, "BYTE");
    public static final MemoryType WORD = new MemoryType(4, "WORD");
    public static final MemoryType LABEL = new MemoryType(5, "LABEL");
    public static final MemoryType STRING = new MemoryType(6, "STRING");
    public static final MemoryType SBYTE = new MemoryType(7, "SBYTE");
    public static final MemoryType DLIST = new MemoryType(8, "DLIST");
    public static final MemoryType STORE = new MemoryType(9, "STORE");
    public static final MemoryType CODE = new MemoryType(10, "CODE");
    public static final MemoryType SYMBOL = new MemoryType(11, "SYMBOL"); // New in version 3.0
    public static final MemoryType FIXUP = new MemoryType(12, "FIXUP");  // New in version 3.0
    // LOTABLE,  // New in version 3.0, not implemented yet
    // HITABLE,  // New in version 3.0, not implemented yet

    /** Corresponds to MEMORY_TYPE_ENUM_ITEM_COUNT in MemoryType.h */
    public static final int MEMORY_TYPE_ENUM_ITEM_COUNT = 13;

    private static final MemoryType[] NAMED = {
            UNKNOWN, LOBYTE, HIBYTE, BYTE, WORD, LABEL, STRING, SBYTE, DLIST, STORE, CODE, SYMBOL, FIXUP
    };

    /**
     * Java 6 translation of the byte -&gt; MemoryType cast used throughout the C++ code.
     * Values 0-12 return the canonical named singleton (so `==` comparisons work as expected);
     * any other value (0-255) returns an unnamed instance wrapping that raw byte, matching the
     * C++ source's unchecked cast rather than throwing -- see the class Javadoc.
     */
    public static MemoryType fromByte(int value) {
        int v = value & 0xFF;
        if (v < NAMED.length) {
            return NAMED[v];
        }
        return new MemoryType(v, "RAW(" + v + ")");
    }

    /** Java 6 translation of static_cast<byte>(memoryType). */
    public int toByte() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MemoryType)) {
            return false;
        }
        return value == ((MemoryType) other).value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
