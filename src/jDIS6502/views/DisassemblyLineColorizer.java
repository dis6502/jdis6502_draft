package jDIS6502.views;

import java.util.ArrayList;
import java.util.List;

/**
 * Faithful, close-to-line-for-line port of DisassemblyControlImpl::PrintOneLineInColor's
 * character-by-character state machine (from the user-supplied DisassemblyControlImpl.cpp),
 * minus the actual Win32 GDI painting: this only recognizes which parts of an already-formatted
 * disassembly line are the label, mnemonic, operand, string, hex number, or comment, returning
 * them as an ordered list of colored Runs that (concatenated) reconstruct the input line exactly.
 * Dis6502Gui applies those runs to a JTextPane's StyledDocument -- see rebuildDisassemblyDocument.
 *
 * This recognizes a fixed line layout (label column, then mnemonic column, then operand, then an
 * optional trailing comment) matching exactly how this project's own Disassembly/DisassemblyLine
 * classes format a line -- it is not a general 6502-assembly-syntax parser.
 */
final class DisassemblyLineColorizer {

    private DisassemblyLineColorizer() {
    }

    /** Matches DIS_STATE and the Colors[] array in DisassemblyControlImpl.cpp. */
    enum State {
        NORMAL,        // RGB(0, 0, 0)
        COMMENT,       // RGB(0, 128, 0)
        NUMBER,        // RGB(128, 0, 0)
        STRING,        // RGB(128, 0, 128)
        INSTRUCTION,   // RGB(0, 0, 128)
        UNREFERENCED   // RGB(192, 192, 192)
    }

    static final class Run {
        final String text;
        final State state;

        Run(String text, State state) {
            this.text = text;
            this.state = state;
        }
    }

    /**
     * Tokenizes one already-formatted disassembly line (optionally already carrying a leading
     * line-number prefix -- see lineNumbersActive, matching PrintOneLineInColor's own parameter).
     * referenced mirrors PrintAll's rule (see Dis6502Gui.rebuildDisassemblyDocument): when false,
     * every run in the line is forced to State.UNREFERENCED (rendered in grey) regardless of what
     * it actually is.
     */
    static List<Run> tokenize(String text, boolean referenced, boolean lineNumbersActive) {
        List<Run> runs = new ArrayList<Run>();
        int srcIndex = 0;
        StringBuilder buf = new StringBuilder();
        State state = State.INSTRUCTION;
        char c, c2;
        boolean isNumber;

        if (lineNumbersActive) {
            c = charAt(text, srcIndex++);
            while (c >= '0' && c <= '9') {
                append(buf, c);
                c = charAt(text, srcIndex++);
            }
            append(buf, c);
            flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
        }

        // Label on column 0 must begin with a letter, '@' or '_'; otherwise the whole line is a comment.
        c = charAt(text, srcIndex++);
        if (c == '@' || c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            while (c != '\0' && c != ' ') {
                append(buf, c);
                c = charAt(text, srcIndex++);
            }
            while (c == ' ') {
                append(buf, c);
                c = charAt(text, srcIndex++);
            }
            flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
        } else if (c != ' ') {
            addRestAsComment(runs, text, srcIndex, referenced);
            return runs;
        }

        // Mnemonic/directive column.
        while (c == ' ') {
            append(buf, c);
            c = charAt(text, srcIndex++);
        }
        if (c == '=' || c == '*') {
            state = State.NORMAL;
        }
        while (c != '\0' && c != ' ') {
            append(buf, c);
            c = charAt(text, srcIndex++);
        }
        while (c == ' ') {
            append(buf, c);
            c = charAt(text, srcIndex++);
        }
        flush(runs, referenced ? state : State.UNREFERENCED, buf);

        // Operand: a quoted string, an immediate '#', or plain -- followed by an optional trailing comment.
        if (c == '"' || c == '\'') {
            c2 = c;
            append(buf, c);
            flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
            c = charAt(text, srcIndex++);
            while (c != '\0' && c != c2) {
                append(buf, c);
                c = charAt(text, srcIndex++);
            }
            flush(runs, referenced ? State.STRING : State.UNREFERENCED, buf);
        } else if (c == '#') {
            append(buf, c);
            flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
            c = charAt(text, srcIndex++);
            while (c == ' ') {
                append(buf, c);
                c = charAt(text, srcIndex++);
            }
        }
        do {
            if (c == ',') {
                append(buf, c);
                c = charAt(text, srcIndex++);
                flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
            }
            isNumber = false;
            if (c == '$') {
                isNumber = true;
                append(buf, c);
                c = charAt(text, srcIndex++);
                flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
            }
            if (isNumber) {
                while ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                    append(buf, c);
                    c = charAt(text, srcIndex++);
                }
                flush(runs, referenced ? State.NUMBER : State.UNREFERENCED, buf);
            }
        } while (c == ',');

        // The rest of the line: plain text, then an optional trailing comment.
        if (c == ';') {
            addRestAsComment(runs, text, srcIndex, referenced);
            return runs;
        }
        append(buf, c);
        c = charAt(text, srcIndex++);
        while (c != '\0' && c != ';') {
            append(buf, c);
            c = charAt(text, srcIndex++);
        }
        flush(runs, referenced ? State.NORMAL : State.UNREFERENCED, buf);
        if (c == ';') {
            addRestAsComment(runs, text, srcIndex, referenced);
        }

        return runs;
    }

    private static void addRestAsComment(List<Run> runs, String text, int srcIndex, boolean referenced) {
        String rest = text.substring(srcIndex - 1);
        if (rest.length() > 0) {
            runs.add(new Run(rest, referenced ? State.COMMENT : State.UNREFERENCED));
        }
    }

    /** Mirrors C's szText[wSrcIndex++] reading past a NUL terminator: always '\0' once past the end. */
    private static char charAt(String text, int index) {
        return index < text.length() ? text.charAt(index) : '\0';
    }

    /** Mirrors szBuf[wDestIndex++] = c, but never actually embeds a NUL character in the Java text. */
    private static void append(StringBuilder buf, char c) {
        if (c != '\0') {
            buf.append(c);
        }
    }

    private static void flush(List<Run> runs, State state, StringBuilder buf) {
        if (buf.length() > 0) {
            runs.add(new Run(buf.toString(), state));
        }
        buf.setLength(0);
    }
}
