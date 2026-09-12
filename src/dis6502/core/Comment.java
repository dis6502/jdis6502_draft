package dis6502.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of Comment.h / Comment.cpp.
 * XML serialization (SerializeTo/DeserializeFrom in the C++ source) is deferred to the
 * persistence-layer pass, once the XML wrapper classes have been translated.
 */
public final class Comment {

    private int offset; // Memory::offset: memory offset of the byte owning the comment
    private String text; // text of the comment (can be multi-line)

    public Comment() {
        this.offset = 0;
        this.text = "";
    }

    public void clear() {
        offset = 0;
        setText("");
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        // Trim trailing (and leading) whitespace, same as Comment::SetText.
        text = Strings.trim(value);
    }

    public void serializeTo(org.w3c.dom.Element element) {
        Xml.setWordAttributeHex(element, "Offset", offset);
        Xml.setStringAttribute(element, "Text", text);
    }

    public void deserializeFrom(org.w3c.dom.Element element) {
        offset = Xml.getWordAttribute(element, "Offset", offset);
        // Direct field assignment, matching the C++ source's GetStdStringAttribute macro,
        // which bypasses SetText()'s trimming (i.e. text is NOT re-trimmed on load).
        text = Xml.getStringAttribute(element, "Text", text);
    }

    /** Java 6 translation of "typedef std::vector<gsl::not_null<Comment*>> CommentList;". */
    public static final class CommentList extends ArrayList<Comment> {
        public CommentList() {
            super();
        }

        public CommentList(List<Comment> initial) {
            super(initial);
        }
    }
}
