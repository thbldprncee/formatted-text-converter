package io.github.thbldprncee.textformat.ast.transform;

import io.github.thbldprncee.textformat.ast.Document;

import java.util.Arrays;
import java.util.function.UnaryOperator;

/** A pure function over documents. Transformers compose into pipelines with {@link #then}. */
@FunctionalInterface
public interface DocumentTransformer extends UnaryOperator<Document> {

    static DocumentTransformer identity() {
        return document -> document;
    }

    static DocumentTransformer chain(DocumentTransformer... steps) {
        return Arrays.stream(steps).reduce(identity(), DocumentTransformer::then);
    }

    default DocumentTransformer then(DocumentTransformer next) {
        return document -> next.apply(apply(document));
    }
}
