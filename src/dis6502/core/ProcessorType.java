package dis6502.core;

import java.util.List;

/**
 * Direct Java 6 translation of ProcessorType.h / ProcessorType.cpp.
 */
public enum ProcessorType {
    UNKNOWN,
    MOS6502,
    MOS65C02;

    /**
     * Java 6 translation of the ProcessorTypeInfo struct.
     */
    public static final class ProcessorTypeInfo {
        private final ProcessorType processorType;
        private final String key;
        private final String text;

        public ProcessorTypeInfo(ProcessorType processorType, String key, String text) {
            this.processorType = processorType;
            this.key = key;
            this.text = text;
        }

        public ProcessorType getProcessorType() {
            return processorType;
        }

        public String getKey() {
            return key;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Java 6 translation of ProcessorTypeFactory.
     */
    public static final class ProcessorTypeFactory {

        private static final ProcessorTypeInfo UNKNOWN_INFO = new ProcessorTypeInfo(ProcessorType.UNKNOWN, "UNKNOWN", "Unknown");
        private static final ProcessorTypeInfo MOS6502_INFO = new ProcessorTypeInfo(ProcessorType.MOS6502, "MOS6502", "MOS 6502");
        private static final ProcessorTypeInfo MOS65C02_INFO = new ProcessorTypeInfo(ProcessorType.MOS65C02, "MOS65C02", "MOS 65C02");

        private ProcessorTypeFactory() {
        }

        public static ProcessorTypeInfo getInfo(String key) {
            if ("MOS6502".equals(key)) {
                return getInfo(ProcessorType.MOS6502);
            } else if ("MOS65C02".equals(key)) {
                return getInfo(ProcessorType.MOS65C02);
            }
            return getInfo(ProcessorType.UNKNOWN);
        }

        public static ProcessorTypeInfo getInfo(ProcessorType processorType) {
            if (processorType == ProcessorType.MOS6502) {
                return MOS6502_INFO;
            } else if (processorType == ProcessorType.MOS65C02) {
                return MOS65C02_INFO;
            }
            return UNKNOWN_INFO;
        }

        public static int getIndex(ProcessorType processorType, List processorTypes, int defaultIndex) {
            int index = processorTypes.indexOf(processorType);
            if (index < 0) {
                return 0;
            }
            return index;
        }
    }
}
