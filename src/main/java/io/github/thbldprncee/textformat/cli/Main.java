package io.github.thbldprncee.textformat.cli;

import io.github.thbldprncee.textformat.api.Converter;
import io.github.thbldprncee.textformat.ast.stats.DocumentStatistics;
import io.github.thbldprncee.textformat.common.TextFormatException;
import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.FormatRegistry;
import io.github.thbldprncee.textformat.format.TextFormat;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.stream.Collectors;

/** Command line entry point: {@code textconv in.md out.json}. */
public final class Main {

    static final int OK = 0;
    static final int USAGE_ERROR = 1;
    static final int CONVERSION_ERROR = 2;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.print(CliOptions.USAGE);
            return USAGE_ERROR;
        }
        return switch (options.command()) {
            case HELP -> {
                out.print(CliOptions.USAGE);
                yield OK;
            }
            case LIST_FORMATS -> {
                out.print(formatTable(FormatRegistry.loadInstalled()));
                yield OK;
            }
            case CONVERT -> convert(options, out, err);
        };
    }

    private static int convert(CliOptions options, PrintStream out, PrintStream err) {
        Converter converter = Converter.builder()
                .indent(options.indent())
                .stripStyles(options.stripStyles())
                .sourceFormat(options.from().orElse(null))
                .build();
        try {
            Converter.Result result = converter.convert(options.input(), options.output());
            out.printf("%s (%s) -> %s (%s)%n", options.input(), result.source().displayName(),
                    options.output(), result.target().displayName());
            if (options.statistics()) {
                out.print(DocumentStatistics.of(result.document()).report());
            }
            return OK;
        } catch (NoSuchFileException e) {
            err.println("Error: file not found: " + e.getFile());
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
        } catch (TextFormatException e) {
            err.println("Error: " + e.getMessage());
        }
        return CONVERSION_ERROR;
    }

    static String formatTable(FormatRegistry registry) {
        return registry.formats().stream()
                .map(format -> "  %-7s %-18s %-24s %s".formatted(format.id(), format.displayName(),
                        format.extensions().stream().map(ext -> "." + ext).collect(Collectors.joining(" ")),
                        capabilities(format)))
                .collect(Collectors.joining("\n", "Supported formats:\n", "\n"));
    }

    private static String capabilities(TextFormat format) {
        return format.capabilities().stream()
                .sorted()
                .map(Capability::name)
                .map(String::toLowerCase)
                .collect(Collectors.joining("/"));
    }
}
