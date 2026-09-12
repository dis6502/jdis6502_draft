package dis6502.core;

import java.util.List;

/**
 * Direct Java 6 translation of LabelAccess.h / LabelAccess (enum class in the C++ source).
 * Describes how an instruction's operand accesses a label/address (read, write, both).
 */
public enum LabelAccess {

    UNKNOWN(0),
    READ(1),
    WRITE(2),
    READ_WRITE(3),
    IMMEDIATE(4);

    private final int value;

    private LabelAccess(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Java 6 translation of LabelAccessInfo (struct in LabelAccess.h).
     */
    public static final class LabelAccessInfo {
        private final LabelAccess labelAccess;
        private final String key;
        private final String text;

        public LabelAccessInfo(LabelAccess labelAccess, String key, String text) {
            this.labelAccess = labelAccess;
            this.key = key;
            this.text = text;
        }

        public LabelAccess getLabelAccess() {
            return labelAccess;
        }

        public String getKey() {
            return key;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Java 6 translation of LabelAccessFactory (LabelAccess.h / presumed LabelAccess.cpp).
     */
    public static final class LabelAccessFactory {

        private static final LabelAccessInfo UNKNOWN_INFO = new LabelAccessInfo(LabelAccess.UNKNOWN, "UNKNOWN", "Unknown");
        private static final LabelAccessInfo READ_INFO = new LabelAccessInfo(LabelAccess.READ, "READ", "Read");
        private static final LabelAccessInfo WRITE_INFO = new LabelAccessInfo(LabelAccess.WRITE, "WRITE", "Write");
        private static final LabelAccessInfo READ_WRITE_INFO = new LabelAccessInfo(LabelAccess.READ_WRITE, "READ_WRITE", "Read-Write");
        // Note: the C++ GetInfo(LabelAccess) never returns an IMMEDIATE info struct (falls through to UNKNOWN),
        // faithfully reproduced here even though it looks like an oversight in the original code.
        private static final LabelAccessInfo IMMEDIATE_INFO = new LabelAccessInfo(LabelAccess.IMMEDIATE, "IMMEDIATE", "Immediate");

        private LabelAccessFactory() {
        }

        public static LabelAccessInfo getInfo(String key) {
            if ("READ".equals(key)) {
                return READ_INFO;
            } else if ("WRITE".equals(key)) {
                return WRITE_INFO;
            } else if ("READ_WRITE".equals(key)) {
                return READ_WRITE_INFO;
            } else if ("IMMEDIATE".equals(key)) {
                return IMMEDIATE_INFO;
            }
            return UNKNOWN_INFO;
        }

        public static LabelAccessInfo getInfo(LabelAccess labelAccess) {
            // Faithful translation: the original GetInfo(LabelAccess) only special-cases
            // READ, WRITE and READ_WRITE; IMMEDIATE falls through to UNKNOWN, same as the C++.
            if (labelAccess == LabelAccess.READ) {
                return READ_INFO;
            } else if (labelAccess == LabelAccess.WRITE) {
                return WRITE_INFO;
            } else if (labelAccess == LabelAccess.READ_WRITE) {
                return READ_WRITE_INFO;
            }
            return UNKNOWN_INFO;
        }

        public static int getIndex(LabelAccess labelAccess, List labelAccessList, int defaultIndex) {
            int index = labelAccessList.indexOf(labelAccess);
            if (index < 0) {
                return 0;
            }
            return index;
        }

        /**
         * Java 6 translation of LabelAccessFactory::GetQualifier.
         * Returns the read/write qualifier text used in disassembly comments.
         */
        public static String getQualifier(LabelAccess labelAccess) {
            switch (labelAccess) {
                case READ:
                    return "<";
                case WRITE:
                    return ">";
                case READ_WRITE:
                    return "=";
                case IMMEDIATE:
                    return "#";
                default:
                    return "?";
            }
        }
    }
}
