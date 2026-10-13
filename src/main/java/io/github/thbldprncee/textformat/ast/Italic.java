package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record Italic(List<Inline> children) implements Style {

    public Italic {
        children = List.copyOf(children);
    }

    public static Italic of(Inline... children) {
        return new Italic(List.of(children));
    }

    public static Italic of(String text) {
        return of(new Text(text));
    }

    @Override
    public Italic withChildren(List<Inline> children) {
        return new Italic(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitItalic(this);
    }
}
