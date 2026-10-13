package io.github.thbldprncee.textformat;

import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Strikethrough;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;

/** Documents that exercise every element of the model. */
public final class TestDocuments {

    private TestDocuments() {
    }

    /** Everything the model can express; Markdown, JSON and XHTML round-trip it exactly. */
    public static Document full() {
        return Document.of(
                Heading.of(1, new Text("Title")),
                Paragraph.of(new Text("Plain "), Bold.of("bold"), new Text(" and "), Italic.of("italic"),
                        new Text(", "), Strikethrough.of("gone"), new Text(" with "), new InlineCode("code()"),
                        new Text(" and "), Link.of("https://example.com/a?b=1", "a link"), new Text(".")),
                Heading.of(2, new Text("Section "), Italic.of("two")),
                Paragraph.of(new Image("img/logo.png", "Alt text")),
                ListBlock.bullet(
                        ListItem.of(new Text("One")),
                        ListItem.of(Paragraph.of(new Text("Two")),
                                ListBlock.numbered(ListItem.of(new Text("Two.1")), ListItem.of(new Text("Two.2")))),
                        ListItem.of(new Text("Three"))),
                ListBlock.numbered(ListItem.of(new Text("First")), ListItem.of(Bold.of("Second"))),
                new CodeBlock("int main() {\n    return 0;\n}", "cpp"),
                Table.of(
                        TableRow.of(TableCell.of("Name"), TableCell.of("Value")),
                        TableRow.of(TableCell.of(Bold.of("a")), TableCell.of("1")),
                        TableRow.of(TableCell.of("b"), TableCell.of(new InlineCode("x|y")))),
                BlockQuote.of(Paragraph.of(new Text("Quoted")), BlockQuote.of(Paragraph.of(new Text("Nested")))),
                new ThematicBreak(),
                Paragraph.of(new Text("Кириллица: «ёлка» — 5 €")));
    }

    /** The subset RTF keeps exactly (quotes hold paragraphs only, no inline styles in headings). */
    public static Document rtfSubset() {
        return Document.of(
                Heading.of(1, new Text("Title")),
                Heading.of(3, new Text("Section "), Italic.of("three")),
                Paragraph.of(new Text("Plain "), Bold.of("bold"), new Text(" and "), Italic.of("italic"),
                        new Text(", "), Strikethrough.of("gone"), new Text(" with "), new InlineCode("code()"),
                        new Text(" and "), Link.of("https://example.com", "a link"), new Text(".")),
                Paragraph.of(new Image("img/logo.png", "Alt text")),
                ListBlock.bullet(
                        ListItem.of(new Text("One")),
                        ListItem.of(Paragraph.of(new Text("Two")),
                                ListBlock.numbered(ListItem.of(new Text("Two.1")), ListItem.of(new Text("Two.2")))),
                        ListItem.of(new Text("Three"))),
                new CodeBlock("int main() {\n    return 0;\n}", "cpp"),
                Table.of(
                        TableRow.of(TableCell.of("Name"), TableCell.of("Value")),
                        TableRow.of(TableCell.of(Bold.of("a")), TableCell.of("1"))),
                BlockQuote.of(Paragraph.of(new Text("Quoted")), BlockQuote.of(Paragraph.of(new Text("Nested")))),
                new ThematicBreak(),
                Paragraph.of(new Text("Кириллица: «ёлка» — 5 € {braces} \\ backslash")));
    }
}
