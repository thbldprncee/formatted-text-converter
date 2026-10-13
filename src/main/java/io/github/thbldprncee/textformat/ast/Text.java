package io.github.thbldprncee.textformat.ast;

import java.util.Objects;

public record Text(String value) implements Inline {

    public Text {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitText(this);
    }
}
