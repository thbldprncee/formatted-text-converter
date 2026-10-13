package io.github.thbldprncee.textformat.ast;

import java.util.Objects;

public record Image(String url, String alt) implements Inline {

    public Image {
        Objects.requireNonNull(url, "url");
        alt = alt == null ? "" : alt;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitImage(this);
    }
}
