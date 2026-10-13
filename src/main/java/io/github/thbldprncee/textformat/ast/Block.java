package io.github.thbldprncee.textformat.ast;

/** A block-level element: stacked vertically inside a document, list item or quote. */
public sealed interface Block extends Node
        permits Paragraph, Heading, CodeBlock, ListBlock, Table, BlockQuote, ThematicBreak {
}
