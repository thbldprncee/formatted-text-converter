package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record TableCell(List<Inline> children) implements Node, InlineContainer {

    public TableCell {
        children = List.copyOf(children);
    }

    public static TableCell of(Inline... children) {
        return new TableCell(List.of(children));
    }

    public static TableCell of(String text) {
        return text.isEmpty() ? new TableCell(List.of()) : of(new Text(text));
    }

    @Override
    public TableCell withChildren(List<Inline> children) {
        return new TableCell(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitTableCell(this);
    }
}
