package dis6502.core;

/**
 * Direct Java 6 translation of MemoryBlock.h / MemoryBlock.cpp.
 * A MemoryBlock pairs a "data" byte array (the actual bytes of the segment) with a parallel
 * "type" byte array (a MemoryType per byte, describing how the disassembler should interpret
 * that byte: code, string, low/high byte of an address, etc).
 *
 * ReadData/WriteData/ReadType (the legacy Workspace14 binary-stream I/O used by
 * Segment::Load14/CreateMemoryBlockFromFile) are still deferred -- that old binary format has
 * no active callers in this translation. XML serialization (the current, actively-used format)
 * is implemented below.
 */
public final class MemoryBlock {

    private int size;
    private ByteArray data;
    private ByteArray type;

    public MemoryBlock() {
        this.size = 0;
        this.data = new ByteArray();
        this.type = new ByteArray();
    }

    /** Direct translation of MemoryBlock::Create(Memory::size size). */
    public void create(int size) {
        this.size = size;
        this.data = new ByteArray(size);
        this.type = new ByteArray(size);

        data.setAt(0, 0x00, size);
        type.setAt(0, MemoryType.UNKNOWN.toByte(), size);
    }

    /** Direct translation of MemoryBlock::Clear(). */
    public void clear() {
        data = new ByteArray();
        type = new ByteArray();
        size = 0;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public ByteSequence getMutableData() {
        return data;
    }

    public ByteSequence getData() {
        return data;
    }

    public int getDataAt(int offset) {
        return data.at(offset);
    }

    public void setDataAt(int offset, int value) {
        data.setAt(offset, value);
    }

    /** Direct translation of MemoryBlock::SetDataAt(offset, const ByteSequence&, dataOffset, dataSize). */
    public void setDataAt(int offset, ByteSequence source, int dataOffset, int dataSize) {
        for (int i = 0; i < dataSize; i++) {
            setDataAt(offset + i, source.at(dataOffset + i));
        }
    }

    public ByteSequence getMutableType() {
        return type;
    }

    public ByteSequence getType() {
        return type;
    }

    public MemoryType getTypeAt(int offset) {
        return MemoryType.fromByte(type.at(offset));
    }

    public void setTypeAt(int offset, MemoryType memoryType) {
        type.setAt(offset, memoryType.toByte());
    }

    public void setTypeAt(int offset, MemoryType memoryType, int size) {
        type.setAt(offset, memoryType.toByte(), size);
    }

    /**
     * Direct translation of MemoryBlock::CopyTo(startOffset, size, MemoryBlock& target, targetOffset).
     */
    public void copyTo(int startOffset, int copySize, MemoryBlock target, int targetOffset) {
        data.copyTo(target.data.get(), targetOffset, target.getSize(), startOffset, copySize);
        type.copyTo(target.type.get(), targetOffset, target.getSize(), startOffset, copySize);
    }

    /** Direct translation of MemoryBlock::SerializeTo. */
    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setSizeAttributeHex(element, "Size", size);
        Xml.setByteArrayAttributeHex(element, "Data", data.get());
        Xml.setByteArrayAttributeHex(element, "Type", type.get());
    }

    /**
     * Direct translation of MemoryBlock::DeserializeFrom. Accepts the legacy "Dump" attribute
     * name (used by workspace files saved by version 3.6) as a fallback for "Data", same as the
     * C++ source.
     */
    public void deserializeFrom(org.w3c.dom.Element element) {
        clear();
        long declaredSize = Xml.getSizeAttribute(element, "Size", 0);

        byte[] dataBytes = Xml.getByteArrayAttribute(element, "Data");
        if (dataBytes == null) {
            dataBytes = Xml.getByteArrayAttribute(element, "Dump"); // For compatibility with 3.6.
        }
        if (dataBytes == null) {
            throw new RuntimeException("Attribute \"Data\" of memory block is missing for element " + element.getTagName());
        }
        if (dataBytes.length != declaredSize) {
            throw new RuntimeException("Size of content array is different from size of memory block");
        }

        byte[] typeBytes = Xml.getByteArrayAttribute(element, "Type");
        if (typeBytes == null) {
            throw new RuntimeException("Attribute \"Type\" of memory block is missing for element " + element.getTagName());
        }
        if (typeBytes.length != declaredSize) {
            throw new RuntimeException("Size of type array is different from size of memory block");
        }

        create((int) declaredSize);
        System.arraycopy(dataBytes, 0, data.get(), 0, dataBytes.length);
        System.arraycopy(typeBytes, 0, type.get(), 0, typeBytes.length);
    }
}
