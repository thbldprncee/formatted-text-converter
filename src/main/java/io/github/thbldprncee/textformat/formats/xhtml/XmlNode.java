package io.github.thbldprncee.textformat.formats.xhtml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lightweight XML tree produced by {@link XmlReader}. */
sealed interface XmlNode {

    record Element(String name, Map<String, String> attributes, List<XmlNode> children) implements XmlNode {

        public Element {
            name = name.toLowerCase(Locale.ROOT);
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
            children = List.copyOf(children);
        }

        Optional<String> attribute(String key) {
            return Optional.ofNullable(attributes.get(key));
        }
    }

    record TextNode(String text) implements XmlNode {
    }
}
