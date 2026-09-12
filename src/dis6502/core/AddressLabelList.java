package dis6502.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Direct Java 6 translation of AddressLabelList.h / AddressLabelList.cpp.
 *
 * Implementation note: the C++ source keeps two structures in sync -- a
 * std::vector<unique_ptr<AddressLabel>> maintained in ascending address order (via manual
 * insertion-point search in AllocateAddressLabel) and a std::map<address, AddressLabel*> for
 * O(log n) lookup by address. A java.util.TreeMap&lt;Integer, AddressLabel&gt; provides both
 * properties directly (sorted iteration order + O(log n) lookup), so this translation uses a
 * single TreeMap instead of replicating the two-structure bookkeeping; externally-observable
 * behavior (iteration order, lookup results) is unchanged.
 */
public final class AddressLabelList {

    /** Java 6 translation of "typedef std::vector<gsl::not_null<AddressLabel*>> AddressLabelVector;". */
    public static final class AddressLabelVector extends ArrayList<AddressLabel> {
        public AddressLabelVector() {
            super();
        }
    }

    private final TreeMap<Integer, AddressLabel> addressLabels = new TreeMap<Integer, AddressLabel>();

    public void enumerate(AddressLabelVector addressLabelsVector) {
        addressLabelsVector.clear();
        Iterator it = addressLabels.values().iterator();
        while (it.hasNext()) {
            addressLabelsVector.add((AddressLabel) it.next());
        }
    }

    public void clear() {
        addressLabels.clear();
    }

    public void allocateAddressLabel(int address) {
        Integer key = new Integer(address);
        if (addressLabels.containsKey(key)) {
            return; // Already allocated
        }
        addressLabels.put(key, new AddressLabel(address));
    }

    public AddressLabel findAddressLabel(int address) {
        return findMutableAddressLabel(address);
    }

    public AddressLabel findMutableAddressLabel(int address) {
        return (AddressLabel) addressLabels.get(new Integer(address));
    }

    /**
     * For all not-yet-aligned labels:
     * - set to "aligned" if their label address matches the specified address
     * - set the nearest address to "address" if the specified address is between the label
     *   address and the current nearest address
     */
    public void alignAddressLabels(int address) {
        Iterator it = addressLabels.values().iterator();
        while (it.hasNext()) {
            AddressLabel addressLabel = (AddressLabel) it.next();
            if (!addressLabel.isAligned()) {
                if (addressLabel.getAddress() == address) {
                    addressLabel.setAligned(true);
                } else if ((addressLabel.getAddress() > address) && (addressLabel.getNearestAddress() < address)) {
                    addressLabel.setNearestAddress(address);
                }
            }
        }
    }

    /** Sets nearestAddress = address for every entry. */
    public void alignNearestAddress() {
        Iterator it = addressLabels.values().iterator();
        while (it.hasNext()) {
            AddressLabel addressLabel = (AddressLabel) it.next();
            addressLabel.setNearestAddress(addressLabel.getAddress());
        }
    }

    public AddressLabel findNearestAddressLabel(int address) {
        AddressLabel result = findAddressLabel(address);
        if (result != null && result.isAligned()) {
            return result;
        }

        Iterator it = addressLabels.values().iterator();
        while (it.hasNext()) {
            AddressLabel addressLabel = (AddressLabel) it.next();
            if (addressLabel.getNearestAddress() == address) {
                return addressLabel;
            }
        }

        return null;
    }
}
