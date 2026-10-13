package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record ListItem(List<Block> blocks) implements Node {

    public ListItem {
        blocks = List.copyOf(blocks);
    }

    public static ListItem of(Block... blocks) {
        return new ListItem(List.of(blocks));
    }

    /** Shortcut for the common case of an item that is a single line of text. */
    public static ListItem of(Inline... content) {
        return new ListItem(List.of(new Paragraph(List.of(content))));
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitListItem(this);
    }
}
