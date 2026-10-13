package io.github.thbldprncee.textformat.ast;

import java.util.Objects;

/** @param language info string of a fenced block, empty when unknown */
public record CodeBlock(String code, String language) implements Block {

    public CodeBlock {
        Objects.requireNonNull(code, "code");
        language = language == null ? "" : language.strip();
    }

    public CodeBlock(String code) {
        this(code, "");
    }

    public boolean hasLanguage() {
        return !language.isEmpty();
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitCodeBlock(this);
    }
}
