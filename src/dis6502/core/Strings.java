package dis6502.core;

/**
 * Java 6 translation of the small subset of String.h / String.cpp (the C++ project's own
 * "String" utility class, not to be confused with java.lang.String) that Segment/SegmentList
 * depend on. Named "Strings" to avoid clashing with java.lang.String.
 *
 * Only String::Trim and String::Empty are needed for this pass; the remaining members of the
 * original String class (Format, wstring/UTF-8 conversions, ToLower, FindAndReplaceAll, etc.)
 * belong to later passes that need them.
 */
public final class Strings {

    private Strings() {
    }

    private static final String WHITESPACE = " \n\r\t\f\u000B"; // \v is \u000B

    public static String empty() {
        return "";
    }

    /**
     * Direct translation of String::Trim(wstring_view s): trims only the exact whitespace
     * set " \n\r\t\f\v" from both ends (not Java's broader definition of whitespace).
     */
    public static String trim(String s) {
        if (s == null) {
            return "";
        }
        int start = 0;
        int end = s.length() - 1;
        while (start <= end && WHITESPACE.indexOf(s.charAt(start)) >= 0) {
            start++;
        }
        while (end >= start && WHITESPACE.indexOf(s.charAt(end)) >= 0) {
            end--;
        }
        if (start > end) {
            return "";
        }
        return s.substring(start, end + 1);
    }

    /** Direct translation of String::ToLower(wstring_view): plain ASCII lower-casing. */
    public static String toLower(String s) {
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c - 'A' + 'a');
            }
            result.append(c);
        }
        return result.toString();
    }
}
