package io.github.thbldprncee.textformat.ast;

/**
 * Visitor over the document model. Writers implement it to render every node type;
 * adding a node type breaks compilation of every visitor until it is handled.
 *
 * @param <R> result of visiting a node ({@link Void} for visitors with side effects)
 */
public interface NodeVisitor<R> {

    R visitDocument(Document document);

    R visitParagraph(Paragraph paragraph);

    R visitHeading(Heading heading);

    R visitCodeBlock(CodeBlock codeBlock);

    R visitList(ListBlock list);

    R visitListItem(ListItem item);

    R visitTable(Table table);

    R visitTableRow(TableRow row);

    R visitTableCell(TableCell cell);

    R visitBlockQuote(BlockQuote quote);

    R visitThematicBreak(ThematicBreak thematicBreak);

    R visitText(Text text);

    R visitBold(Bold bold);

    R visitItalic(Italic italic);

    R visitStrikethrough(Strikethrough strikethrough);

    R visitInlineCode(InlineCode code);

    R visitLink(Link link);

    R visitImage(Image image);
}
