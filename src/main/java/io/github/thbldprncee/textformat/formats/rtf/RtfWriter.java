package io.github.thbldprncee.textformat.formats.rtf;

import io.github.thbldprncee.textformat.ast.Block;
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
import io.github.thbldprncee.textformat.format.AbstractTextWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Rich Text Format 1.9 with the ANSI character set. Characters outside ASCII are written as
 * {@code \\uN?} escapes, so any text survives. Visual formatting uses standard control words
 * (outline levels, indents, real tables, HYPERLINK fields); structure that RTF cannot express
 * directly is recorded in ignorable {@code {\\*\\tfmeta ...}} groups, which other readers skip.
 */
public final class RtfWriter extends AbstractTextWriter {

    static final String META = "tfmeta";
    private static final int[] HEADING_SIZES = {40, 34, 30, 28, 26, 24}; // half-points
    private static final int INDENT = 720; // twips per nesting level
    private static final int CELL_WIDTH = 2400;

    private int listDepth;
    private int quoteDepth;

    @Override
    public String visitDocument(Document document) {
        return "{\\rtf1\\ansi\\ansicpg1252\\deff0\\uc1\n"
                + "{\\fonttbl{\\f0\\fswiss Arial;}{\\f1\\fmodern Courier New;}}\n"
                + "{\\colortbl;\\red5\\green99\\blue193;}\n"
                + blocks(document.blocks())
                + (document.isEmpty() ? "" : "\n")
                + "}\n";
    }

    @Override
    protected String blockSeparator() {
        return "\n";
    }

    @Override
    public String visitParagraph(Paragraph paragraph) {
        String meta = quoteDepth > 0 ? "q " + quoteDepth : "";
        return paragraph(indent(listDepth + quoteDepth), meta, inlines(paragraph.children()));
    }

    @Override
    public String visitHeading(Heading heading) {
        return "\\pard\\plain\\s%d\\outlinelevel%d\\sb240\\sa120\\f0\\b\\fs%d %s\\b0\\par".formatted(
                heading.level(), heading.level() - 1, HEADING_SIZES[heading.level() - 1], inlines(heading.children()));
    }

    @Override
    public String visitCodeBlock(CodeBlock code) {
        String body = code.code().lines().map(RtfWriter::escape).collect(Collectors.joining("\\line "));
        // Content follows the meta group's closing brace directly: a space there would be text.
        return "\\pard\\plain" + indent(listDepth + quoteDepth) + "\\f1\\fs20" + meta("code " + escape(code.language()))
                + body + "\\par";
    }

    @Override
    public String visitList(ListBlock list) {
        listDepth++;
        try {
            return IntStream.range(0, list.items().size())
                    .mapToObj(i -> item(list.items().get(i), list.ordered() ? (i + 1) + "." : "\\bullet", list.ordered()))
                    .collect(Collectors.joining("\n"));
        } finally {
            listDepth--;
        }
    }

    private String item(ListItem item, String marker, boolean ordered) {
        List<String> parts = new ArrayList<>();
        List<Block> blocks = item.blocks();
        boolean firstIsParagraph = !blocks.isEmpty() && blocks.getFirst() instanceof Paragraph;
        String first = firstIsParagraph ? inlines(((Paragraph) blocks.getFirst()).children()) : "";
        parts.add("\\pard\\plain" + indent(listDepth) + "\\fi-360\\f0\\fs24"
                + meta("li %d %d".formatted(listDepth, ordered ? 1 : 0)) + marker + "\\tab " + first + "\\par");
        for (int i = firstIsParagraph ? 1 : 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            parts.add(block instanceof Paragraph paragraph
                    ? paragraph(indent(listDepth), "lic " + listDepth, inlines(paragraph.children()))
                    : block.accept(this));
        }
        return String.join("\n", parts);
    }

    @Override
    public String visitListItem(ListItem item) {
        return item(item, "\\bullet", false);
    }

    @Override
    public String visitTable(Table table) {
        int columns = Math.max(1, table.columnCount());
        String cellBorders = IntStream.rangeClosed(1, columns)
                .mapToObj(i -> "\\clbrdrt\\brdrs\\clbrdrl\\brdrs\\clbrdrb\\brdrs\\clbrdrr\\brdrs\\cellx" + i * CELL_WIDTH)
                .collect(Collectors.joining());
        return table.rows().stream()
                .map(row -> "\\trowd\\trgaph108" + cellBorders + "\n" + row.accept(this))
                .collect(Collectors.joining("\n")) + "\n\\pard";
    }

    @Override
    public String visitTableRow(TableRow row) {
        return row.cells().stream().map(cell -> cell.accept(this)).collect(Collectors.joining()) + "\\row";
    }

    @Override
    public String visitTableCell(TableCell cell) {
        return "\\pard\\intbl\\plain\\f0\\fs24 " + inlines(cell.children()) + "\\cell";
    }

    @Override
    public String visitBlockQuote(BlockQuote quote) {
        quoteDepth++;
        try {
            return blocks(quote.blocks());
        } finally {
            quoteDepth--;
        }
    }

    @Override
    public String visitThematicBreak(ThematicBreak thematicBreak) {
        return "\\pard\\plain\\brdrb\\brdrs\\brdrw10\\brsp20" + meta("hr") + "\\par";
    }

    @Override
    public String visitText(Text text) {
        return escape(text.value());
    }

    @Override
    public String visitBold(Bold bold) {
        return "{\\b " + inlines(bold.children()) + "}";
    }

    @Override
    public String visitItalic(Italic italic) {
        return "{\\i " + inlines(italic.children()) + "}";
    }

    @Override
    public String visitStrikethrough(Strikethrough strikethrough) {
        return "{\\strike " + inlines(strikethrough.children()) + "}";
    }

    @Override
    public String visitInlineCode(InlineCode code) {
        return "{\\f1 " + escape(code.code()) + "}";
    }

    @Override
    public String visitLink(Link link) {
        return "{\\field{\\*\\fldinst HYPERLINK \"" + escape(link.url()) + "\"}{\\fldrslt{\\ul\\cf1 "
                + inlines(link.children()) + "}}}";
    }

    @Override
    public String visitImage(Image image) {
        return "{\\field{\\*\\fldinst INCLUDEPICTURE \"" + escape(image.url()) + "\"}{\\fldrslt " + escape(image.alt()) + "}}";
    }

    private static String paragraph(String indent, String meta, String content) {
        return "\\pard\\plain" + indent + "\\f0\\fs24" + (meta.isEmpty() ? " " : meta(meta)) + content + "\\par";
    }

    private static String meta(String value) {
        return "{\\*\\" + META + " " + value + "}";
    }

    private static String indent(int depth) {
        return depth == 0 ? "" : "\\li" + depth * INDENT;
    }

    /** Escapes RTF specials; non-ASCII becomes {@code \\uN?} (signed 16-bit, '?' as ANSI fallback). */
    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '{' -> escaped.append("\\{");
                case '}' -> escaped.append("\\}");
                case '\t' -> escaped.append("\\tab ");
                case '\n' -> escaped.append("\\line ");
                case '\r' -> { }
                default -> {
                    if (ch < 0x80) {
                        escaped.append(ch);
                    } else {
                        escaped.append("\\u").append((int) (short) ch).append('?');
                    }
                }
            }
        }
        return escaped.toString();
    }
}
