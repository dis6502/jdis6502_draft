package dis6502.core;

import java.util.Arrays;

/**
 * Direct Java 6 translation of ByteArray.h / ByteArray.cpp.
 *
 * A ByteArray is a fixed-size, modifiable owner of a byte[]. In the C++ source, copying a
 * ByteArray deep-copies the underlying buffer; the same behavior is preserved here via an
 * explicit copy constructor (Java has no copy-constructor language feature, nor operator
 * overloading, so callers that relied on C++ copy semantics should call
 * {@code new ByteArray(existing)} explicitly instead of a plain assignment).
 */
public final class ByteArray implements ByteSequence {

    private byte[] array;

    /** Creates an own empty byte[]. Corresponds to ByteArray(). */
    public ByteArray() {
        this(0);
    }

    /** Creates an own byte[] of the given size, zero-filled. Corresponds to ByteArray(size_t size). */
    public ByteArray(int size) {
        this.array = new byte[size];
    }

    /**
     * Takes over the given byte[] (no copy). Corresponds to ByteArray(byte array[], size_t size).
     * Note: unlike the C++ constructor, "size" is implied by array.length; a size parameter
     * is kept for signature parity but is otherwise unused (Java arrays always know their length).
     */
    public ByteArray(byte[] array, int size) {
        this.array = array;
    }

    /** Creates a copy of the given ByteArray's byte[]. Corresponds to the C++ copy constructor. */
    public ByteArray(ByteArray other) {
        this.array = new byte[other.array.length];
        System.arraycopy(other.array, 0, this.array, 0, other.array.length);
    }

    /**
     * Assigns the contents of another ByteArray into this one, resizing if needed.
     * Corresponds to ByteArray::operator=(const ByteArray&) / AssignFrom.
     */
    public ByteArray assignFrom(ByteArray other) {
        if (this == other) {
            return this;
        }
        if (this.array.length != other.array.length) {
            this.array = new byte[other.array.length];
        }
        System.arraycopy(other.array, 0, this.array, 0, other.array.length);
        return this;
    }

    public byte[] get() {
        return array;
    }

    public byte[] getConst() {
        return array;
    }

    public boolean isEmpty() {
        return array.length == 0;
    }

    public int size() {
        return array.length;
    }

    public int at(int index) {
        if (index < array.length) {
            return array[index] & 0xFF;
        }
        throw new RuntimeException("Array index " + index + " is larger than " + (array.length - 1));
    }

    public void copyTo(byte[] target, int targetOffset, int targetSize, int offset, int size) {
        if (offset + size > array.length) {
            throw new RuntimeException("Source offset plus size exceed size of this byte array");
        }
        if (targetOffset + size > targetSize) {
            throw new RuntimeException("Target offset plus size exceed size of target byte array");
        }
        System.arraycopy(array, offset, target, targetOffset, size);
    }

    public ByteSequence getSubSequence(int offset, int size) {
        if (offset >= array.length) {
            return new ByteArray();
        }
        int copySize = Math.min(array.length - offset, size);
        byte[] resultArray = new byte[copySize];
        System.arraycopy(array, offset, resultArray, 0, copySize);
        return new ByteArray(resultArray, copySize);
    }

    /**
     * Direct translation of ByteArray::stringAt, implemented with the intended memcpy-style
     * semantics (copy "size" bytes starting at "offset") rather than the literal C++ source,
     * which passes "offset" as the stop-character argument of _memccpy(dest, src, c, count) --
     * that looks like a transcription bug in the original (it should have been
     * memcpy(buffer, _array + offset, resultSize)), since _memccpy's third parameter is a
     * byte value to search for, not a source offset. This translation implements the
     * evidently-intended behavior instead of reproducing that bug.
     */
    public String stringAt(int offset, int size) {
        if (offset >= array.length) {
            return "";
        }
        int resultSize = Math.min(array.length - offset, size);
        byte[] buffer = new byte[resultSize];
        System.arraycopy(array, offset, buffer, 0, resultSize);
        // Original uses plain 8-bit "string" (ANSI/Latin-1 style); ISO-8859-1 preserves byte values 1:1.
        try {
            return new String(buffer, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toWString() {
        return DatatypeUtility.byteArrayToHexString(array, true);
    }

    public void setAt(int index, int value) {
        if (index >= array.length) {
            throw new RuntimeException("Array index " + index + " is larger than " + (array.length - 1));
        }
        array[index] = (byte) value;
    }

    /** Fills "size" bytes starting at "index" with "value". Corresponds to setAt(index, value, size). */
    public void setAt(int index, int value, int size) {
        if (index >= array.length) {
            throw new RuntimeException("Array index " + index + " is larger than " + (array.length - 1));
        }
        if (index + size - 1 >= array.length) {
            throw new RuntimeException("Array index " + index + " plus size " + size
                    + " is larger than " + (array.length - 1));
        }
        Arrays.fill(array, index, index + size, (byte) value);
    }
}
