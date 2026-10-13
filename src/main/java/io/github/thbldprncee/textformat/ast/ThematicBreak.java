package io.github.thbldprncee.textformat.ast;

/** A horizontal rule ({@code ---} in Markdown, {@code <hr/>} in XHTML). */
public record ThematicBreak() implements Block {

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitThematicBreak(this);
    }
}
