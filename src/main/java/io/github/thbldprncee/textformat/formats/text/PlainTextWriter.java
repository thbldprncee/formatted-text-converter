package io.github.thbldprncee.textformat.formats.text;

import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
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
import io.github.thbldprncee.textformat.format.AbstractTextWriter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Readable text without markup: headings are underlined, lists keep their markers,
 * tables are aligned in columns, links show their address.
 */
public final class PlainTextWriter extends AbstractTextWriter {

    @Override
    public String visitParagraph(Paragraph paragraph) {
        return inlines(paragraph.children());
    }

    @Override
    public String visitHeading(Heading heading) {
        String title = inlines(heading.children());
        if (heading.level() > 2) {
            return title;
        }
        return title + "\n" + (heading.level() == 1 ? "=" : "-").repeat(Math.max(3, title.length()));
    }

    @Override
    public String visitCodeBlock(CodeBlock code) {
        return code.code().lines().map(line -> "    " + line).collect(Collectors.joining("\n"));
    }

    @Override
    public String visitList(ListBlock list) {
        return IntStream.range(0, list.items().size())
                .mapToObj(i -> {
                    String marker = list.ordered() ? (i + 1) + ". " : "* ";
                    String body = list.items().get(i).accept(this);
                    return marker + body.replace("\n", "\n" + " ".repeat(marker.length()));
                })
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String visitListItem(ListItem item) {
        return item.blocks().stream().map(block -> block.accept(this)).collect(Collectors.joining("\n"));
    }

    @Override
    public String visitTable(Table table) {
        int columns = table.columnCount();
        List<List<String>> cells = table.rows().stream()
                .map(row -> IntStream.range(0, columns)
                        .mapToObj(i -> i < row.cells().size() ? row.cells().get(i).accept(this) : "")
                        .toList())
                .toList();
        int[] widths = IntStream.range(0, columns)
                .map(col -> cells.stream().mapToInt(row -> row.get(col).length()).max().orElse(0))
                .toArray();
        return cells.stream()
                .map(row -> IntStream.range(0, columns)
                        .mapToObj(col -> row.get(col) + " ".repeat(widths[col] - row.get(col).length()))
                        .collect(Collectors.joining("  ")).stripTrailing())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String visitTableRow(TableRow row) {
        return row.cells().stream().map(cell -> cell.accept(this)).collect(Collectors.joining("  "));
    }

    @Override
    public String visitTableCell(TableCell cell) {
        return inlines(cell.children());
    }

    @Override
    public String visitBlockQuote(BlockQuote quote) {
        return blocks(quote.blocks()).lines()
                .map(line -> line.isEmpty() ? "|" : "| " + line)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String visitThematicBreak(ThematicBreak thematicBreak) {
        return "-".repeat(40);
    }

    @Override
    public String visitText(Text text) {
        return text.value();
    }

    @Override
    public String visitBold(Bold bold) {
        return inlines(bold.children());
    }

    @Override
    public String visitItalic(Italic italic) {
        return inlines(italic.children());
    }

    @Override
    public String visitStrikethrough(Strikethrough strikethrough) {
        return inlines(strikethrough.children());
    }

    @Override
    public String visitInlineCode(InlineCode code) {
        return code.code();
    }

    @Override
    public String visitLink(Link link) {
        String label = inlines(link.children());
        return label.equals(link.url()) ? label : label + " (" + link.url() + ")";
    }

    @Override
    public String visitImage(Image image) {
        return "[" + (image.alt().isEmpty() ? "image" : image.alt()) + "]";
    }
}
