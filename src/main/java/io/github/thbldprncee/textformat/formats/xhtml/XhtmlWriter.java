package io.github.thbldprncee.textformat.formats.xhtml;

import io.github.thbldprncee.textformat.ast.Block;
import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Nodes;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Strikethrough;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.common.IndentedOutput;
import io.github.thbldprncee.textformat.format.AbstractStructuredWriter;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Well-formed XHTML 1.0 document. Block elements go on their own lines, indented with the
 * configured style; inline content stays on the line of its block; code is never re-indented.
 */
public final class XhtmlWriter extends AbstractStructuredWriter {

    public XhtmlWriter(WriterOptions options) {
        super(options);
    }

    @Override
    public void write(Document document, Appendable target) {
        IndentedOutput out = new IndentedOutput(target, indent());
        out.line("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .line("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">")
                .line("<html xmlns=\"http://www.w3.org/1999/xhtml\">").indent()
                .line("<head>").indent()
                .line("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>")
                .line("<title>" + escape(title(document)) + "</title>").dedent()
                .line("</head>")
                .line("<body>").indent();
        document.blocks().forEach(block -> block(block, out));
        out.dedent().line("</body>").dedent().line("</html>");
    }

    private static String title(Document document) {
        return document.blocks().stream()
                .filter(Heading.class::isInstance)
                .map(Nodes::plainText)
                .findFirst()
                .orElse("Document");
    }

    private void block(Block block, IndentedOutput out) {
        switch (block) {
            case Paragraph paragraph -> out.line("<p>" + inlines(paragraph.children()) + "</p>");
            case Heading heading -> out.line("<h%d>%s</h%d>".formatted(heading.level(), inlines(heading.children()), heading.level()));
            case CodeBlock code -> out.line("<pre><code" + (code.hasLanguage()
                    ? " class=\"language-" + escape(code.language()) + "\"" : "") + ">" + escape(code.code()) + "</code></pre>");
            case ListBlock list -> {
                String tag = list.ordered() ? "ol" : "ul";
                out.line("<" + tag + ">").indent();
                list.items().forEach(item -> item(item, out));
                out.dedent().line("</" + tag + ">");
            }
            case Table table -> table(table, out);
            case BlockQuote quote -> {
                out.line("<blockquote>").indent();
                quote.blocks().forEach(child -> block(child, out));
                out.dedent().line("</blockquote>");
            }
            case ThematicBreak ignored -> out.line("<hr/>");
        }
    }

    private void item(ListItem item, IndentedOutput out) {
        if (item.blocks().size() == 1 && item.blocks().getFirst() instanceof Paragraph paragraph) {
            out.line("<li>" + inlines(paragraph.children()) + "</li>");
            return;
        }
        out.line("<li>").indent();
        item.blocks().forEach(block -> block(block, out));
        out.dedent().line("</li>");
    }

    private void table(Table table, IndentedOutput out) {
        out.line("<table>").indent();
        if (!table.rows().isEmpty()) {
            out.line("<thead>").indent();
            row(table.rows().getFirst(), "th", out);
            out.dedent().line("</thead>");
        }
        if (table.rows().size() > 1) {
            out.line("<tbody>").indent();
            table.rows().stream().skip(1).forEach(row -> row(row, "td", out));
            out.dedent().line("</tbody>");
        }
        out.dedent().line("</table>");
    }

    private void row(TableRow row, String cellTag, IndentedOutput out) {
        out.line(row.cells().stream()
                .map(cell -> "<%s>%s</%s>".formatted(cellTag, inlines(cell.children()), cellTag))
                .collect(Collectors.joining("", "<tr>", "</tr>")));
    }

    private String inlines(List<Inline> inlines) {
        return inlines.stream().map(this::inline).collect(Collectors.joining());
    }

    private String inline(Inline inline) {
        return switch (inline) {
            case Text text -> escape(text.value());
            case Bold bold -> "<strong>" + inlines(bold.children()) + "</strong>";
            case Italic italic -> "<em>" + inlines(italic.children()) + "</em>";
            case Strikethrough strike -> "<del>" + inlines(strike.children()) + "</del>";
            case InlineCode code -> "<code>" + escape(code.code()) + "</code>";
            case Link link -> "<a href=\"" + escape(link.url()) + "\">" + inlines(link.children()) + "</a>";
            case Image image -> "<img src=\"" + escape(image.url()) + "\" alt=\"" + escape(image.alt()) + "\"/>";
        };
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
