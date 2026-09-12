package dis6502.core;

/**
 * Direct Java 6 translation of DatatypeUtility.h / DatatypeUtility.cpp.
 *
 * Values that were "byte" (0-255) or "word" (0-65535) in the C++ source are represented
 * as plain int here, masked as needed, since Java's byte/short are signed.
 */
public final class DatatypeUtility {

    private static final String HEX_CHARACTERS = "0123456789ABCDEF";

    private DatatypeUtility() {
    }

    public static String byteToHexString(int value, boolean withPrefix) {
        byte[] single = new byte[] { (byte) (value & 0xFF) };
        return byteArrayToHexString(single, withPrefix);
    }

    public static String wordToHexString(int value, boolean withPrefix) {
        // Assume little endian... the original writes the bytes big-endian into the buffer
        // (high byte first) because ByteArrayToHexString renders left-to-right; this matches
        // DatatypeUtility::WordToHexString in the C++ source exactly.
        byte[] buffer = new byte[2];
        buffer[0] = (byte) ((value >> 8) & 0xFF);
        buffer[1] = (byte) (value & 0xFF);
        return byteArrayToHexString(buffer, withPrefix);
    }

    public static String sizeToHexString(long value, boolean withPrefix) {
        return unsignedLongHexString(value, withPrefix);
    }

    public static String unsignedLongHexString(long value, boolean withPrefix) {
        String hex = Long.toHexString(value);
        return withPrefix ? "0x" + hex : hex;
    }

    public static String byteArrayToHexString(byte[] valueArray, boolean withPrefix) {
        return byteArrayToHexString(valueArray, valueArray.length, withPrefix);
    }

    public static String byteArrayToHexString(byte[] valueArray, int size, boolean withPrefix) {
        StringBuilder outBuffer = new StringBuilder();
        if (withPrefix) {
            outBuffer.append("0x");
        }
        for (int i = 0; i < size; i++) {
            int value = valueArray[i] & 0xFF;
            outBuffer.append(HEX_CHARACTERS.charAt((value >> 4) & 0xF));
            outBuffer.append(HEX_CHARACTERS.charAt(value & 0xF));
        }
        return outBuffer.toString();
    }

    /** Result holder for the "out parameter" style of the C++ ByteFromString etc. */
    public static final class ByteResult {
        public int value;
    }

    public static final class WordResult {
        public int value;
    }

    public static final class LongResult {
        public long value;
    }

    // The method accepts decimal and hexa-decimal (prefix 0x) input.
    public static boolean byteFromString(ByteResult result, String stringValue) {
        WordResult wordResult = new WordResult();
        if (wordFromString(wordResult, stringValue)) {
            if (wordResult.value <= 255) {
                result.value = wordResult.value;
                return true;
            }
        }
        return false;
    }

    public static boolean wordFromString(WordResult result, String stringValue) {
        LongResult longResult = new LongResult();
        if (unsignedLongFromString(longResult, stringValue)) {
            if (longResult.value <= 65535L) {
                result.value = (int) longResult.value;
                return true;
            }
        }
        return false;
    }

    public static boolean sizeFromString(LongResult result, String stringValue) {
        return unsignedLongFromString(result, stringValue);
    }

    public static boolean unsignedLongFromString(LongResult result, String stringValue) {
        if (stringValue == null || stringValue.length() < 1) {
            return false;
        }
        try {
            if (stringValue.startsWith("0x") || stringValue.startsWith("0X")) {
                result.value = Long.parseLong(stringValue.substring(2), 16);
            } else {
                result.value = Long.parseLong(stringValue, 10);
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Result holder mirroring the "byte*& valueArray, size_t& outSize" out-parameters of
     * DatatypeUtility::ByteArrayFromHexString in the C++ source.
     */
    public static final class ByteArrayResult {
        public byte[] value;
    }

    public static boolean byteArrayFromHexString(ByteArrayResult result, String stringValue) {
        result.value = null;
        int length;
        if (stringValue == null) {
            return false;
        }
        length = stringValue.length();
        if (length < 2) {
            return false;
        }

        int j;
        if (stringValue.charAt(0) == '0' && stringValue.charAt(1) == 'x') {
            j = 2;
            length -= j;
        } else {
            return false;
        }

        if ((length & 0x1) != 0) {
            return false;
        }

        int size = length / 2;
        byte[] buffer = new byte[size];

        for (int i = 0; i < size; i++) {
            String pair = stringValue.substring(j, j + 2);
            j += 2;
            try {
                int value = Integer.parseInt(pair, 16);
                if (value < 0 || value > 255) {
                    return false;
                }
                buffer[i] = (byte) value;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        result.value = buffer;
        return true;
    }

    public static void clearByteArray(byte[] arrayValue) {
        fillByteArray(arrayValue, (byte) 0);
    }

    public static void fillByteArray(byte[] arrayValue, byte value) {
        java.util.Arrays.fill(arrayValue, value);
    }
}
