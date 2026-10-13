package io.github.thbldprncee.textformat.ast.transform;

import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.Style;

import java.util.List;

/**
 * Removes all purely presentational formatting (bold, italic, strikethrough), keeping the text.
 * Structure (headings, lists, tables, links, code) is preserved.
 */
public final class StyleRemover extends InlineRewriter {

    private static final DocumentTransformer INSTANCE = new StyleRemover().then(new TextMerger());

    /** Removes styles and merges the text pieces that were split by them. */
    public static DocumentTransformer instance() {
        return INSTANCE;
    }

    @Override
    protected List<Inline> rewrite(Inline inline) {
        if (inline instanceof Style style) {
            return rewriteInlines(style.children());
        }
        return super.rewrite(inline);
    }
}
