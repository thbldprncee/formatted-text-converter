package io.github.thbldprncee.textformat.ast;

import java.util.List;
import java.util.Objects;

public record Link(String url, List<Inline> children) implements Inline, InlineContainer {

    public Link {
        Objects.requireNonNull(url, "url");
        children = List.copyOf(children);
    }

    public static Link of(String url, String text) {
        return new Link(url, List.of(new Text(text)));
    }

    @Override
    public Link withChildren(List<Inline> children) {
        return new Link(url, children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitLink(this);
    }
}
