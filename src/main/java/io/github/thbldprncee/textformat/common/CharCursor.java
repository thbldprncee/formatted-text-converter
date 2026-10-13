package io.github.thbldprncee.textformat.common;

import java.util.function.IntPredicate;

/**
 * Forward-only cursor over a string with line/column tracking.
 * Shared by the hand-written JSON, XML and RTF parsers.
 */
public final class CharCursor {

    public static final char EOF = '\0';

    private final String input;
    private int position;
    private int line = 1;
    private int column = 1;

    public CharCursor(String input) {
        this.input = input;
    }

    public boolean atEnd() {
        return position >= input.length();
    }

    public char peek() {
        return peek(0);
    }

    public char peek(int offset) {
        int index = position + offset;
        return index < input.length() ? input.charAt(index) : EOF;
    }

    public char next() {
        if (atEnd()) {
            throw error("Unexpected end of input");
        }
        char ch = input.charAt(position++);
        if (ch == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return ch;
    }

    public boolean startsWith(String prefix) {
        return input.startsWith(prefix, position);
    }

    /** Consumes {@code expected} if it is next; returns whether it did. */
    public boolean consume(char expected) {
        if (peek() == expected && !atEnd()) {
            next();
            return true;
        }
        return false;
    }

    public boolean consume(String expected) {
        if (!startsWith(expected)) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            next();
        }
        return true;
    }

    public void expect(char expected) {
        if (!consume(expected)) {
            throw error("Expected '%s' but found %s".formatted(expected, describe(peek())));
        }
    }

    public void expect(String expected) {
        if (!consume(expected)) {
            throw error("Expected \"%s\"".formatted(expected));
        }
    }

    public String readWhile(IntPredicate predicate) {
        int start = position;
        while (!atEnd() && predicate.test(peek())) {
            next();
        }
        return input.substring(start, position);
    }

    /** Reads up to (not including) {@code delimiter}, or to the end of input. */
    public String readUntil(String delimiter) {
        int end = input.indexOf(delimiter, position);
        int stop = end < 0 ? input.length() : end;
        StringBuilder text = new StringBuilder(stop - position);
        while (position < stop) {
            text.append(next());
        }
        return text.toString();
    }

    public void skipWhitespace() {
        readWhile(Character::isWhitespace);
    }

    public ParseException error(String message) {
        return new ParseException(message, line, column);
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    private String describe(char ch) {
        return atEnd() ? "end of input" : "'" + ch + "'";
    }
}
