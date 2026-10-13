package io.github.thbldprncee.textformat.formats.rtf;

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
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.common.ParseException;
import io.github.thbldprncee.textformat.format.DocumentParser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads RTF in two phases: an interpreter turns the token stream into paragraphs and tables
 * (tracking character formatting per group), then {@link #assemble} rebuilds headings, lists,
 * quotes and code blocks. Documents written by {@link RtfWriter} carry {@code \\*\\tfmeta}
 * hints and round-trip exactly; for other RTF the standard control words are interpreted
 * ({@code \\outlinelevel}, {@code \\intbl}, bullets, monospace fonts, HYPERLINK fields).
 */
public final class RtfParser implements DocumentParser {

    private static final Set<String> SKIPPED_DESTINATIONS = Set.of("colortbl", "stylesheet", "info", "pict",
            "header", "headerl", "headerr", "footer", "footerl", "footerr", "footnote", "listtable",
            "listoverridetable", "generator", "themedata", "colorschememapping", "latentstyles", "datastore",
            "xmlnstbl", "rsidtbl", "mmathPr", "object", "nonshppict", "shp", "revtbl");
    private static final Map<String, String> SYMBOLS = Map.of(
            "bullet", "•", "emdash", "—", "endash", "–", "lquote", "‘", "rquote", "’",
            "ldblquote", "“", "rdblquote", "”", "tab", "\t", "line", "\n");
    private static final Pattern FIELD = Pattern.compile("^\\s*(HYPERLINK|INCLUDEPICTURE)\\s+\"([^\"]*)\"");
    private static final Pattern FONT_ENTRY = Pattern.compile("\\\\f(\\d+)([^;{}]*);");
    private static final Pattern FOREIGN_LIST_MARKER = Pattern.compile("^(•|\\d+[.)])[\\t ]+");

    @Override
    public Document parse(String content) {
        Interpreter interpreter = new Interpreter(content);
        interpreter.run();
        return new Document(assemble(interpreter.segments));
    }

    // ------------------------------------------------------------------ phase 1: interpretation

    /** A finished paragraph or table, in document order. */
    private sealed interface Segment permits ParagraphSegment, TableSegment {
    }

    private record ParagraphSegment(List<Inline> content, int outlineLevel, String meta) implements Segment {
    }

    private record TableSegment(List<TableRow> rows) implements Segment {
    }

    /** Character formatting; copied on '{' and restored on '}'. */
    private record CharState(boolean bold, boolean italic, boolean strike, boolean mono) {

        static final CharState PLAIN = new CharState(false, false, false, false);
    }

    private static final class FieldContext {
        final int depth;
        final int start;
        String instruction = "";

        FieldContext(int depth, int start) {
            this.depth = depth;
            this.start = start;
        }
    }

    private static final class Interpreter {

        private final String input;
        private final List<Segment> segments = new ArrayList<>();
        private final Deque<CharState> stack = new ArrayDeque<>();
        private final Deque<FieldContext> fields = new ArrayDeque<>();
        private final Set<Integer> monospaceFonts = new HashSet<>(Set.of(1));
        private final List<Inline> runs = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private final List<TableCell> rowCells = new ArrayList<>();
        private final List<TableRow> tableRows = new ArrayList<>();

        private CharState state = CharState.PLAIN;
        private CharState textState = CharState.PLAIN;
        private Charset charset = Charset.forName("windows-1252");
        private int pos;
        private int unicodeSkip = 1;
        private int pendingSkip;
        private int outlineLevel = -1;
        private String meta = "";
        private boolean inTable;
        private int line = 1;

        Interpreter(String input) {
            this.input = input;
        }

        void run() {
            if (!input.stripLeading().startsWith("{\\rtf")) {
                throw new ParseException("Not an RTF document: expected '{\\rtf'", 1, 1);
            }
            while (pos < input.length()) {
                char ch = input.charAt(pos++);
                switch (ch) {
                    case '{' -> openGroup();
                    case '}' -> closeGroup();
                    case '\\' -> control();
                    case '\r' -> { }
                    case '\n' -> line++;
                    default -> character(String.valueOf(ch));
                }
            }
            endParagraph();
            closeTable();
        }

        private void openGroup() {
            // Destinations whose content is not document text.
            if (input.startsWith("\\*\\", pos)) {
                String word = peekWord(pos + 3);
                String body = skipGroup();
                if (word.equals(RtfWriter.META)) {
                    meta = body.substring(body.indexOf(RtfWriter.META) + RtfWriter.META.length()).strip();
                } else if (word.equals("fldinst") && !fields.isEmpty()) {
                    fields.peek().instruction = decodePlain(body.substring(body.indexOf("fldinst") + 7));
                }
                return;
            }
            if (input.startsWith("\\", pos)) {
                String word = peekWord(pos + 1);
                if (word.equals("fonttbl")) {
                    readFontTable(skipGroup());
                    return;
                }
                if (SKIPPED_DESTINATIONS.contains(word)) {
                    skipGroup();
                    return;
                }
                if (word.equals("fldinst")) {
                    String body = skipGroup();
                    if (!fields.isEmpty()) {
                        fields.peek().instruction = decodePlain(body.substring(body.indexOf("fldinst") + 7));
                    }
                    return;
                }
                if (word.equals("field")) {
                    flushText();
                    fields.push(new FieldContext(stack.size() + 1, runs.size()));
                }
            }
            stack.push(state);
        }

        private void closeGroup() {
            if (stack.isEmpty()) {
                return; // tolerate a stray closing brace
            }
            if (!fields.isEmpty() && fields.peek().depth == stack.size()) {
                flushText();
                finishField(fields.pop());
            }
            state = stack.pop();
        }

        private void finishField(FieldContext field) {
            List<Inline> result = new ArrayList<>(runs.subList(field.start, runs.size()));
            runs.subList(field.start, runs.size()).clear();
            Matcher match = FIELD.matcher(field.instruction);
            if (!match.find()) {
                runs.addAll(result);
            } else if (match.group(1).equals("HYPERLINK")) {
                runs.add(new Link(match.group(2), result));
            } else {
                runs.add(new Image(match.group(2), result.stream().map(Nodes::plainText).reduce("", String::concat)));
            }
        }

        private void control() {
            if (pos >= input.length()) {
                return;
            }
            char next = input.charAt(pos);
            if (!Character.isLetter(next)) {
                pos++;
                switch (next) {
                    case '\\', '{', '}' -> character(String.valueOf(next));
                    case '~' -> character(" ");
                    case '_' -> character("-");
                    case '\'' -> hexCharacter();
                    case '\n', '\r' -> endParagraph();
                    default -> { } // \- optional hyphen, \* handled at group start, others ignored
                }
                return;
            }
            String word = readWord();
            Integer parameter = readParameter();
            if (pos < input.length() && input.charAt(pos) == ' ') {
                pos++; // the delimiting space belongs to the control word
            }
            controlWord(word, parameter);
        }

        private void controlWord(String word, Integer parameter) {
            boolean on = parameter == null || parameter != 0;
            switch (word) {
                case "par" -> {
                    if (inTable) {
                        character("\n");
                    } else {
                        endParagraph();
                    }
                }
                case "pard" -> {
                    outlineLevel = -1;
                    meta = "";
                    inTable = false;
                }
                case "plain" -> setState(CharState.PLAIN);
                case "b" -> setState(new CharState(on, state.italic(), state.strike(), state.mono()));
                case "i" -> setState(new CharState(state.bold(), on, state.strike(), state.mono()));
                case "strike", "striked" -> setState(new CharState(state.bold(), state.italic(), on, state.mono()));
                case "f" -> setState(new CharState(state.bold(), state.italic(), state.strike(),
                        parameter != null && monospaceFonts.contains(parameter)));
                case "outlinelevel" -> outlineLevel = parameter == null ? 0 : parameter;
                case "intbl" -> inTable = true;
                case "cell" -> {
                    flushText();
                    rowCells.add(new TableCell(trim(List.copyOf(runs))));
                    runs.clear();
                }
                case "row" -> {
                    tableRows.add(new TableRow(List.copyOf(rowCells)));
                    rowCells.clear();
                }
                case "ansicpg" -> charset = codePage(parameter);
                case "uc" -> unicodeSkip = parameter == null ? 1 : parameter;
                case "u" -> {
                    if (parameter != null) {
                        character(String.valueOf((char) (parameter < 0 ? parameter + 65536 : parameter)));
                        pendingSkip = unicodeSkip;
                    }
                }
                default -> {
                    String symbol = SYMBOLS.get(word);
                    if (symbol != null) {
                        character(symbol);
                    }
                }
            }
        }

        private void setState(CharState newState) {
            state = newState;
        }

        private void hexCharacter() {
            if (pos + 2 > input.length()) {
                return;
            }
            int value = Integer.parseInt(input.substring(pos, pos + 2), 16);
            pos += 2;
            character(new String(new byte[]{(byte) value}, charset));
        }

        private void character(String value) {
            if (pendingSkip > 0) {
                pendingSkip--; // ANSI fallback of a preceding \\uN
                return;
            }
            if (!state.equals(textState)) {
                flushText();
                textState = state;
            }
            text.append(value);
        }

        private void flushText() {
            if (text.isEmpty()) {
                return;
            }
            String value = text.toString();
            text.setLength(0);
            Inline inline = textState.mono() ? new InlineCode(value) : new Text(value);
            if (!textState.mono()) {
                if (textState.strike()) {
                    inline = new Strikethrough(List.of(inline));
                }
                if (textState.italic()) {
                    inline = new Italic(List.of(inline));
                }
                if (textState.bold()) {
                    inline = new Bold(List.of(inline));
                }
            }
            runs.add(inline);
        }

        private void endParagraph() {
            flushText();
            if (!inTable) {
                closeTable();
            }
            List<Inline> content = merge(runs);
            runs.clear();
            if (!content.isEmpty() || !meta.isEmpty()) {
                segments.add(new ParagraphSegment(content, outlineLevel, meta));
            }
        }

        private void closeTable() {
            if (!tableRows.isEmpty()) {
                segments.add(new TableSegment(List.copyOf(tableRows)));
                tableRows.clear();
            }
        }

        /** Returns the raw text of the group starting at the current position and moves past it. */
        private String skipGroup() {
            int depth = 1;
            int start = pos;
            while (pos < input.length() && depth > 0) {
                char ch = input.charAt(pos++);
                if (ch == '\\') {
                    pos++;
                } else if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                }
            }
            if (depth > 0) {
                throw new ParseException("Unclosed RTF group", line, 1);
            }
            return input.substring(start, pos - 1);
        }

        private void readFontTable(String table) {
            Matcher entry = FONT_ENTRY.matcher(table);
            monospaceFonts.clear();
            while (entry.find()) {
                String description = entry.group(2).toLowerCase();
                if (description.contains("\\fmodern") || description.matches(".*(courier|mono|consolas|menlo).*")) {
                    monospaceFonts.add(Integer.parseInt(entry.group(1)));
                }
            }
        }

        /** Text of a destination such as fldinst: escapes decoded, control words dropped. */
        private String decodePlain(String raw) {
            StringBuilder plain = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                if (ch == '\\' && i + 1 < raw.length()) {
                    char next = raw.charAt(i + 1);
                    if (next == '\\' || next == '{' || next == '}') {
                        plain.append(next);
                        i++;
                    } else if (next == 'u' && i + 2 < raw.length()
                            && (Character.isDigit(raw.charAt(i + 2)) || raw.charAt(i + 2) == '-')) {
                        int end = i + 2;
                        while (end < raw.length() && (Character.isDigit(raw.charAt(end)) || raw.charAt(end) == '-')) {
                            end++;
                        }
                        int code = Integer.parseInt(raw.substring(i + 2, end));
                        plain.append((char) (code < 0 ? code + 65536 : code));
                        i = end < raw.length() && raw.charAt(end) == '?' ? end : end - 1;
                    } else {
                        while (i + 1 < raw.length() && Character.isLetterOrDigit(raw.charAt(i + 1))) {
                            i++;
                        }
                    }
                } else if (ch != '{' && ch != '}') {
                    plain.append(ch);
                }
            }
            return plain.toString();
        }

        private String readWord() {
            int start = pos;
            while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
                pos++;
            }
            return input.substring(start, pos);
        }

        private String peekWord(int from) {
            int end = from;
            while (end < input.length() && Character.isLetter(input.charAt(end))) {
                end++;
            }
            return input.substring(from, end);
        }

        private Integer readParameter() {
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            String digits = input.substring(start, pos);
            return digits.isEmpty() || digits.equals("-") ? null : Integer.valueOf(digits);
        }

        private static Charset codePage(Integer page) {
            try {
                return page == null ? StandardCharsets.ISO_8859_1 : Charset.forName("windows-" + page);
            } catch (IllegalArgumentException e) {
                return Charset.forName("windows-1252");
            }
        }
    }

    // ------------------------------------------------------------------ phase 2: structure

    private List<Block> assemble(List<Segment> segments) {
        List<Block> blocks = new ArrayList<>();
        Position at = new Position(segments);
        while (at.hasMore()) {
            blocks.add(block(at));
        }
        return blocks;
    }

    private static final class Position {
        final List<Segment> segments;
        int index;

        Position(List<Segment> segments) {
            this.segments = segments;
        }

        boolean hasMore() {
            return index < segments.size();
        }

        Segment peek() {
            return segments.get(index);
        }

        Segment next() {
            return segments.get(index++);
        }
    }

    private Block block(Position at) {
        Segment segment = at.peek();
        if (segment instanceof TableSegment table) {
            at.next();
            return new Table(table.rows());
        }
        ParagraphSegment paragraph = (ParagraphSegment) segment;
        String[] meta = paragraph.meta().split("\\s+");
        switch (meta[0]) {
            case "code" -> {
                at.next();
                return new CodeBlock(plain(paragraph.content()), meta.length > 1 ? meta[1] : "");
            }
            case "hr" -> {
                at.next();
                return new ThematicBreak();
            }
            case "li" -> {
                return list(at, Integer.parseInt(meta[1]), meta[2].equals("1"));
            }
            case "q" -> {
                return quote(at, Integer.parseInt(meta[1]));
            }
            default -> {
            }
        }
        if (paragraph.outlineLevel() >= 0) {
            at.next();
            return new Heading(Heading.clampLevel(paragraph.outlineLevel() + 1), unbold(trim(paragraph.content())));
        }
        String text = plain(paragraph.content());
        if (paragraph.meta().isEmpty() && FOREIGN_LIST_MARKER.matcher(text).find()) {
            return foreignList(at);
        }
        at.next();
        return new Paragraph(trim(paragraph.content()));
    }

    private ListBlock list(Position at, int depth, boolean ordered) {
        List<ListItem> items = new ArrayList<>();
        while (at.hasMore() && at.peek() instanceof ParagraphSegment start
                && start.meta().equals("li %d %d".formatted(depth, ordered ? 1 : 0))) {
            at.next();
            List<Block> blocks = new ArrayList<>();
            List<Inline> first = stripMarker(start.content());
            // An item that starts with a nested list has no text of its own.
            boolean startsWithNestedList = first.isEmpty() && at.hasMore()
                    && at.peek() instanceof ParagraphSegment next && next.meta().startsWith("li ")
                    && Integer.parseInt(next.meta().split("\\s+")[1]) > depth;
            if (!startsWithNestedList) {
                blocks.add(new Paragraph(first));
            }
            while (at.hasMore() && at.peek() instanceof ParagraphSegment next) {
                String[] meta = next.meta().split("\\s+");
                if (meta[0].equals("lic") && Integer.parseInt(meta[1]) == depth) {
                    at.next();
                    blocks.add(new Paragraph(trim(next.content())));
                } else if (meta[0].equals("li") && Integer.parseInt(meta[1]) > depth) {
                    blocks.add(list(at, Integer.parseInt(meta[1]), meta[2].equals("1")));
                } else {
                    break;
                }
            }
            items.add(new ListItem(blocks));
        }
        return new ListBlock(ordered, items);
    }

    private BlockQuote quote(Position at, int depth) {
        List<Block> blocks = new ArrayList<>();
        while (at.hasMore() && at.peek() instanceof ParagraphSegment next && next.meta().startsWith("q ")) {
            int level = Integer.parseInt(next.meta().substring(2).strip());
            if (level < depth) {
                break;
            }
            if (level > depth) {
                blocks.add(quote(at, depth + 1));
            } else {
                at.next();
                blocks.add(new Paragraph(trim(next.content())));
            }
        }
        return new BlockQuote(blocks);
    }

    /** Bullet or numbered paragraphs from other RTF producers (no meta hints). */
    private ListBlock foreignList(Position at) {
        List<ListItem> items = new ArrayList<>();
        boolean ordered = Character.isDigit(plain(((ParagraphSegment) at.peek()).content()).strip().charAt(0));
        while (at.hasMore() && at.peek() instanceof ParagraphSegment next && next.meta().isEmpty()
                && next.outlineLevel() < 0 && FOREIGN_LIST_MARKER.matcher(plain(next.content())).find()) {
            at.next();
            items.add(new ListItem(List.of(new Paragraph(stripMarker(next.content())))));
        }
        return new ListBlock(ordered, items);
    }

    /** Removes the leading "•\t" / "12.\t" written before an item's text. */
    private static List<Inline> stripMarker(List<Inline> content) {
        List<Inline> trimmed = trim(content);
        if (!trimmed.isEmpty() && trimmed.getFirst() instanceof Text first) {
            Matcher marker = FOREIGN_LIST_MARKER.matcher(first.value());
            if (marker.find()) {
                List<Inline> rest = new ArrayList<>(trimmed);
                String remaining = first.value().substring(marker.end());
                if (remaining.isEmpty()) {
                    rest.removeFirst();
                } else {
                    rest.set(0, new Text(remaining));
                }
                return List.copyOf(rest);
            }
        }
        return trimmed;
    }

    /** Headings are written in bold; that bold is presentation of the heading, not content. */
    private static List<Inline> unbold(List<Inline> content) {
        return merge(content.stream()
                .flatMap(inline -> inline instanceof Bold bold ? bold.children().stream() : java.util.stream.Stream.of(inline))
                .toList());
    }

    private static List<Inline> merge(List<Inline> runs) {
        List<Inline> merged = new ArrayList<>();
        for (Inline run : runs) {
            if (!merged.isEmpty() && merged.getLast() instanceof Text previous && run instanceof Text text) {
                merged.set(merged.size() - 1, new Text(previous.value() + text.value()));
            } else {
                merged.add(run);
            }
        }
        return merged;
    }

    /** Strips whitespace at the paragraph edges (line breaks in the RTF source are not content). */
    private static List<Inline> trim(List<Inline> content) {
        List<Inline> result = new ArrayList<>(content);
        if (!result.isEmpty() && result.getFirst() instanceof Text first) {
            result.set(0, new Text(first.value().stripLeading()));
        }
        if (!result.isEmpty() && result.getLast() instanceof Text last) {
            result.set(result.size() - 1, new Text(last.value().stripTrailing()));
        }
        return result.stream().filter(inline -> !(inline instanceof Text text && text.value().isEmpty())).toList();
    }

    private static String plain(List<Inline> content) {
        return content.stream().map(Nodes::plainText).reduce("", String::concat);
    }
}
