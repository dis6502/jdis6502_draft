package dis6502.core;

/**
 * Direct Java 6 translation of MemoryBlockIterator.h / MemoryBlockIterator.cpp.
 * A simple forward-only cursor over a MemoryBlock's data/type bytes.
 */
public final class MemoryBlockIterator {

    private final MemoryBlock memoryBlock;
    private int index;

    public MemoryBlockIterator(MemoryBlock memoryBlock) {
        this.memoryBlock = memoryBlock;
        this.index = 0;
    }

    public boolean hasNext() {
        return index < memoryBlock.getSize();
    }

    public int getData() {
        return memoryBlock.getDataAt(index);
    }

    public int nextData() {
        int result = getData();
        next();
        return result;
    }

    /** Direct translation of MemoryBlockIterator::NextAddress(): reads a little-endian word. */
    public int nextAddress() {
        int low = getData();
        next();
        int high = getData();
        next();
        return Memory.toAddress(low, high);
    }

    public MemoryType getType() {
        return memoryBlock.getTypeAt(index);
    }

    public void next() {
        if (!hasNext()) {
            throw new RuntimeException("End of memory block reached");
        }
        index++;
    }
}
