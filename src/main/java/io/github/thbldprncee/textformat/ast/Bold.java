package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record Bold(List<Inline> children) implements Style {

    public Bold {
        children = List.copyOf(children);
    }

    public static Bold of(Inline... children) {
        return new Bold(List.of(children));
    }

    public static Bold of(String text) {
        return of(new Text(text));
    }

    @Override
    public Bold withChildren(List<Inline> children) {
        return new Bold(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitBold(this);
    }
}
