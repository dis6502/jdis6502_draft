package dis6502.core;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Direct Java 6 translation of Fixup.h / Fixup.cpp.
 * XML serialization is deferred to the persistence-layer pass.
 */
public final class Fixup {

    /** Direct translation of "enum class FixupType : Memory::byte". */
    public enum FixupType {
        ADD_250_BYTES(0xFF),
        SET_BLOCK_NUM(0xFE),
        SET_BLOCK_ADDR(0xFD),
        END(0xFC);

        private final int value;

        private FixupType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private int address;          // Memory::address: address of the word to fix-up in the segment
    private int labelSegmentIndex; // segment where this address is located

    public Fixup() {
        this.address = 0;
        this.labelSegmentIndex = 0;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getLabelSegmentIndex() {
        return labelSegmentIndex;
    }

    public void setLabelSegmentIndex(int labelSegmentIndex) {
        this.labelSegmentIndex = labelSegmentIndex;
    }

    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttributeHex(element, "Address", address);
        Xml.setIntAttribute(element, "LabelSegmentIndex", labelSegmentIndex);
    }

    public void deserializeFrom(org.w3c.dom.Element element) {
        address = Xml.getWordAttribute(element, "Address", address);
        labelSegmentIndex = Xml.getIntAttribute(element, "LabelSegmentIndex", labelSegmentIndex);
    }

    /**
     * Direct translation of Fixup::IsAddressLess, exposed as a Comparator so it can be used
     * directly with Java 6's Collections.sort(list, comparator).
     */
    public static final Comparator<Fixup> ADDRESS_ORDER = new Comparator<Fixup>() {
        public int compare(Fixup a, Fixup b) {
            if (a.getAddress() < b.getAddress()) {
                return -1;
            } else if (a.getAddress() > b.getAddress()) {
                return 1;
            }
            return 0;
        }
    };

    public static boolean isAddressLess(Fixup a, Fixup b) {
        return a.getAddress() < b.getAddress();
    }

    /** Java 6 translation of "typedef std::vector<gsl::not_null<Fixup*>> FixupList;". */
    public static final class FixupList extends ArrayList<Fixup> {
        public FixupList() {
            super();
        }
    }
}
