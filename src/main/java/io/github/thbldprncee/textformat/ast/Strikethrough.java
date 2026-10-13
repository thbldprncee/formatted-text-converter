package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record Strikethrough(List<Inline> children) implements Style {

    public Strikethrough {
        children = List.copyOf(children);
    }

    public static Strikethrough of(Inline... children) {
        return new Strikethrough(List.of(children));
    }

    public static Strikethrough of(String text) {
        return of(new Text(text));
    }

    @Override
    public Strikethrough withChildren(List<Inline> children) {
        return new Strikethrough(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitStrikethrough(this);
    }
}
