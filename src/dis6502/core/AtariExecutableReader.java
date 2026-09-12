package dis6502.core;

import java.io.IOException;

/**
 * Direct Java translation of the executable-file-loading half of systems/atari800/Atari800.cpp
 * (Atari800::GuessFileType's header check + Atari800::ReadExecutableFile + Atari800::LoadFixUps),
 * which was not yet ported when the "systems" layer was deferred (see the placeholder comment
 * in ComputerSystem.java). This is what the GUI needs to open anything other than a plain
 * DOS 2.x $FFFF binary -- in particular SpartaDOS X (SDX) executables, whose segments are
 * introduced by one of five other header words instead of $FFFF:
 *
 *   $FFFF  ATARI_BINARY      plain DOS 2.x segment: [start][end][data]
 *   $FFFA  SDX_FIXED_BLK     same layout as ATARI_BINARY, SDX variant
 *   $FFFE  SDX_RELOC_BLK     [blockNum][controlByte][begin][size]tab, optionally + data
 *   $FFFD  SDX_FIX_UP_BLK    [blockNum][size]tab, followed by a fix-up byte stream
 *   $FFFB  SDX_SYM_REQUIRED  [8-char symbol][size]tab, followed by a fix-up byte stream
 *   $FFFC  SDX_SYM_DEFINED   [blockNum][address][8-char symbol]tab (no data, no fix-ups)
 *
 * so a SpartaDOS X executable's first two bytes are legitimately $FFFA-$FFFE, never $FFFF --
 * exactly the files the previous from-scratch parser in Dis6502Gui.openExecutableFile rejected.
 *
 * As in the C++ source, ATARI_BINARY/SDX_FIXED_BLK segments may be introduced either by a
 * repeated header word or, in the far more common case, by the raw 2-byte start address
 * appearing directly after the previous segment's data (no header word at all) -- both forms
 * are handled here, matching the state kept in wHeader/wPreviousHeader/wBegin below.
 */
public final class AtariExecutableReader {

    private AtariExecutableReader() {
    }

    /** Thrown for the same malformed-file cases the C++ source reports via IOException. */
    public static final class FormatException extends IOException {
        public FormatException(String message) {
            super(message);
        }
    }

    // Small cursor over the in-memory file, standing in for the C++ InputStream + bytesRemaining
    // pair (Read(...) in Atari800.cpp).
    private static final class Cursor {
        final byte[] bytes;
        int pos;

        Cursor(byte[] bytes) {
            this.bytes = bytes;
            this.pos = 0;
        }

        int remaining() {
            return bytes.length - pos;
        }

        int readByte() throws FormatException {
            require(1);
            return bytes[pos++] & 0xFF;
        }

        int readWord() throws FormatException {
            require(2);
            int value = (bytes[pos] & 0xFF) | ((bytes[pos + 1] & 0xFF) << 8);
            pos += 2;
            return value;
        }

        void require(int size) throws FormatException {
            if (size > remaining()) {
                throw new FormatException("Computed remaining length of stream of " + remaining()
                        + " is smaller than requested amount of " + size + " to read");
            }
        }
    }

    /**
     * Direct translation of the header portion of Atari800::GuessFileType: returns true if the
     * first two bytes of the file are one of the three headers that mark it as an executable
     * ($FFFF DOS 2.x, or SDX's $FFFA/$FFFE). Used by callers (e.g. the GUI's file-open dialog)
     * that want to sanity-check a file before handing it to readExecutableFile.
     *
     * Note this intentionally mirrors GuessFileType's own (narrower) check, not the full set of
     * headers readExecutableFile accepts as a *first* header ($FFFB/$FFFC/$FFFD are technically
     * only ever continuation headers in practice, exactly as in the C++ source).
     */
    public static boolean looksLikeExecutable(byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }
        int value = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
        return value == FileHeader.ATARI_BINARY.getValue()
                || value == FileHeader.SDX_FIXED_BLK.getValue()
                || value == FileHeader.SDX_RELOC_BLK.getValue();
    }

    /**
     * Direct translation of Atari800::ReadExecutableFile(SegmentListInserter&, InputStream&,
     * FileIO::FILE_SIZE). Appends one Segment per block found in the file to segmentList
     * (via insertSegmentAt, same as the GUI's existing plain-$FFFF parser) and returns the
     * number of segments appended.
     */
    public static int readExecutableFile(SegmentList segmentList, byte[] bytes) throws IOException {
        Cursor cursor = new Cursor(bytes);

        // Check that the file begins with a known header.
        int firstHeaderValue = cursor.readWord();
        FileHeader header = headerFromValue(firstHeaderValue);
        if (header != FileHeader.ATARI_BINARY && header != FileHeader.SDX_FIXED_BLK
                && header != FileHeader.SDX_SYM_REQUIRED && header != FileHeader.SDX_SYM_DEFINED
                && header != FileHeader.SDX_FIX_UP_BLK && header != FileHeader.SDX_RELOC_BLK) {
            throw new FormatException(
                    "Unsupported file header " + DatatypeUtility.wordToHexString(firstHeaderValue, true));
        }

        boolean firstSegment = true;
        FileHeader previousHeader = FileHeader.RAW;
        int begin = 0xFFFF; // Memory::word wBegin = 0xFFFF; (sentinel: "read a start address next")
        int segmentCount = 0;

        while (cursor.remaining() > 0) {
            Segment segment = segmentList.insertSegmentAt(segmentList.getCount());

            switch (header) {
                case SDX_RELOC_BLK: {
                    int sdxBlockNumber = cursor.readByte();
                    int sdxControlByte = cursor.readByte();
                    begin = cursor.readWord();
                    int size = cursor.readWord();
                    int end = (begin + size - 1) & 0xFFFF;

                    segment.setHeader(header);
                    segment.wBegin = begin;
                    segment.wEnd = end;
                    segment.bSDXBlockNumber = sdxBlockNumber;
                    segment.bSDXControlByte = sdxControlByte;
                    segment.bBinary = false;

                    if (segment.isSDXRelocBlkWithData()) {
                        segment.createMemoryBlockFromBeginToEnd();
                        readData(cursor, segment);
                        segment.bBinary = true;
                    } else {
                        // High bit of the control byte set: a "reserve N bytes of RAM" block
                        // (BSS-style), no data follows. wBegin/wEnd above already give its real
                        // size, so allocate that for real (zero-initialized) rather than the
                        // 1-byte FakeSegment placeholder used for e.g. SDX_SYM_DEFINED, keeping
                        // Segment.getSize() in sync with what was actually allocated.
                        segment.createMemoryBlockFromBeginToEnd();
                    }
                    break;
                }

                case SDX_FIX_UP_BLK: {
                    int sdxBlockNumber = cursor.readByte();
                    int sdxFixUpSize = cursor.readWord(); // never seen non-zero in practice, per the C++ comment

                    segment.setHeader(header);
                    segment.wBegin = 0; // wEnd set by loadFixUps
                    segment.bSDXBlockNumber = sdxBlockNumber;
                    segment.wSDXFixUpSize = sdxFixUpSize;
                    segment.bBinary = false;
                    loadFixUps(segment, cursor);
                    break;
                }

                case SDX_SYM_REQUIRED: {
                    String sdxSymbol = readSdxSymbol(cursor);
                    int sdxFixUpSize = cursor.readWord();

                    segment.setHeader(header);
                    segment.wBegin = 0; // wEnd set by loadFixUps
                    segment.wSDXFixUpSize = sdxFixUpSize;
                    segment.bBinary = false;
                    segment.szSDXSymbol = sdxSymbol;
                    loadFixUps(segment, cursor);
                    break;
                }

                case SDX_SYM_DEFINED: {
                    int sdxBlockNumber = cursor.readByte();
                    begin = cursor.readWord();
                    String sdxSymbol = readSdxSymbol(cursor);

                    segment.setHeader(header);
                    segment.wBegin = begin;
                    segment.wEnd = begin;
                    segment.bSDXBlockNumber = sdxBlockNumber;
                    segment.bBinary = false;
                    segment.szSDXSymbol = sdxSymbol;

                    // Mark the segment as used by allocating a 1-byte placeholder buffer: this
                    // block only ever defines a symbol/address pair, it has no real size.
                    segment.createMemoryBlockWithSize(1);
                    break;
                }

                case ATARI_BINARY:
                case SDX_FIXED_BLK:
                    // Read segment start address (unless it was already read speculatively as
                    // the previous iteration's "next segment header", see the bottom of the loop).
                    if (begin == 0xFFFF) {
                        begin = cursor.readWord();
                    }
                    previousHeader = header;
                    // fallthrough is explicit, matching the C++ source's [[fallthrough]]

                default: {
                    // Read segment end address.
                    FileHeader fhBegin = headerFromValue(begin);

                    if (!firstSegment && (fhBegin == FileHeader.ATARI_BINARY || fhBegin == FileHeader.SDX_FIXED_BLK)) {
                        header = fhBegin;
                        begin = cursor.readWord();
                    } else {
                        header = previousHeader;
                    }
                    int end = cursor.readWord();

                    if (end < begin) {
                        throw new FormatException("Segment end address is lower than segment start address");
                    }

                    int size = end - begin + 1;
                    cursor.require(size);

                    segment.setHeader(header);
                    segment.wBegin = begin;
                    segment.wEnd = end;
                    segment.bBinary = true;
                    segment.createMemoryBlockFromBeginToEnd();
                    for (int i = 0; i < size; i++) {
                        segment.setData(i, cursor.bytes[cursor.pos + i] & 0xFF);
                    }
                    cursor.pos += size;

                    // Change segment type if segment is RUNAD (02E0) or INITAD (02E2).
                    if (((begin == 0x02E0) && (size == 2 || size == 4)) || ((begin == 0x02E2) && (size == 2))) {
                        segment.setType(0, MemoryType.LABEL, size);
                    }

                    previousHeader = header;
                    break;
                }
            }

            segmentCount++;

            if (cursor.remaining() == 0) {
                return segmentCount;
            }

            // Read the next segment header (assume it is the begin address with no magic header).
            begin = cursor.readWord();

            // If the begin address is actually a magic header, transfer it to header.
            FileHeader beginAsHeader = headerFromValue(begin);
            if (beginAsHeader == FileHeader.ATARI_BINARY || beginAsHeader == FileHeader.SDX_FIXED_BLK
                    || beginAsHeader == FileHeader.SDX_SYM_REQUIRED || beginAsHeader == FileHeader.SDX_SYM_DEFINED
                    || beginAsHeader == FileHeader.SDX_FIX_UP_BLK || beginAsHeader == FileHeader.SDX_RELOC_BLK) {
                header = beginAsHeader;
                begin = 0xFFFF;
            } else {
                header = previousHeader;
            }

            firstSegment = false;
        }

        return segmentCount;
    }

    /** Direct translation of the free function ReadData(InputStream&, Segment&, FILE_SIZE&). */
    private static void readData(Cursor cursor, Segment segment) throws FormatException {
        int size = segment.getSize();
        cursor.require(size);
        for (int i = 0; i < size; i++) {
            segment.setData(i, cursor.bytes[cursor.pos + i] & 0xFF);
        }
        cursor.pos += size;
    }

    /**
     * Direct translation of Atari800::LoadFixUps: reads a byte-coded fix-up stream (used by
     * SDX_FIX_UP_BLK and SDX_SYM_REQUIRED segments) until the END marker (0xFC), storing the
     * raw fix-up bytes themselves as the segment's memory block (exactly as the C++ source
     * does -- these bytes are only ever displayed/exported raw, not further interpreted here).
     */
    private static void loadFixUps(Segment segment, Cursor cursor) throws FormatException {
        final int bufferSize = 8192; // unknown size so allocate enough for fix-up data, as in the C++ source
        byte[] buffer = new byte[bufferSize];
        int offset = 0;

        int b = cursor.readByte();
        buffer[offset++] = (byte) b;

        while (b != Fixup.FixupType.END.getValue() && offset < (bufferSize - 3)) {
            if (b == Fixup.FixupType.SET_BLOCK_NUM.getValue()) {
                b = cursor.readByte();
                buffer[offset++] = (byte) b;
            } else if (b == Fixup.FixupType.SET_BLOCK_ADDR.getValue()) {
                b = cursor.readByte();
                buffer[offset++] = (byte) b;
                b = cursor.readByte();
                buffer[offset++] = (byte) b;
            }

            b = cursor.readByte();
            buffer[offset++] = (byte) b;
        }

        segment.wBegin = 0;
        segment.wEnd = offset - 1;
        segment.createMemoryBlockFromBeginToEnd();
        for (int i = 0; i < offset; i++) {
            segment.setData(i, buffer[i] & 0xFF);
        }
    }

    private static final int SDX_SYMBOL_LEN = 8; // dis_k::SDX_SYMBOL_LEN, from Syntax.h

    /**
     * Direct translation of the free function ReadSDXSymbol: reads a fixed-length (8 byte,
     * space-padded) SDX symbol name and returns it without trailing spaces.
     */
    private static String readSdxSymbol(Cursor cursor) throws FormatException {
        cursor.require(SDX_SYMBOL_LEN);
        StringBuilder result = new StringBuilder(SDX_SYMBOL_LEN);
        for (int i = 0; i < SDX_SYMBOL_LEN; i++) {
            char c = (char) (cursor.bytes[cursor.pos + i] & 0xFF);
            if (c != ' ') {
                result.append(c);
            } else {
                break;
            }
        }
        cursor.pos += SDX_SYMBOL_LEN;
        return result.toString();
    }

    /** FileHeader.fromValue, but returning RAW instead of throwing for values with no matching constant (e.g. an ordinary address). */
    private static FileHeader headerFromValue(int value) {
        try {
            return FileHeader.fromValue(value);
        } catch (IllegalArgumentException e) {
            return FileHeader.RAW;
        }
    }
}
