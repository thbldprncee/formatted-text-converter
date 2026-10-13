package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.ast.Document;

/** Reads one format into the common model. Implementations are stateless and thread-safe. */
@FunctionalInterface
public interface DocumentParser {

    Document parse(String content);
}
