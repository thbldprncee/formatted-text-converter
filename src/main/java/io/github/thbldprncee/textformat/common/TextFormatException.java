package io.github.thbldprncee.textformat.common;

import java.io.Serial;

/** Base of all errors raised while reading, writing or selecting a format. */
public class TextFormatException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TextFormatException(String message) {
        super(message);
    }

    public TextFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
