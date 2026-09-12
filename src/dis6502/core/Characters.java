package dis6502.core;

/**
 * Direct Java 6 translation of Character.h.
 * Named "Characters" to avoid clashing with java.lang.Character. Matches the "C" locale
 * behavior of the original ctype.h-backed isspace/isalpha/isalnum (i.e. plain ASCII), which
 * is what the Windows build effectively used.
 */
public final class Characters {

    private Characters() {
    }

    public static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
