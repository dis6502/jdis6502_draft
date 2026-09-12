package dis6502.core;

/**
 * Direct Java 6 translation of EquateType.h / EquateType.cpp.
 */
public enum EquateType {
    UNKNOWN,
    EMPTY,
    COMMENT,
    LABEL;

    /** Direct translation of the EquateTypeInfo struct. */
    public static final class EquateTypeInfo {
        private final EquateType equateType;
        private final String key;
        private final String text;

        public EquateTypeInfo(EquateType equateType, String key, String text) {
            this.equateType = equateType;
            this.key = key;
            this.text = text;
        }

        public EquateType getEquateType() {
            return equateType;
        }

        public String getKey() {
            return key;
        }

        public String getText() {
            return text;
        }
    }

    /** Direct translation of EquateTypeFactory. */
    public static final class EquateTypeFactory {

        private static final EquateTypeInfo UNKNOWN_INFO = new EquateTypeInfo(EquateType.UNKNOWN, "UNKNOWN", "Unknown");
        private static final EquateTypeInfo EMPTY_INFO = new EquateTypeInfo(EquateType.EMPTY, "EMPTY", "Empty Line");
        private static final EquateTypeInfo COMMENT_INFO = new EquateTypeInfo(EquateType.COMMENT, "COMMENT", "Comment Line");
        private static final EquateTypeInfo LABEL_INFO = new EquateTypeInfo(EquateType.LABEL, "LABEL", "Label Line");

        private EquateTypeFactory() {
        }

        public static EquateTypeInfo getInfo(String key) {
            if ("EMPTY".equals(key)) {
                return getInfo(EquateType.EMPTY);
            } else if ("COMMENT".equals(key)) {
                return getInfo(EquateType.COMMENT);
            } else if ("LABEL".equals(key)) {
                return getInfo(EquateType.LABEL);
            }
            return getInfo(EquateType.UNKNOWN);
        }

        public static EquateTypeInfo getInfo(EquateType equateType) {
            if (equateType == EquateType.EMPTY) {
                return EMPTY_INFO;
            } else if (equateType == EquateType.COMMENT) {
                return COMMENT_INFO;
            } else if (equateType == EquateType.LABEL) {
                return LABEL_INFO;
            }
            return UNKNOWN_INFO;
        }

        public static int getIndex(EquateType equateType, java.util.List<EquateType> equateTypes, int defaultIndex) {
            int index = equateTypes.indexOf(equateType);
            if (index < 0) {
                return 0;
            }
            return index;
        }
    }
}
