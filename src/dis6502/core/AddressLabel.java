package dis6502.core;

import java.util.Comparator;

/**
 * Direct Java 6 translation of AddressLabel.h / AddressLabel.cpp.
 * XML serialization is deferred to the persistence-layer pass.
 */
public final class AddressLabel {

    private final int address;      // Memory::address: address of the label
    private int nearestAddress;     // nearest address before address
    private boolean aligned;        // true if the address is at the beginning of an instruction

    public AddressLabel(int address) {
        this.address = address;
        this.nearestAddress = 0;
        this.aligned = false;
    }

    public int getAddress() {
        return address;
    }

    public int getNearestAddress() {
        return nearestAddress;
    }

    public void setNearestAddress(int value) {
        nearestAddress = value;
    }

    public boolean isAligned() {
        return aligned;
    }

    public void setAligned(boolean value) {
        aligned = value;
    }

    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttributeHex(element, "Address", address);
        Xml.setWordAttributeHex(element, "NearestAddress", nearestAddress);
        Xml.setBoolAttribute(element, "Aligned", aligned);
    }

    public void deserializeFrom(org.w3c.dom.Element element) {
        // Deliberate deviation from the C++ source: DeserializeFrom there also reassigns
        // "address" via GetWordAttribute(Address, address) (the field isn't const in the C++
        // header). This method is currently dead code either way -- Segment::SerializeTo/
        // DeserializeFrom never call it (the whole AddressLabel(s) serialization block is
        // commented out there as "for debugging" -- see Segment.java). Since AddressLabelList
        // here keys its TreeMap by address, reassigning it after construction would silently
        // desynchronize the map from its own keys; "address" is kept final and NOT
        // reassignable here, unlike the C++ field, until/unless this path is ever wired up for
        // real, at which point the caller would need to re-key the map instead.
        nearestAddress = Xml.getWordAttribute(element, "NearestAddress", nearestAddress);
        aligned = Xml.getBoolAttribute(element, "Aligned", aligned);
    }

    public static boolean isAddressLess(AddressLabel a, AddressLabel b) {
        return a.getAddress() < b.getAddress();
    }

    public static final Comparator<AddressLabel> ADDRESS_ORDER = new Comparator<AddressLabel>() {
        public int compare(AddressLabel a, AddressLabel b) {
            if (a.getAddress() < b.getAddress()) {
                return -1;
            } else if (a.getAddress() > b.getAddress()) {
                return 1;
            }
            return 0;
        }
    };
}
