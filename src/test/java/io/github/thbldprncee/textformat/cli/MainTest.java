package io.github.thbldprncee.textformat.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void convertsWithOptionsAndPrintsStatistics(@TempDir Path dir) throws IOException {
        Path in = Files.writeString(dir.resolve("in.md"), "# T\n\n**Bold** words here");
        Path out = dir.resolve("out.json");

        assertThat(run(in.toString(), out.toString(), "--indent=4", "--strip-styles", "--stats")).isZero();

        assertThat(Files.readString(out)).contains("        \"type\": \"header\"").doesNotContain("bold");
        assertThat(stdout()).contains("(Markdown) -> ", "(JSON)", "Words:                 4");
    }

    @Test
    void fromOptionReadsLiterally(@TempDir Path dir) throws IOException {
        Path in = Files.writeString(dir.resolve("in.txt"), "*x*");
        Path out = dir.resolve("out.md");
        assertThat(run(in.toString(), out.toString(), "--from=txt")).isZero();
        assertThat(Files.readString(out)).isEqualTo("\\*x\\*\n");
    }

    @Test
    void helpAndFormatList() {
        assertThat(run("--help")).isZero();
        assertThat(stdout()).contains("Usage: textconv <in_file> <out_file>");
        assertThat(run("--list-formats")).isZero();
        assertThat(stdout()).contains("md      Markdown", "latex   LaTeX", "write", "read/write");
    }

    @Test
    void usageErrors() {
        assertThat(run()).isEqualTo(Main.USAGE_ERROR);
        assertThat(run("a", "b", "c")).isEqualTo(Main.USAGE_ERROR);
        assertThat(run("a", "b", "--bogus")).isEqualTo(Main.USAGE_ERROR);
        assertThat(run("a", "b", "--indent=wide")).isEqualTo(Main.USAGE_ERROR);
        assertThat(stderr()).contains("Unknown option --bogus", "Usage:");
    }

    @Test
    void conversionErrors(@TempDir Path dir) throws IOException {
        assertThat(run(dir.resolve("missing.md").toString(), dir.resolve("o.json").toString()))
                .isEqualTo(Main.CONVERSION_ERROR);
        assertThat(stderr()).contains("file not found");

        Path bad = Files.writeString(dir.resolve("bad.json"), "[{\"type\": \"text\"}]");
        assertThat(run(bad.toString(), dir.resolve("o.md").toString())).isEqualTo(Main.CONVERSION_ERROR);
        assertThat(stderr()).contains("missing \"content\"");

        assertThat(run(bad.toString(), dir.resolve("o.pdf").toString())).isEqualTo(Main.CONVERSION_ERROR);
        assertThat(stderr()).contains("Unsupported output format '.pdf'");

        assertThat(run(bad.toString(), dir.toString() + "/")).isEqualTo(Main.CONVERSION_ERROR);
    }
}
