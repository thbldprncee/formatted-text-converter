package io.github.thbldprncee.textformat.ast;

import java.util.List;

/** A node whose content is a sequence of inline elements. */
public interface InlineContainer {

    List<Inline> children();

    /** Returns a copy of this node with other children (nodes are immutable). */
    InlineContainer withChildren(List<Inline> children);

    /** True when the content is plain text only, e.g. {@code **bold**} but not {@code ***bold italic***}. */
    default boolean isPlainText() {
        return children().stream().allMatch(Text.class::isInstance);
    }
}
