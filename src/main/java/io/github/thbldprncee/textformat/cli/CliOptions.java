package io.github.thbldprncee.textformat.cli;

import io.github.thbldprncee.textformat.common.IndentStyle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parsed command line: {@code <in_file> <out_file> [options]}.
 *
 * @param from optional input format id overriding content detection
 */
record CliOptions(Path input, Path output, IndentStyle indent, boolean stripStyles, Optional<String> from,
                  boolean statistics, Command command) {

    enum Command { CONVERT, HELP, LIST_FORMATS }

    static final String USAGE = """
            Usage: textconv <in_file> <out_file> [options]

            The input format is recognised by content, the output format by the
            extension of <out_file> (.md .json .xhtml .html .rtf .txt .tex).

            Options:
              --indent=<n|tab>  indentation of nested elements in XHTML/JSON (default: 2)
              --strip-styles    remove bold, italic and strikethrough
              --from=<format>   read the input as this format instead of detecting it
              --stats           print document statistics
              --list-formats    list supported formats
              -h, --help        show this help
            """;

    static CliOptions parse(String... args) {
        List<String> positional = new ArrayList<>();
        IndentStyle indent = IndentStyle.DEFAULT;
        boolean stripStyles = false;
        boolean statistics = false;
        Optional<String> from = Optional.empty();
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                return command(Command.HELP);
            } else if (arg.equals("--list-formats")) {
                return command(Command.LIST_FORMATS);
            } else if (arg.equals("--strip-styles")) {
                stripStyles = true;
            } else if (arg.equals("--stats")) {
                statistics = true;
            } else if (arg.startsWith("--indent=")) {
                indent = IndentStyle.parse(arg.substring("--indent=".length()));
            } else if (arg.startsWith("--from=")) {
                from = Optional.of(arg.substring("--from=".length()));
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown option " + arg);
            } else {
                positional.add(arg);
            }
        }
        if (positional.size() != 2) {
            throw new IllegalArgumentException("Expected <in_file> <out_file>, got " + positional.size() + " argument(s)");
        }
        return new CliOptions(Path.of(positional.get(0)), Path.of(positional.get(1)), indent, stripStyles, from,
                statistics, Command.CONVERT);
    }

    private static CliOptions command(Command command) {
        return new CliOptions(null, null, IndentStyle.DEFAULT, false, Optional.empty(), false, command);
    }
}
