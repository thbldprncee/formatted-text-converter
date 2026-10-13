package io.github.thbldprncee.textformat.common;

import java.io.Serial;

public class UnsupportedFormatException extends TextFormatException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnsupportedFormatException(String message) {
        super(message);
    }
}
