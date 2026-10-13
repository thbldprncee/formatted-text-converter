package io.github.thbldprncee.textformat.common;

import java.io.Serial;

import lombok.Getter;

/** Malformed input; carries the 1-based position where parsing failed. */
@Getter
public class ParseException extends TextFormatException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public ParseException(String message, int line, int column) {
        super("%s (line %d, column %d)".formatted(message, line, column));
        this.line = line;
        this.column = column;
    }
}
