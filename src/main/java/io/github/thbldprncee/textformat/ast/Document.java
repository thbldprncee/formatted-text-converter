package io.github.thbldprncee.textformat.ast;

import java.util.ArrayList;
import java.util.List;

public record Document(List<Block> blocks) implements Node {

    public Document {
        blocks = List.copyOf(blocks);
    }

    public static Document of(Block... blocks) {
        return new Document(List.of(blocks));
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitDocument(this);
    }

    /** Fluent construction of documents (Builder). */
    public static final class Builder {

        private final List<Block> blocks = new ArrayList<>();

        private Builder() {
        }

        public Builder add(Block block) {
            blocks.add(block);
            return this;
        }

        public Builder heading(int level, String text) {
            return add(new Heading(level, List.of(new Text(text))));
        }

        public Builder paragraph(Inline... content) {
            return add(new Paragraph(List.of(content)));
        }

        public Builder paragraph(String text) {
            return paragraph(new Text(text));
        }

        public Builder code(String code, String language) {
            return add(new CodeBlock(code, language));
        }

        public Document build() {
            return new Document(blocks);
        }
    }
}
