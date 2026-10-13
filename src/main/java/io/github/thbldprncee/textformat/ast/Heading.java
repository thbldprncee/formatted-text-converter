package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record Heading(int level, List<Inline> children) implements Block, InlineContainer {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 6;

    public Heading {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Heading level must be 1..6, got " + level);
        }
        children = List.copyOf(children);
    }

    public static Heading of(int level, Inline... children) {
        return new Heading(level, List.of(children));
    }

    /** Clamps levels coming from lenient sources (e.g. JSON "level": 9) into 1..6. */
    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    @Override
    public Heading withChildren(List<Inline> children) {
        return new Heading(level, children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitHeading(this);
    }
}
