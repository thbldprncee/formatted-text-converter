package io.github.thbldprncee.textformat.ast.transform;

import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.Text;

import java.util.ArrayList;
import java.util.List;

/** Normalisation: joins adjacent text nodes and drops empty ones. */
public final class TextMerger extends InlineRewriter {

    @Override
    protected List<Inline> rewriteInlines(List<Inline> inlines) {
        List<Inline> merged = new ArrayList<>();
        for (Inline inline : super.rewriteInlines(inlines)) {
            if (inline instanceof Text text) {
                if (text.value().isEmpty()) {
                    continue;
                }
                if (!merged.isEmpty() && merged.getLast() instanceof Text previous) {
                    merged.set(merged.size() - 1, new Text(previous.value() + text.value()));
                    continue;
                }
            }
            merged.add(inline);
        }
        return List.copyOf(merged);
    }
}
