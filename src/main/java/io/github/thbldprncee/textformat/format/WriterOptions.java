package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.common.IndentStyle;

import java.util.Objects;

/**
 * Options shared by all writers.
 *
 * @param indent indentation of nested elements; used by the XML and JSON writers
 */
public record WriterOptions(IndentStyle indent) {

    public static final WriterOptions DEFAULT = new WriterOptions(IndentStyle.DEFAULT);

    public WriterOptions {
        Objects.requireNonNull(indent, "indent");
    }

    public WriterOptions withIndent(IndentStyle indent) {
        return new WriterOptions(indent);
    }
}
