package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.ast.Block;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Writes Markdown that {@link MarkdownParser} reads back into the same model: special
 * characters are escaped, nested lists are indented under their item, tables are padded.
 */
public final class MarkdownWriter extends AbstractTextWriter {

    private static final Pattern BACKTICK_RUN = Pattern.compile("`+");
    /** A line starting like this would be read as a block marker, so its first character is escaped. */
    private static final Pattern BLOCK_START = Pattern.compile("^( *)(#|>|[-+*](?=\\s|$)|=+\\s*$|\\d+(?=[.)]))");

    @Override
    public String visitParagraph(Paragraph paragraph) {
        return escapeBlockStart(inlines(paragraph.children()));
    }

    @Override
    public String visitHeading(Heading heading) {
        return "#".repeat(heading.level()) + " " + inlines(heading.children());
    }

    @Override
    public String visitCodeBlock(CodeBlock code) {
        String fence = "`".repeat(Math.max(3, longestRun(code.code(), '`') + 1));
        return fence + code.language() + "\n" + code.code() + (code.code().isEmpty() ? "" : "\n") + fence;
    }

    @Override
    public String visitList(ListBlock list) {
        return IntStream.range(0, list.items().size())
                .mapToObj(i -> item(list.ordered() ? (i + 1) + ". " : "- ", list.items().get(i)))
                .collect(Collectors.joining("\n"));
    }

    /** First block on the marker line, the rest indented to the content column. */
    private String item(String marker, ListItem item) {
        String indent = " ".repeat(marker.length());
        StringBuilder text = new StringBuilder();
        List<Block> blocks = item.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (i > 0) {
                // A nested list hugs its paragraph; other blocks need a blank line.
                text.append(block instanceof ListBlock ? "\n" : "\n\n");
            }
            text.append(block.accept(this));
        }
        return marker + text.toString().replace("\n", "\n" + indent).replaceAll("(?m)^ +$", "");
    }

    @Override
    public String visitListItem(ListItem item) {
        return item("- ", item);
    }

    @Override
    public String visitTable(Table table) {
        if (table.rows().isEmpty()) {
            return "";
        }
        int columns = table.columnCount();
        List<List<String>> cells = table.rows().stream()
                .map(row -> IntStream.range(0, columns)
                        .mapToObj(i -> i < row.cells().size() ? row.cells().get(i).accept(this) : "")
                        .toList())
                .toList();
        int[] widths = IntStream.range(0, columns)
                .map(col -> Math.max(3, cells.stream().mapToInt(row -> row.get(col).length()).max().orElse(0)))
                .toArray();

        List<String> lines = new ArrayList<>();
        lines.add(tableLine(cells.getFirst(), widths));
        lines.add(IntStream.range(0, columns)
                .mapToObj(col -> "-".repeat(widths[col]))
                .collect(Collectors.joining(" | ", "| ", " |")));
        cells.stream().skip(1).map(row -> tableLine(row, widths)).forEach(lines::add);
        return String.join("\n", lines);
    }

    private static String tableLine(List<String> row, int[] widths) {
        return IntStream.range(0, widths.length)
                .mapToObj(col -> row.get(col) + " ".repeat(widths[col] - row.get(col).length()))
                .collect(Collectors.joining(" | ", "| ", " |"));
    }

    @Override
    public String visitTableRow(TableRow row) {
        return row.cells().stream().map(cell -> cell.accept(this)).collect(Collectors.joining(" | ", "| ", " |"));
    }

    @Override
    public String visitTableCell(TableCell cell) {
        return inlines(cell.children()).replace("|", "\\|");
    }

    @Override
    public String visitBlockQuote(BlockQuote quote) {
        return blocks(quote.blocks()).lines()
                .map(line -> line.isEmpty() ? ">" : "> " + line)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String visitThematicBreak(ThematicBreak thematicBreak) {
        return "---";
    }

    @Override
    public String visitText(Text text) {
        return escape(text.value().replace('\n', ' '));
    }

    @Override
    public String visitBold(Bold bold) {
        return "**" + inlines(bold.children()) + "**";
    }

    @Override
    public String visitItalic(Italic italic) {
        return "*" + inlines(italic.children()) + "*";
    }

    @Override
    public String visitStrikethrough(Strikethrough strikethrough) {
        return "~~" + inlines(strikethrough.children()) + "~~";
    }

    @Override
    public String visitInlineCode(InlineCode code) {
        String ticks = "`".repeat(longestRun(code.code(), '`') + 1);
        boolean pad = code.code().startsWith("`") || code.code().endsWith("`");
        String content = pad ? " " + code.code() + " " : code.code();
        return ticks + content + ticks;
    }

    @Override
    public String visitLink(Link link) {
        return "[" + inlines(link.children()) + "](" + url(link.url()) + ")";
    }

    @Override
    public String visitImage(Image image) {
        return "![" + escape(image.alt()) + "](" + url(image.url()) + ")";
    }

    private static String url(String url) {
        return url.matches(".*[\\s()<>].*") ? "<" + url + ">" : url;
    }

    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ("\\`*_[]~<".indexOf(ch) >= 0) {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    private static String escapeBlockStart(String line) {
        Matcher match = BLOCK_START.matcher(line);
        if (!match.find()) {
            return line;
        }
        int at = match.group(1).length();
        if (Character.isDigit(line.charAt(at))) {
            // "1. text" -> "1\. text"
            int dot = at;
            while (dot < line.length() && Character.isDigit(line.charAt(dot))) {
                dot++;
            }
            return line.substring(0, dot) + "\\" + line.substring(dot);
        }
        return line.substring(0, at) + "\\" + line.substring(at);
    }

    private static int longestRun(String text, char ch) {
        Matcher match = BACKTICK_RUN.matcher(ch == '`' ? text : "");
        int longest = 0;
        while (match.find()) {
            longest = Math.max(longest, match.group().length());
        }
        return longest;
    }
}
