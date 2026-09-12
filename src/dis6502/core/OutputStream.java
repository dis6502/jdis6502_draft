package dis6502.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Direct Java 6 translation of the encoding-aware writing behavior of OutputStream.h / .cpp.
 * The C++ source opens a raw Win32/CRT file descriptor (_wsopen) and writes to it directly;
 * this translation uses java.io.FileOutputStream instead, since the raw-fd mechanics are a
 * platform implementation detail with no meaningful behavior of their own. The per-encoding
 * character validation (ASCII/ATASCII/UTF8, throwing on out-of-range characters) is preserved
 * exactly, since that IS meaningful, user-visible behavior.
 */
public final class OutputStream {

    private final String filePath;
    private final FileOutputStream fileOutputStream;
    private final Encoding encoding;
    private boolean closed = false;

    public static OutputStream openFile(String filePath, Encoding encoding) throws IOException {
        if (encoding == Encoding.UNKNOWN) {
            throw new IOException("Cannot write files if encoding is unknown");
        }

        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            throw new IOException("Cannot open file \"" + filePath + "\" for write access: " + e.getMessage(), e);
        }
        return new OutputStream(filePath, fos, encoding);
    }

    private OutputStream(String filePath, FileOutputStream fileOutputStream, Encoding encoding) {
        this.filePath = filePath;
        this.fileOutputStream = fileOutputStream;
        this.encoding = encoding;
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            fileOutputStream.close();
        }
    }

    public void writeString(String value) throws IOException {
        if (encoding == Encoding.UNKNOWN) {
            throw new RuntimeException("Invalid encoding");
        } else if (encoding == Encoding.BINARY) {
            throw new IOException("Cannot write strings if encoding is binary");
        } else if (encoding == Encoding.ASCII) {
            byte[] buffer = new byte[value.length()];
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == 10 || (32 <= c && c <= 127)) {
                    buffer[i] = (byte) c;
                } else {
                    throw new IOException("Character '" + c + "' (" + (int) c + ") at position " + i
                            + " of string '" + value + "' is no ASCII character and cannot be written in ASCII encoding mode.");
                }
            }
            write(buffer);
        } else if (encoding == Encoding.ATASCII) {
            byte[] buffer = new byte[value.length()];
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                // Faithful translation of a bug in the C++ source: this bound check compares
                // against 2555, which looks like it should have been 255 (a single byte's
                // range). As written, characters in the range 256-2555 are silently truncated
                // to a single byte (data corruption) instead of being rejected; only characters
                // above 2555 are actually caught. Preserved as-is rather than "fixed".
                if (c <= 2555) {
                    buffer[i] = (byte) c;
                } else {
                    throw new IOException("Character '" + c + "' (" + (int) c + ") at position " + i
                            + " of string '" + value + "' is no ASCII character and cannot be written in ATASCII encoding mode.");
                }
            }
            write(buffer);
        } else if (encoding == Encoding.UTF8) {
            try {
                write(value.getBytes("UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void write(byte[] buffer) throws IOException {
        fileOutputStream.write(buffer);
    }
}
