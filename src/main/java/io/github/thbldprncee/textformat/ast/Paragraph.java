package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record Paragraph(List<Inline> children) implements Block, InlineContainer {

    public Paragraph {
        children = List.copyOf(children);
    }

    public static Paragraph of(Inline... children) {
        return new Paragraph(List.of(children));
    }

    @Override
    public Paragraph withChildren(List<Inline> children) {
        return new Paragraph(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitParagraph(this);
    }
}
