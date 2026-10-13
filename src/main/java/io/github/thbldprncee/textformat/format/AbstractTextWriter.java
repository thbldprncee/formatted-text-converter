package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.ast.Block;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.NodeVisitor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class of writers that render the document as a string tree (Markdown, plain text,
 * LaTeX, RTF). Each node is rendered by a visitor method returning its text; this class
 * provides the shared plumbing: block separation and inline concatenation.
 */
public abstract class AbstractTextWriter implements DocumentWriter, NodeVisitor<String> {

    @Override
    public final void write(Document document, Appendable out) throws IOException {
        out.append(document.accept(this));
    }

    @Override
    public String visitDocument(Document document) {
        String body = blocks(document.blocks());
        return body.isEmpty() ? "" : body + "\n";
    }

    /** Separator between consecutive blocks; a blank line by default. */
    protected String blockSeparator() {
        return "\n\n";
    }

    protected String blocks(List<Block> blocks) {
        return blocks.stream().map(block -> block.accept(this)).collect(Collectors.joining(blockSeparator()));
    }

    protected String inlines(List<Inline> inlines) {
        return inlines.stream().map(inline -> inline.accept(this)).collect(Collectors.joining());
    }
}
