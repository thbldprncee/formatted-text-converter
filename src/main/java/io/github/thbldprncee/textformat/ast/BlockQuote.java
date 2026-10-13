package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record BlockQuote(List<Block> blocks) implements Block {

    public BlockQuote {
        blocks = List.copyOf(blocks);
    }

    public static BlockQuote of(Block... blocks) {
        return new BlockQuote(List.of(blocks));
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitBlockQuote(this);
    }
}
