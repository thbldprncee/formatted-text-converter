package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.ast.Block;
import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.format.DocumentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Block structure of CommonMark-style Markdown: ATX and setext headings, paragraphs, fenced
 * code, block quotes, thematic breaks, nested bullet/numbered lists and GFM tables.
 * Container blocks (quotes, list items) are parsed recursively on their de-indented lines.
 */
public final class MarkdownParser implements DocumentParser {

    private static final Pattern ATX_HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?(?:[ \\t]+#+)?[ \\t]*$");
    private static final Pattern FENCE = Pattern.compile("^( {0,3})(`{3,}|~{3,})[ \\t]*([^`]*?)[ \\t]*$");
    private static final Pattern THEMATIC_BREAK = Pattern.compile("^ {0,3}([-*_])(?:[ \\t]*\\1){2,}[ \\t]*$");
    private static final Pattern BLOCK_QUOTE = Pattern.compile("^ {0,3}> ?(.*)$");
    private static final Pattern LIST_ITEM = Pattern.compile("^( {0,3})([-+*]|\\d{1,9}[.)])([ \\t]+|$)(.*)$");
    private static final Pattern SETEXT_UNDERLINE = Pattern.compile("^ {0,3}(=+|-+)[ \\t]*$");
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");

    @Override
    public Document parse(String content) {
        List<String> lines = content.replace("\r\n", "\n").replace('\r', '\n').lines()
                .map(MarkdownParser::expandTabs)
                .toList();
        return new Document(blocks(lines));
    }

    private List<Block> blocks(List<String> lines) {
        List<Block> blocks = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.isBlank()) {
                i++;
                continue;
            }
            Matcher match;
            if ((match = FENCE.matcher(line)).matches()) {
                i = fencedCode(lines, i, match, blocks);
            } else if ((match = ATX_HEADING.matcher(line)).matches()) {
                String text = match.group(2) == null ? "" : match.group(2);
                blocks.add(new Heading(match.group(1).length(), MarkdownInlineParser.parse(text)));
                i++;
            } else if (THEMATIC_BREAK.matcher(line).matches()) {
                blocks.add(new ThematicBreak());
                i++;
            } else if (BLOCK_QUOTE.matcher(line).matches()) {
                i = blockQuote(lines, i, blocks);
            } else if (LIST_ITEM.matcher(line).matches()) {
                i = list(lines, i, blocks);
            } else if (isTableStart(lines, i)) {
                i = table(lines, i, blocks);
            } else {
                i = paragraph(lines, i, blocks);
            }
        }
        return blocks;
    }

    private int fencedCode(List<String> lines, int start, Matcher opening, List<Block> blocks) {
        int indent = opening.group(1).length();
        String fence = opening.group(2);
        String language = opening.group(3).split("\\s+")[0];
        List<String> code = new ArrayList<>();
        int i = start + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            String stripped = line.strip();
            if (stripped.length() >= fence.length() && stripped.chars().allMatch(c -> c == fence.charAt(0))
                    && leadingSpaces(line) <= 3) {
                i++;
                break;
            }
            code.add(line.substring(Math.min(indent, leadingSpaces(line))));
            i++;
        }
        blocks.add(new CodeBlock(String.join("\n", code), language));
        return i;
    }

    private int blockQuote(List<String> lines, int start, List<Block> blocks) {
        List<String> inner = new ArrayList<>();
        int i = start;
        while (i < lines.size()) {
            Matcher match = BLOCK_QUOTE.matcher(lines.get(i));
            if (match.matches()) {
                inner.add(match.group(1));
            } else if (!lines.get(i).isBlank() && !inner.isEmpty() && !inner.getLast().isBlank()
                    && !startsBlock(lines.get(i))) {
                inner.add(lines.get(i)); // lazy continuation of a quoted paragraph
            } else {
                break;
            }
            i++;
        }
        blocks.add(new BlockQuote(blocks(inner)));
        return i;
    }

    /** Collects consecutive items of the same kind; each item's lines are parsed recursively. */
    private int list(List<String> lines, int start, List<Block> blocks) {
        Matcher first = LIST_ITEM.matcher(lines.get(start));
        first.matches();
        boolean ordered = Character.isDigit(first.group(2).charAt(0));
        char bulletOrDelimiter = first.group(2).charAt(first.group(2).length() - 1);
        int markerIndent = first.group(1).length();

        List<ListItem> items = new ArrayList<>();
        int i = start;
        while (i < lines.size()) {
            Matcher item = LIST_ITEM.matcher(lines.get(i));
            if (!item.matches() || item.group(1).length() != markerIndent
                    || Character.isDigit(item.group(2).charAt(0)) != ordered
                    || item.group(2).charAt(item.group(2).length() - 1) != bulletOrDelimiter) {
                break;
            }
            int contentIndent = contentIndent(item);
            List<String> itemLines = new ArrayList<>();
            itemLines.add(item.group(4));
            i++;
            while (i < lines.size()) {
                String line = lines.get(i);
                if (line.isBlank()) {
                    // A blank line continues the item only if indented content follows.
                    int next = nextNonBlank(lines, i);
                    if (next < lines.size() && leadingSpaces(lines.get(next)) >= contentIndent) {
                        itemLines.add("");
                        i++;
                        continue;
                    }
                    break;
                }
                if (leadingSpaces(line) >= contentIndent) {
                    itemLines.add(line.substring(contentIndent));
                } else if (!startsBlock(line) && !itemLines.getLast().isBlank()) {
                    itemLines.add(line.strip()); // lazy continuation
                } else {
                    break;
                }
                i++;
            }
            items.add(new ListItem(blocks(itemLines)));
            int next = nextNonBlank(lines, i);
            if (next < lines.size() && next > i && !LIST_ITEM.matcher(lines.get(next)).matches()) {
                break;
            }
            i = next;
        }
        blocks.add(new ListBlock(ordered, items));
        return i;
    }

    private static int contentIndent(Matcher item) {
        int spacing = item.group(3).length();
        if (spacing == 0 || spacing > 4) {
            spacing = 1; // indented code inside items is not supported; treat as one space
        }
        return item.group(1).length() + item.group(2).length() + spacing;
    }

    private boolean isTableStart(List<String> lines, int i) {
        return lines.get(i).contains("|") && i + 1 < lines.size()
                && TABLE_SEPARATOR.matcher(lines.get(i + 1)).matches() && lines.get(i + 1).contains("-");
    }

    private int table(List<String> lines, int start, List<Block> blocks) {
        List<TableRow> rows = new ArrayList<>();
        rows.add(row(lines.get(start)));
        int i = start + 2;
        while (i < lines.size() && !lines.get(i).isBlank() && lines.get(i).contains("|")) {
            rows.add(row(lines.get(i)));
            i++;
        }
        blocks.add(new Table(rows));
        return i;
    }

    private TableRow row(String line) {
        return new TableRow(splitCells(line).stream()
                .map(cell -> new TableCell(MarkdownInlineParser.parse(cell.strip())))
                .toList());
    }

    /** Splits on unescaped pipes outside code spans; outer pipes are optional. */
    static List<String> splitCells(String line) {
        String row = line.strip();
        if (row.startsWith("|")) {
            row = row.substring(1);
        }
        if (row.endsWith("|") && !row.endsWith("\\|")) {
            row = row.substring(0, row.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inCode = false;
        for (int i = 0; i < row.length(); i++) {
            char ch = row.charAt(i);
            if (ch == '\\' && i + 1 < row.length() && row.charAt(i + 1) == '|') {
                cell.append('|');
                i++;
            } else if (ch == '`') {
                inCode = !inCode;
                cell.append(ch);
            } else if (ch == '|' && !inCode) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private int paragraph(List<String> lines, int start, List<Block> blocks) {
        List<String> text = new ArrayList<>();
        text.add(lines.get(start).strip());
        int i = start + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            Matcher underline = SETEXT_UNDERLINE.matcher(line);
            if (underline.matches()) {
                int level = underline.group(1).charAt(0) == '=' ? 1 : 2;
                blocks.add(new Heading(level, MarkdownInlineParser.parse(String.join(" ", text))));
                return i + 1;
            }
            if (line.isBlank() || startsBlock(line) || isTableStart(lines, i)) {
                break;
            }
            text.add(line.strip());
            i++;
        }
        blocks.add(new Paragraph(MarkdownInlineParser.parse(String.join(" ", text))));
        return i;
    }

    /** Lines that interrupt a paragraph. */
    private static boolean startsBlock(String line) {
        return ATX_HEADING.matcher(line).matches()
                || FENCE.matcher(line).matches()
                || THEMATIC_BREAK.matcher(line).matches()
                || BLOCK_QUOTE.matcher(line).matches()
                || Optional.of(LIST_ITEM.matcher(line)).filter(Matcher::matches)
                        .map(m -> !m.group(4).isBlank()).orElse(false);
    }

    private static int nextNonBlank(List<String> lines, int from) {
        int i = from;
        while (i < lines.size() && lines.get(i).isBlank()) {
            i++;
        }
        return i;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /** Leading tabs count as four spaces, as in CommonMark. */
    private static String expandTabs(String line) {
        int i = 0;
        StringBuilder prefix = new StringBuilder();
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            prefix.append(line.charAt(i) == '\t' ? " ".repeat(4 - prefix.length() % 4) : " ");
            i++;
        }
        return prefix + line.substring(i);
    }
}
