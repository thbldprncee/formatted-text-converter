package io.github.thbldprncee.textformat.ast;

/** An inline element: flows inside a paragraph, heading, table cell or style. */
public sealed interface Inline extends Node permits Text, Style, InlineCode, Link, Image {
}
