package dis6502.core;

/**
 * Direct Java 6 translation of Pass1.h / Pass1.cpp.
 * Pass 1: attach SDX label fix-ups on each segment.
 */
public final class Pass1 {

    private Pass1() {
    }

    public static void execute(Workspace workspace) {
        int startAddr = 0;

        SegmentList segmentList = workspace.getSegmentList();
        for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
            Segment segment = segmentList.getConstSegment(segmentIndex);

            if (segment.isHeader(FileHeader.SDX_SYM_DEFINED) || segment.isSDXRelocBlkWithoutData()) {
                // TODO (same as the C++ source): with or without? Original code:
                // if ((lpSegment->IsHeader(SDX_RELOC_BLK)) && (lpSegment->bSDXControlByte & 0x80))
                Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);

                if (labelSegment != null) {
                    startAddr = segment.wBegin;
                    labelSegment.getFixupAddressLabels().allocateAddressLabel(startAddr);
                }
            } else if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED) || segment.isHeader(FileHeader.SDX_FIX_UP_BLK)) {
                Segment blockSegment = null;
                startAddr = 0;
                int sdxBlockNumber = 1;

                MemoryBlockIterator i = new MemoryBlockIterator(segment.memoryBlock);
                while (i.hasNext() && i.getData() != Fixup.FixupType.END.getValue()) {
                    int data = i.nextData();

                    // Direct translation of the C++ "switch (static_cast<FixupType>(data))",
                    // including its intentional fall-through from SET_BLOCK_ADDR into default
                    // ("data = 0; // fall thru !").
                    if (data == Fixup.FixupType.ADD_250_BYTES.getValue()) {
                        startAddr += 250;

                    } else if (data == Fixup.FixupType.SET_BLOCK_NUM.getValue()) {
                        sdxBlockNumber = i.nextData();
                        blockSegment = segmentList.findBySDXBlockNumber(sdxBlockNumber);
                        if (blockSegment != null) {
                            startAddr = blockSegment.wBegin;
                        }

                    } else {
                        if (data == Fixup.FixupType.SET_BLOCK_ADDR.getValue()) {
                            startAddr = i.nextAddress();
                            int iSegmentIndex = segmentList.findByAddr(startAddr);
                            if (iSegmentIndex != SegmentList.NO_SEGMENT_INDEX) {
                                blockSegment = segmentList.getSegment(iSegmentIndex);
                            } else {
                                blockSegment = null;
                            }
                            data = 0;
                            // Fall through into the "default" behavior below, same as the C++ source.
                        }

                        // "default" case (also reached via SET_BLOCK_ADDR's fall-through above).
                        startAddr += data;
                        if (blockSegment != null) {
                            if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED)) {
                                blockSegment.allocateSymbol(startAddr, segment.szSDXSymbol);
                            } else if (segment.isHeader(FileHeader.SDX_FIX_UP_BLK)) {
                                Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);
                                if (labelSegment != null) {
                                    blockSegment.allocateFixup(segmentList.getSegmentIndex(labelSegment), labelSegment, startAddr);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
