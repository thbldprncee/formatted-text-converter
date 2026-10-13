package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.common.IndentStyle;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Base class of writers of nested, indentable formats (XHTML, JSON): they share the
 * configurable {@link IndentStyle} for nested elements.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractStructuredWriter implements DocumentWriter {

    private final WriterOptions options;

    protected IndentStyle indent() {
        return options.indent();
    }
}
