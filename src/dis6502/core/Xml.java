package dis6502.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Direct Java 6 translation of the persistence-relevant surface of XML.h / XML.cpp.
 *
 * The original wraps tinyxml2 (a ~5,400-line third-party C++ XML library, bundled directly in
 * this project as tinyxml2.h/.cpp); that library itself is out of scope for translation. This
 * class instead uses the DOM APIs built into Java 6 (javax.xml.parsers, org.w3c.dom,
 * javax.xml.transform) to provide the same observable behavior: attribute-based serialization
 * of a tree of Serializable objects to/from a UTF-8 XML file.
 *
 * The many GetXxxAttribute/SetXxxAttribute C++ preprocessor macros in XML.h become plain static
 * methods here (Java has no macros); their names are kept close to the originals
 * (getBoolAttribute/setBoolAttribute, etc). Two navigation helpers (firstChildElement /
 * nextSiblingElement) stand in for tinyxml2::XMLElement::FirstChildElement(name) /
 * NextSiblingElement(name), since org.w3c.dom's Node API is iterated one node at a time and
 * doesn't filter by element name or skip text/comment nodes on its own.
 */
public final class Xml {

    private Xml() {
    }

    /** Direct Java 6 translation of XML::Serializable. */
    public interface Serializable {
        void serializeTo(Element element);

        void deserializeFrom(Element element);
    }

    /** Direct translation of XML::AddChildElement. */
    public static Element addChildElement(Element parent, String elementName) {
        Element child = parent.getOwnerDocument().createElement(elementName);
        parent.appendChild(child);
        return child;
    }

    /** Corresponds to tinyxml2::XMLElement::FirstChildElement(name). */
    public static Element firstChildElement(Element parent, String tagName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && (tagName == null || tagName.equals(((Element) child).getTagName()))) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /** Corresponds to tinyxml2::XMLElement::NextSiblingElement(name). */
    public static Element nextSiblingElement(Element current, String tagName) {
        Node sibling = current.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE
                    && (tagName == null || tagName.equals(((Element) sibling).getTagName()))) {
                return (Element) sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
     * Direct translation of XML::Load(Serializable&, elementName, filePath): parses filePath,
     * checks the root element's tag name matches elementName, and calls
     * serializable.deserializeFrom(root). Throws IOException on any failure (mismatched root
     * element, malformed XML, missing file, etc), where the C++ source returns an XML::Error
     * code instead -- callers that need the old "check a return code" style can catch
     * IOException instead.
     */
    public static void load(Serializable serializable, String elementName, String filePath) throws IOException {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new File(filePath));
        } catch (Exception e) {
            throw new IOException("Cannot load XML file '" + filePath + "': " + e.getMessage(), e);
        }

        Element root = document.getDocumentElement();
        if (root == null || !elementName.equals(root.getTagName())) {
            throw new IOException("Mismatched root element in '" + filePath + "': expected '"
                    + elementName + "' but found '" + (root == null ? "(none)" : root.getTagName()) + "'");
        }
        serializable.deserializeFrom(root);
    }

    /**
     * Direct translation of XML::Save(const Serializable&, elementName, filePath): builds a
     * document with a single root element named elementName, calls
     * serializable.serializeTo(root), and writes it as UTF-8 (with a byte-order mark, matching
     * the C++ source's document.SetBOM(true) for the file-saving overload) to filePath.
     */
    public static void save(Serializable serializable, String elementName, String filePath) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement(elementName);
            document.appendChild(root);
            serializable.serializeTo(root);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            try {
                // UTF-8 byte-order mark, matching document.SetBOM(true) in the C++ source.
                fileOutputStream.write(0xEF);
                fileOutputStream.write(0xBB);
                fileOutputStream.write(0xBF);
                transformer.transform(new DOMSource(document), new StreamResult(fileOutputStream));
            } finally {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot save XML file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    // --- Attribute helpers. Each Get method returns defaultValue if the attribute is absent or
    // unparseable, matching the C++ macros (which leave FIELD untouched in that case). ---

    public static boolean getBoolAttribute(Element element, String name, boolean defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        String value = element.getAttribute(name);
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return defaultValue;
    }

    public static int getByteAttribute(Element element, String name, int defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        DatatypeUtility.ByteResult result = new DatatypeUtility.ByteResult();
        if (DatatypeUtility.byteFromString(result, element.getAttribute(name))) {
            return result.value;
        }
        return defaultValue;
    }

    public static int getWordAttribute(Element element, String name, int defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        DatatypeUtility.WordResult result = new DatatypeUtility.WordResult();
        if (DatatypeUtility.wordFromString(result, element.getAttribute(name))) {
            return result.value;
        }
        return defaultValue;
    }

    public static int getIntAttribute(Element element, String name, int defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(element.getAttribute(name));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getSizeAttribute(Element element, String name, long defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        DatatypeUtility.LongResult result = new DatatypeUtility.LongResult();
        if (DatatypeUtility.sizeFromString(result, element.getAttribute(name))) {
            return result.value;
        }
        return defaultValue;
    }

    public static String getStringAttribute(Element element, String name, String defaultValue) {
        if (!element.hasAttribute(name)) {
            return defaultValue;
        }
        return element.getAttribute(name);
    }

    /** Returns null if the attribute is absent or cannot be parsed as a hex byte array. */
    public static byte[] getByteArrayAttribute(Element element, String name) {
        if (!element.hasAttribute(name)) {
            return null;
        }
        DatatypeUtility.ByteArrayResult result = new DatatypeUtility.ByteArrayResult();
        if (DatatypeUtility.byteArrayFromHexString(result, element.getAttribute(name))) {
            return result.value;
        }
        return null;
    }

    public static void setBoolAttribute(Element element, String name, boolean value) {
        element.setAttribute(name, value ? "true" : "false");
    }

    public static void setByteAttribute(Element element, String name, int value) {
        element.setAttribute(name, String.valueOf(value & 0xFF));
    }

    public static void setByteAttributeHex(Element element, String name, int value) {
        element.setAttribute(name, DatatypeUtility.byteToHexString(value, true));
    }

    public static void setWordAttribute(Element element, String name, int value) {
        element.setAttribute(name, String.valueOf(value & 0xFFFF));
    }

    public static void setWordAttributeHex(Element element, String name, int value) {
        element.setAttribute(name, DatatypeUtility.wordToHexString(value, true));
    }

    public static void setIntAttribute(Element element, String name, int value) {
        element.setAttribute(name, String.valueOf(value));
    }

    public static void setSizeAttributeHex(Element element, String name, long value) {
        element.setAttribute(name, DatatypeUtility.sizeToHexString(value, true));
    }

    public static void setStringAttribute(Element element, String name, String value) {
        element.setAttribute(name, value);
    }

    public static void setByteArrayAttributeHex(Element element, String name, byte[] value) {
        element.setAttribute(name, DatatypeUtility.byteArrayToHexString(value, true));
    }
}
