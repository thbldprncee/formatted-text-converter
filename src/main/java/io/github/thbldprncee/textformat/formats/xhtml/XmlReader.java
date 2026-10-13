package io.github.thbldprncee.textformat.formats.xhtml;

import io.github.thbldprncee.textformat.common.CharCursor;
import io.github.thbldprncee.textformat.formats.xhtml.XmlNode.Element;
import io.github.thbldprncee.textformat.formats.xhtml.XmlNode.TextNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small non-validating XML reader: elements, attributes, character and entity references,
 * comments, CDATA, processing instructions and DOCTYPE. Well-formedness errors (mismatched or
 * unclosed tags) are reported with their position. HTML void elements may omit the slash.
 */
final class XmlReader {

    private static final Set<String> VOID_ELEMENTS = Set.of("br", "hr", "img", "meta", "link", "input", "col", "base");
    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("nbsp", " "), Map.entry("mdash", "—"),
            Map.entry("ndash", "–"), Map.entry("hellip", "…"), Map.entry("laquo", "«"),
            Map.entry("raquo", "»"), Map.entry("copy", "©"));

    private final CharCursor cursor;

    private XmlReader(String input) {
        this.cursor = new CharCursor(input);
    }

    /** Parses the content into a synthetic root element holding the top-level nodes. */
    static Element parse(String input) {
        XmlReader reader = new XmlReader(input.startsWith("﻿") ? input.substring(1) : input);
        List<XmlNode> nodes = reader.content(null);
        return new Element("#root", Map.of(), nodes);
    }

    private List<XmlNode> content(String parent) {
        List<XmlNode> nodes = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        while (!cursor.atEnd()) {
            if (cursor.startsWith("</")) {
                if (parent == null) {
                    throw cursor.error("Unexpected closing tag");
                }
                break;
            }
            if (cursor.consume("<!--")) {
                cursor.readUntil("-->");
                cursor.expect("-->");
            } else if (cursor.consume("<![CDATA[")) {
                text.append(cursor.readUntil("]]>"));
                cursor.expect("]]>");
            } else if (cursor.consume("<?")) {
                cursor.readUntil("?>");
                cursor.expect("?>");
            } else if (cursor.startsWith("<!")) {
                cursor.readUntil(">");
                cursor.expect('>');
            } else if (cursor.peek() == '<') {
                flushText(text, nodes);
                nodes.add(element());
            } else if (cursor.peek() == '&') {
                text.append(entity());
            } else {
                text.append(cursor.next());
            }
        }
        if (parent != null && cursor.atEnd()) {
            throw cursor.error("Unclosed tag <" + parent + ">");
        }
        flushText(text, nodes);
        return nodes;
    }

    private Element element() {
        cursor.expect('<');
        String name = name();
        if (name.isEmpty()) {
            throw cursor.error("Expected a tag name");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        while (true) {
            cursor.skipWhitespace();
            if (cursor.consume("/>")) {
                return new Element(name, attributes, List.of());
            }
            if (cursor.consume('>')) {
                break;
            }
            String key = name();
            if (key.isEmpty()) {
                throw cursor.error("Malformed attribute in <" + name + ">");
            }
            cursor.skipWhitespace();
            String value = "";
            if (cursor.consume('=')) {
                cursor.skipWhitespace();
                value = attributeValue();
            }
            attributes.put(key.toLowerCase(Locale.ROOT), value);
        }
        if (VOID_ELEMENTS.contains(name.toLowerCase(Locale.ROOT))) {
            if (cursor.startsWith("</" + name)) {
                closingTag(name);
            }
            return new Element(name, attributes, List.of());
        }
        List<XmlNode> children = content(name);
        closingTag(name);
        return new Element(name, attributes, children);
    }

    private void closingTag(String name) {
        cursor.expect("</");
        String closing = name();
        cursor.skipWhitespace();
        cursor.expect('>');
        if (!closing.equalsIgnoreCase(name)) {
            throw cursor.error("Mismatched closing tag </%s>, expected </%s>".formatted(closing, name));
        }
    }

    private String attributeValue() {
        char quote = cursor.peek();
        if (quote != '"' && quote != '\'') {
            return cursor.readWhile(ch -> !Character.isWhitespace(ch) && ch != '>' && ch != '/');
        }
        cursor.next();
        StringBuilder value = new StringBuilder();
        while (cursor.peek() != quote) {
            if (cursor.atEnd()) {
                throw cursor.error("Unterminated attribute value");
            }
            value.append(cursor.peek() == '&' ? entity() : String.valueOf(cursor.next()));
        }
        cursor.next();
        return value.toString();
    }

    private String entity() {
        cursor.expect('&');
        String reference = cursor.readWhile(ch -> ch != ';' && ch != '<' && ch != '&' && !Character.isWhitespace(ch));
        if (!cursor.consume(';')) {
            return "&" + reference; // a stray ampersand, keep it literally
        }
        if (reference.startsWith("#x") || reference.startsWith("#X")) {
            return Character.toString(Integer.parseInt(reference.substring(2), 16));
        }
        if (reference.startsWith("#")) {
            return Character.toString(Integer.parseInt(reference.substring(1)));
        }
        String resolved = ENTITIES.get(reference);
        if (resolved == null) {
            throw cursor.error("Unknown entity &" + reference + ";");
        }
        return resolved;
    }

    private String name() {
        return cursor.readWhile(ch -> Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':' || ch == '.');
    }

    private static void flushText(StringBuilder text, List<XmlNode> nodes) {
        if (!text.isEmpty()) {
            nodes.add(new TextNode(text.toString()));
            text.setLength(0);
        }
    }
}
