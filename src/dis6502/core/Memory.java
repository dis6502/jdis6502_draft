package dis6502.core;

/**
 * Direct Java 6 translation of Memory.h / Memory.cpp.
 *
 * Model of an 8-bit memory with 16 bit address space and little endian byte order.
 *
 * The C++ class defines a family of type aliases (size, offset, byte, word, address,
 * address_offset, dword). Java has no type aliases, so these are expressed as plain
 * int (for unsigned 16/32-bit quantities) or short (for the signed address_offset),
 * with the same constants and helper methods as the original.
 */
public final class Memory {

    private Memory() {
    }

    public static final int MAX_SIZE = 0x10000;
    public static final int MAX_OFFSET = 0xffff;

    /**
     * Direct translation of Memory::to_word(byte low, byte high).
     */
    public static int toWord(int low, int high) {
        int result = high & 0xFF;
        result = (low & 0xFF) | (result << 8);
        return result & 0xFFFF;
    }

    /**
     * Direct translation of Memory::to_address(byte low, byte high).
     */
    public static int toAddress(int low, int high) {
        return toWord(low, high);
    }

    /**
     * Direct translation of Memory::to_low_byte(word word).
     */
    public static int toLowByte(int word) {
        return word & 0xFF;
    }

    /**
     * Direct translation of Memory::to_high_byte(word word).
     */
    public static int toHighByte(int word) {
        return (word >> 8) & 0xFF;
    }

    public static String sizeToHexString(int size) {
        if (size < MAX_SIZE) {
            return DatatypeUtility.wordToHexString(size, false);
        } else if (size == MAX_SIZE) {
            return "10000";
        }
        throw new RuntimeException("Size " + Integer.toHexString(size)
                + " exceeds maximum size of " + Integer.toHexString(MAX_SIZE));
    }

    public static String sizeToString(int size) {
        return String.valueOf(size);
    }

    public static String offsetToHexString(int offset) {
        if (offset <= MAX_OFFSET) {
            return DatatypeUtility.wordToHexString(offset, false);
        }
        throw new RuntimeException("Offset " + Integer.toHexString(offset)
                + " exceeds maximum size of " + Integer.toHexString(MAX_OFFSET));
    }

    public static String byteToHexString(int b) {
        return DatatypeUtility.byteToHexString(b, false);
    }

    public static String addressToHexString(int address) {
        return DatatypeUtility.wordToHexString(address, false);
    }

    /**
     * Direct translation of Memory::address_offset_to_string. addressOffset is a signed
     * 16 bit quantity (address_offset in the C++ source).
     */
    public static String addressOffsetToString(int addressOffset) {
        if (addressOffset == 0) {
            return "";
        }
        if (addressOffset > 0) {
            return "+" + addressOffset;
        }
        // "-" is already part of Java's Integer.toString() for negative numbers.
        return String.valueOf(addressOffset);
    }
}
