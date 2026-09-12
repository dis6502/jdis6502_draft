package dis6502.core;

/**
 * Direct Java 6 translation of FileHeader.h / FileHeader.cpp.
 *
 * File and file segment headers.
 * See http://sdx.atari8.info/sdx_files/4.49/SDX449_Programming_Guide_EN.pdf for the details
 * of the SDX file formats.
 *
 * The C++ source is "enum class FileHeader : unsigned short", i.e. backed by a 16 bit value;
 * that backing value is preserved here via getValue()/fromValue() since it is read/written
 * as a raw word during (de)serialization.
 */
public enum FileHeader {
    RAW(0x0000),
    ATARI_BINARY(0xFFFF),
    SDX_FIXED_BLK(0xFFFA),
    SDX_SYM_REQUIRED(0xFFFB),
    SDX_SYM_DEFINED(0xFFFC),
    SDX_FIX_UP_BLK(0xFFFD),
    SDX_RELOC_BLK(0xFFFE),
    ORIC_BINARY(0x1616);

    private final int value;

    private FileHeader(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /** Direct translation of the free function "wstring to_wstring(FileHeader fileHeader)". */
    public String toWString() {
        return DatatypeUtility.wordToHexString(value, true);
    }

    public static FileHeader fromValue(int value) {
        FileHeader[] all = values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].value == (value & 0xFFFF)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("Unknown FileHeader value: " + Integer.toHexString(value));
    }
}
