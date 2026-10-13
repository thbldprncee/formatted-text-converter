package io.github.thbldprncee.textformat.ast;

import java.util.Objects;

public record InlineCode(String code) implements Inline {

    public InlineCode {
        Objects.requireNonNull(code, "code");
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitInlineCode(this);
    }
}
