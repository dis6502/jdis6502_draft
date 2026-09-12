package dis6502.core;

/**
 * Direct Java 6 translation of ComputerSystemType.h / ComputerSystemType.cpp.
 */
public enum ComputerSystemType {
    ATARI5200,
    ATARI800,
    C64,
    ORIC,
    UNKNOWN;

    /** Direct translation of the ComputerSystemTypeInfo class. */
    public static final class ComputerSystemTypeInfo {
        private final ComputerSystemType type;
        private final String id;
        private final String text;
        private final String fileName;

        public ComputerSystemTypeInfo(ComputerSystemType type, String id, String text, String fileName) {
            this.type = type;
            this.id = id;
            this.text = text;
            this.fileName = fileName;
        }

        public ComputerSystemType getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
