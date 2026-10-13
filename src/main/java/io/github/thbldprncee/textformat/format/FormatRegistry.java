package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.common.UnsupportedFormatException;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/** All known formats; looked up by id, by output file extension or by sniffing content. */
public final class FormatRegistry {

    private final List<TextFormat> formats;

    public FormatRegistry(List<TextFormat> formats) {
        this.formats = List.copyOf(formats);
        long distinctIds = this.formats.stream().map(TextFormat::id).distinct().count();
        if (distinctIds != this.formats.size()) {
            throw new IllegalArgumentException("Duplicate format ids in " + ids());
        }
    }

    /** Every format registered in {@code META-INF/services} (built-in and plug-ins). */
    public static FormatRegistry loadInstalled() {
        return new FormatRegistry(ServiceLoader.load(TextFormat.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(TextFormat::id))
                .toList());
    }

    public List<TextFormat> formats() {
        return formats;
    }

    public Optional<TextFormat> byId(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        return formats.stream()
                .filter(format -> format.id().equals(key) || format.extensions().contains(key))
                .findFirst();
    }

    public TextFormat require(String id) {
        return byId(id).orElseThrow(() -> new UnsupportedFormatException(
                "Unknown format '%s'; known: %s".formatted(id, ids())));
    }

    /** The output format is defined by the file extension. */
    public TextFormat forOutput(Path path) {
        String extension = extension(path).orElseThrow(() -> new UnsupportedFormatException(
                "Output file '%s' has no extension; use one of: %s".formatted(path, writableExtensions())));
        return formats.stream()
                .filter(TextFormat::canWrite)
                .filter(format -> format.extensions().contains(extension))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFormatException(
                        "Unsupported output format '.%s'; use one of: %s".formatted(extension, writableExtensions())));
    }

    /** The input format is recognised by content: the readable format with the highest confidence wins. */
    public TextFormat detect(String content) {
        return formats.stream()
                .filter(TextFormat::canRead)
                .max(Comparator.comparingInt((TextFormat format) -> format.detect(content))
                        .thenComparing(TextFormat::id, Comparator.reverseOrder()))
                .orElseThrow(() -> new UnsupportedFormatException("No readable formats are installed"));
    }

    static Optional<String> extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
                ? Optional.empty()
                : Optional.of(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private String ids() {
        return formats.stream().map(TextFormat::id).collect(Collectors.joining(", "));
    }

    private String writableExtensions() {
        return formats.stream()
                .filter(TextFormat::canWrite)
                .flatMap(format -> format.extensions().stream())
                .map(ext -> "." + ext)
                .collect(Collectors.joining(", "));
    }
}
