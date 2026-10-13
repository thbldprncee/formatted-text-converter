package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.common.UnsupportedFormatException;

import java.util.List;
import java.util.Set;

/**
 * A supported file format (Strategy + Abstract Factory): knows its file extensions, how to
 * recognise its content and how to create a matching parser and writer.
 * <p>
 * Implementations are discovered with {@link java.util.ServiceLoader}, so adding a format
 * means adding a class and one line in {@code META-INF/services} - nothing else changes.
 */
public interface TextFormat {

    /** Short identifier used on the command line, e.g. {@code md}. */
    String id();

    String displayName();

    /** Lower-case file extensions without the dot; the first one is the preferred one. */
    List<String> extensions();

    Set<Capability> capabilities();

    default boolean canRead() {
        return capabilities().contains(Capability.READ);
    }

    default boolean canWrite() {
        return capabilities().contains(Capability.WRITE);
    }

    default DocumentParser parser() {
        throw new UnsupportedFormatException(displayName() + " cannot be read");
    }

    default DocumentWriter writer(WriterOptions options) {
        throw new UnsupportedFormatException(displayName() + " cannot be written");
    }

    /**
     * How confident this format is that {@code content} is written in it, 0 (no) to 100 (certain).
     * The input file's extension is deliberately not used: it does not identify the format.
     */
    default int detect(String content) {
        return 0;
    }
}
