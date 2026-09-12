package dis6502.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of the platform-independent surface of FileIO.h / FileIO.cpp.
 * File-descriptor-based OpenFile/CloseFile and SetCurrentWorkingDirectory (both thin wrappers
 * over Windows/CRT calls with no meaningful cross-platform equivalent worth preserving) are
 * omitted; everything else is a direct, real translation using java.io.
 */
public final class FileIO {

    public static final String FILE_SEPARATOR = File.separator;
    public static final String EMPTY_FILE_PATH = "";

    private FileIO() {
    }

    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static boolean isFileReadOnly(String filePath) {
        File file = new File(filePath);
        return file.exists() && !file.canWrite();
    }

    /** Throws IOException, matching the C++ source's IOException on failure. */
    public static long getFileSize(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Cannot determine file size of file '" + filePath + "': file does not exist");
        }
        return file.length();
    }

    public static byte[] readByteArray(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Cannot open file '" + filePath + "' for reading");
        }
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException("Requested buffer size " + fileSize + " exceeds the memory limits");
        }
        byte[] buffer = new byte[(int) fileSize];
        FileInputStream fis = new FileInputStream(file);
        try {
            int totalRead = 0;
            while (totalRead < buffer.length) {
                int read = fis.read(buffer, totalRead, buffer.length - totalRead);
                if (read < 0) {
                    break;
                }
                totalRead += read;
            }
            if (totalRead != buffer.length) {
                throw new IOException("Cannot read expected " + buffer.length + " bytes from file \""
                        + filePath + "\". Only " + totalRead + " bytes read.");
            }
        } finally {
            fis.close();
        }
        return buffer;
    }

    /** Direct translation of FileIO::ReadString: reads the whole file and decodes it as ISO-8859-1 (see ByteArray.toWString for the same choice). */
    public static String readString(String filePath) throws IOException {
        byte[] textBytes = readByteArray(filePath);
        try {
            return new String(textBytes, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** Direct translation of FileIO::ReadStrings: reads the file line by line (any of \n, \r\n, \r as line endings). */
    public static List<String> readStrings(String filePath) throws IOException {
        List<String> result = new ArrayList<String>();
        FileInputStream fis = new FileInputStream(filePath);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } finally {
            fis.close();
        }
        return result;
    }
}
