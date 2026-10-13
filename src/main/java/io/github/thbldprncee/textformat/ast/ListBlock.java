package io.github.thbldprncee.textformat.ast;

import java.util.List;

/** A bullet ({@code ordered = false}) or numbered list. Items may contain nested lists. */
public record ListBlock(boolean ordered, List<ListItem> items) implements Block {

    public ListBlock {
        items = List.copyOf(items);
    }

    public static ListBlock bullet(ListItem... items) {
        return new ListBlock(false, List.of(items));
    }

    public static ListBlock numbered(ListItem... items) {
        return new ListBlock(true, List.of(items));
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitList(this);
    }
}
