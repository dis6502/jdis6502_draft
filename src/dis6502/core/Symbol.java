package dis6502.core;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Direct Java 6 translation of Symbol.h / Symbol.cpp.
 * XML serialization is deferred to the persistence-layer pass.
 */
public final class Symbol {

    private int address; // Memory::address: address of the word to fix-up in the segment
    private String symbol; // symbol name to fix-up

    public Symbol(int address, String symbol) {
        this.address = address;
        this.symbol = symbol;
    }

    public int getAddress() {
        return address;
    }

    public String getSymbol() {
        return symbol;
    }

    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttributeHex(element, "Address", address);
        Xml.setStringAttribute(element, "Symbol", symbol);
    }

    public void deserializeFrom(org.w3c.dom.Element element) {
        address = Xml.getWordAttribute(element, "Address", address);
        symbol = Xml.getStringAttribute(element, "Symbol", symbol);
    }

    public static boolean isAddressLess(Symbol a, Symbol b) {
        return a.getAddress() < b.getAddress();
    }

    public static final Comparator<Symbol> ADDRESS_ORDER = new Comparator<Symbol>() {
        public int compare(Symbol a, Symbol b) {
            if (a.getAddress() < b.getAddress()) {
                return -1;
            } else if (a.getAddress() > b.getAddress()) {
                return 1;
            }
            return 0;
        }
    };

    /** Java 6 translation of "typedef std::vector<std::unique_ptr<Symbol>> SymbolList;". */
    public static final class SymbolList extends ArrayList<Symbol> {
        public SymbolList() {
            super();
        }
    }
}
