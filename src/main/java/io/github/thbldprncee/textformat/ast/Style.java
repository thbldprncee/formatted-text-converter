package io.github.thbldprncee.textformat.ast;

/** Purely presentational inline formatting; removed by {@link io.github.thbldprncee.textformat.ast.transform.StyleRemover}. */
public sealed interface Style extends Inline, InlineContainer permits Bold, Italic, Strikethrough {
}
