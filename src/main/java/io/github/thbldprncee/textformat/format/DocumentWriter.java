package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.ast.Document;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Renders the common model in one format. */
@FunctionalInterface
public interface DocumentWriter {

    void write(Document document, Appendable out) throws IOException;

    default String render(Document document) {
        StringBuilder out = new StringBuilder();
        try {
            write(document, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e); // StringBuilder never throws
        }
        return out.toString();
    }
}
