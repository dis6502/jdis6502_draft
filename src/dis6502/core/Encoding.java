package dis6502.core;

import java.util.List;

/**
 * Direct Java 6 translation of Encoding.h / Encoding.cpp.
 * The "flags" field (originally an _O_TEXT/_O_BINARY/_O_U8TEXT open()-flag constant, used only
 * on the Windows file-I/O path) is kept as a named enum instead of the raw platform-specific
 * int, since those constants have no meaning outside the Win32 CRT; the file-I/O layer that
 * once consumed them is itself deferred to a later pass.
 */
public enum Encoding {
    UNKNOWN,
    ASCII,
    ATASCII,
    BINARY,
    UTF8;

    /** Stand-in for the Win32 CRT open() flags (_O_TEXT / _O_BINARY / _O_U8TEXT) once used here. */
    public enum OpenMode {
        NONE,
        TEXT,
        BINARY,
        UTF8_TEXT
    }

    public static final class EncodingInfo {
        private final Encoding encoding;
        private final String key;
        private final String text;
        private final String newline;
        private final OpenMode openMode;

        public EncodingInfo(Encoding encoding, String key, String text, String newline, OpenMode openMode) {
            this.encoding = encoding;
            this.key = key;
            this.text = text;
            this.newline = newline;
            this.openMode = openMode;
        }

        public Encoding getEncoding() {
            return encoding;
        }

        public String getKey() {
            return key;
        }

        public String getText() {
            return text;
        }

        public String getNewline() {
            return newline;
        }

        public OpenMode getOpenMode() {
            return openMode;
        }
    }

    public static final class EncodingFactory {

        private EncodingFactory() {
        }

        public static EncodingInfo getInfo(String key) {
            Encoding encoding = Encoding.UNKNOWN;
            if ("ASCII".equals(key)) {
                encoding = Encoding.ASCII;
            } else if ("BINARY".equals(key)) {
                encoding = Encoding.BINARY;
            } else if ("ATASCII".equals(key)) {
                encoding = Encoding.ATASCII;
            } else if ("UTF8".equals(key)) {
                encoding = Encoding.UTF8;
            }
            return getInfo(encoding);
        }

        public static EncodingInfo getInfo(Encoding encoding) {
            if (encoding == Encoding.ASCII) {
                return new EncodingInfo(Encoding.ASCII, "ASCII", "ASCII", "\n", OpenMode.TEXT);
            } else if (encoding == Encoding.ATASCII) {
                return new EncodingInfo(Encoding.ATASCII, "ATASCII", "ATASCII", "\u009b", OpenMode.BINARY);
            } else if (encoding == Encoding.BINARY) {
                return new EncodingInfo(Encoding.BINARY, "BINARY", "Binary", "\n", OpenMode.BINARY);
            } else if (encoding == Encoding.UTF8) {
                return new EncodingInfo(Encoding.UTF8, "UTF8", "UTF-8", "\n", OpenMode.UTF8_TEXT);
            }
            return new EncodingInfo(Encoding.UNKNOWN, "UNKNOWN", "Unknown", "", OpenMode.NONE);
        }

        public static int getIndex(Encoding encoding, List<Encoding> encodings, int defaultIndex) {
            int index = encodings.indexOf(encoding);
            if (index < 0) {
                return 0;
            }
            return index;
        }
    }
}
