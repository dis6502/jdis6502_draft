package dis6502.core;

/**
 * Direct Java 6 translation of the ByteSequence abstract base class (ByteSequence.h).
 * Expressed as an interface since it has no state or shared implementation of its own.
 */
public interface ByteSequence {

    /** Mutable access to the backing array. Corresponds to byte* get(). */
    byte[] get();

    /** Read-only access to the backing array. Corresponds to const byte* getConst() const. */
    byte[] getConst();

    boolean isEmpty();

    int size();

    /** Returns the byte at index as an unsigned value (0-255). */
    int at(int index);

    /**
     * Direct translation of ByteSequence::copyTo. targetOffset is between 0 and less than
     * targetSize; targetSize is the total size of the target, irrespective of targetOffset.
     */
    void copyTo(byte[] target, int targetOffset, int targetSize, int offset, int size);

    ByteSequence getSubSequence(int offset, int size);

    String stringAt(int offset, int size);

    String toWString();
}
