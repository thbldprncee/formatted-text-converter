package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.Strikethrough;
import io.github.thbldprncee.textformat.ast.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Inline syntax: emphasis ({@code *a*}, {@code _a_}), strong ({@code **a**}, {@code __a__}),
 * {@code ***both***}, strikethrough ({@code ~~a~~}), code spans of any backtick length,
 * links, images, autolinks ({@code <https://...>}) and backslash escapes.
 */
final class MarkdownInlineParser {

    private static final String ESCAPABLE = "\\`*_{}[]()#+-.!|~<>\"'";

    private final String text;
    private final List<Inline> result = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private int pos;

    private MarkdownInlineParser(String text) {
        this.text = text;
    }

    static List<Inline> parse(String text) {
        MarkdownInlineParser parser = new MarkdownInlineParser(text);
        parser.run();
        return List.copyOf(parser.result);
    }

    private void run() {
        while (pos < text.length()) {
            char ch = text.charAt(pos);
            boolean handled = switch (ch) {
                case '\\' -> escape();
                case '`' -> codeSpan();
                case '!' -> text.startsWith("![", pos) && image();
                case '[' -> link();
                case '<' -> autolink();
                case '~' -> text.startsWith("~~", pos) && delimited("~~", Strikethrough::new);
                case '*', '_' -> emphasis(ch);
                default -> false;
            };
            if (!handled) {
                buffer.append(ch);
                pos++;
            }
        }
        flush();
    }

    private boolean escape() {
        if (pos + 1 < text.length() && ESCAPABLE.indexOf(text.charAt(pos + 1)) >= 0) {
            buffer.append(text.charAt(pos + 1));
            pos += 2;
            return true;
        }
        return false;
    }

    private boolean codeSpan() {
        int ticks = runLength(pos, '`');
        int close = findBacktickRun(pos + ticks, ticks);
        if (close < 0) {
            buffer.append("`".repeat(ticks));
            pos += ticks;
            return true;
        }
        String code = text.substring(pos + ticks, close);
        if (code.length() >= 2 && code.startsWith(" ") && code.endsWith(" ") && !code.isBlank()) {
            code = code.substring(1, code.length() - 1);
        }
        emit(new InlineCode(code));
        pos = close + ticks;
        return true;
    }

    private boolean image() {
        Optional<Target> target = bracketTarget(pos + 1);
        if (target.isEmpty()) {
            return false;
        }
        emit(new Image(target.get().url(), plain(target.get().label())));
        pos = target.get().end();
        return true;
    }

    private boolean link() {
        Optional<Target> target = bracketTarget(pos);
        if (target.isEmpty()) {
            return false;
        }
        emit(new Link(target.get().url(), parse(target.get().label())));
        pos = target.get().end();
        return true;
    }

    private boolean autolink() {
        int close = text.indexOf('>', pos);
        if (close < 0) {
            return false;
        }
        String url = text.substring(pos + 1, close);
        if (!url.matches("(?i)(https?|ftp|mailto):\\S+")) {
            return false;
        }
        emit(Link.of(url, url));
        pos = close + 1;
        return true;
    }

    private boolean emphasis(char marker) {
        int run = runLength(pos, marker);
        if (run >= 3 && wrapped(String.valueOf(marker).repeat(3), inner -> List.of(new Bold(List.of(new Italic(inner)))))) {
            return true;
        }
        if (run >= 2 && delimited(String.valueOf(marker).repeat(2), Bold::new)) {
            return true;
        }
        return delimited(String.valueOf(marker), Italic::new);
    }

    private boolean delimited(String delimiter, Function<List<Inline>, Inline> factory) {
        return wrapped(delimiter, inner -> List.of(factory.apply(inner)));
    }

    /**
     * Opening delimiter must be followed by non-whitespace, closing one preceded by it;
     * underscores additionally must not be inside a word (snake_case stays text).
     */
    private boolean wrapped(String delimiter, java.util.function.UnaryOperator<List<Inline>> wrap) {
        int start = pos + delimiter.length();
        if (start >= text.length() || Character.isWhitespace(text.charAt(start))) {
            return false;
        }
        boolean underscore = delimiter.charAt(0) == '_';
        if (underscore && pos > 0 && Character.isLetterOrDigit(text.charAt(pos - 1))) {
            return false;
        }
        int close = findClosing(start, delimiter);
        if (close < 0) {
            return false;
        }
        List<Inline> inner = parse(text.substring(start, close));
        flush();
        result.addAll(wrap.apply(inner));
        pos = close + delimiter.length();
        return true;
    }

    private int findClosing(int from, String delimiter) {
        char marker = delimiter.charAt(0);
        int i = from;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i += 2;
                continue;
            }
            if (ch == '`') {
                int ticks = runLength(i, '`');
                int close = findBacktickRun(i + ticks, ticks);
                i = close < 0 ? i + ticks : close + ticks;
                continue;
            }
            if (ch == marker) {
                int run = runLength(i, marker);
                boolean canClose = i > from && !Character.isWhitespace(text.charAt(i - 1)) && run >= delimiter.length();
                boolean underscoreInsideWord = marker == '_'
                        && i + run < text.length() && Character.isLetterOrDigit(text.charAt(i + run));
                // A single marker must not close on a "**" run that belongs to strong emphasis inside.
                boolean partOfStrongRun = delimiter.length() == 1 && run == 2;
                if (canClose && !underscoreInsideWord && !partOfStrongRun) {
                    return i + run - delimiter.length();
                }
                i += run;
                continue;
            }
            i++;
        }
        return -1;
    }

    private record Target(String label, String url, int end) {
    }

    /** {@code [label](url "title")} starting at the opening bracket. */
    private Optional<Target> bracketTarget(int open) {
        int depth = 0;
        int close = -1;
        for (int i = open; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i++;
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']' && --depth == 0) {
                close = i;
                break;
            }
        }
        if (close < 0 || close + 1 >= text.length() || text.charAt(close + 1) != '(') {
            return Optional.empty();
        }
        int urlStart = close + 2;
        int end;
        String url;
        if (urlStart < text.length() && text.charAt(urlStart) == '<') {
            int angle = text.indexOf('>', urlStart);
            if (angle < 0) {
                return Optional.empty();
            }
            url = text.substring(urlStart + 1, angle);
            end = text.indexOf(')', angle);
        } else {
            int parens = 0;
            end = -1;
            for (int i = urlStart; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '(') {
                    parens++;
                } else if (ch == ')' && parens-- == 0) {
                    end = i;
                    break;
                }
            }
            if (end < 0) {
                return Optional.empty();
            }
            url = text.substring(urlStart, end).strip();
            int space = url.indexOf(' ');
            if (space >= 0) {
                url = url.substring(0, space); // drop an optional "title"
            }
        }
        if (end < 0) {
            return Optional.empty();
        }
        return Optional.of(new Target(text.substring(open + 1, close), url, end + 1));
    }

    private int runLength(int from, char ch) {
        int i = from;
        while (i < text.length() && text.charAt(i) == ch) {
            i++;
        }
        return i - from;
    }

    private int findBacktickRun(int from, int length) {
        int i = from;
        while (i < text.length()) {
            if (text.charAt(i) == '`') {
                int run = runLength(i, '`');
                if (run == length) {
                    return i;
                }
                i += run;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static String plain(String label) {
        return parse(label).stream()
                .map(io.github.thbldprncee.textformat.ast.Nodes::plainText)
                .reduce("", String::concat);
    }

    private void emit(Inline inline) {
        flush();
        result.add(inline);
    }

    private void flush() {
        if (!buffer.isEmpty()) {
            result.add(new Text(buffer.toString()));
            buffer.setLength(0);
        }
    }
}
