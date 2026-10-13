package io.github.thbldprncee.textformat.api;

import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.transform.DocumentTransformer;
import io.github.thbldprncee.textformat.ast.transform.StyleRemover;
import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.UnsupportedFormatException;
import io.github.thbldprncee.textformat.format.FormatRegistry;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;
import lombok.Builder;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Facade over the whole library: detect the input format, parse, transform, write.
 * <pre>{@code
 * Converter.builder()
 *          .indent(IndentStyle.tabs())
 *          .stripStyles(true)
 *          .build()
 *          .convert(Path.of("in.md"), Path.of("out.json"));
 * }</pre>
 */
@Builder(toBuilder = true)
public final class Converter {

    @Builder.Default
    private final FormatRegistry registry = FormatRegistry.loadInstalled();

    /** Indentation of nested elements in XHTML and JSON output. */
    @Builder.Default
    private final IndentStyle indent = IndentStyle.DEFAULT;

    /** Remove bold, italic and strikethrough before writing. */
    private final boolean stripStyles;

    /** Extra transformation applied after parsing (and after style removal). */
    @Builder.Default
    private final DocumentTransformer transformer = DocumentTransformer.identity();

    /** Input format id; {@code null} means "recognise by content". */
    private final String sourceFormat;

    public static Converter defaults() {
        return builder().build();
    }

    public record Result(TextFormat source, TextFormat target, Document document) {
    }

    public FormatRegistry registry() {
        return registry;
    }

    public TextFormat sourceFormatOf(String content) {
        TextFormat format = sourceFormat == null ? registry.detect(content) : registry.require(sourceFormat);
        if (!format.canRead()) {
            throw new UnsupportedFormatException(format.displayName() + " cannot be used as input");
        }
        return format;
    }

    public Document read(String content) {
        Document document = sourceFormatOf(content).parser().parse(content);
        DocumentTransformer pipeline = stripStyles ? StyleRemover.instance().then(transformer) : transformer;
        return pipeline.apply(document);
    }

    public String write(Document document, TextFormat target) {
        return target.writer(new WriterOptions(indent)).render(document);
    }

    /** Converts text to the format with the given id or extension. */
    public String convert(String content, String targetFormat) {
        return write(read(content), registry.require(targetFormat));
    }

    /** Converts a file; the output format is the output file's extension. */
    public Result convert(Path input, Path output) throws IOException {
        TextFormat target = registry.forOutput(output);
        String content = Files.readString(input, StandardCharsets.UTF_8);
        TextFormat source = sourceFormatOf(content);
        Document document = read(content);
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            target.writer(new WriterOptions(indent)).write(document, writer);
        }
        return new Result(source, target, document);
    }
}
