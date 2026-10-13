package io.github.thbldprncee.textformat.ast;

/**
 * Root of the format-independent document model (Composite).
 * <p>
 * The hierarchy is sealed: every format reads into and writes from exactly these types,
 * and the compiler checks that switches over them are exhaustive.
 */
public sealed interface Node permits Document, Block, Inline, ListItem, TableRow, TableCell {

    <R> R accept(NodeVisitor<R> visitor);
}
