package io.github.thbldprncee.textformat.common;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Decorator over an {@link Appendable} that tracks a nesting level and prefixes new lines
 * with the configured {@link IndentStyle}. I/O errors are rethrown unchecked so that it can
 * be used from visitor methods.
 */
@RequiredArgsConstructor
public final class IndentedOutput {

    private final Appendable target;
    private final IndentStyle style;
    private int level;
    private boolean lineStart = true;

    public IndentedOutput write(String text) {
        if (text.isEmpty()) {
            return this;
        }
        if (lineStart) {
            append(style.at(level));
            lineStart = false;
        }
        append(text);
        return this;
    }

    public IndentedOutput newline() {
        append("\n");
        lineStart = true;
        return this;
    }

    public IndentedOutput line(String text) {
        return write(text).newline();
    }

    public IndentedOutput indent() {
        level++;
        return this;
    }

    public IndentedOutput dedent() {
        if (level > 0) {
            level--;
        }
        return this;
    }

    private void append(String text) {
        try {
            target.append(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
