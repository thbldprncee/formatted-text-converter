package io.github.thbldprncee.textformat.format;

import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.UnsupportedFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatRegistryTest {

    private final FormatRegistry registry = FormatRegistry.loadInstalled();

    @Test
    void discoversAllFormatsWithServiceLoader() {
        assertThat(registry.formats()).extracting(TextFormat::id)
                .containsExactly("json", "latex", "md", "rtf", "txt", "xhtml");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\\rtf1\\ansi hi} | rtf",
            "  [{\"type\": \"text\"}] | json",
            "<?xml version=\"1.0\"?><html/> | xhtml",
            "<p>hi</p> | xhtml",
            "# Heading | md",
            "just some words | md",
            "<https://a.b> autolink | md"})
    void recognisesTheInputByContent(String content, String expected) {
        assertThat(registry.detect(content).id()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"out.json, json", "OUT.MD, md", "a.markdown, md", "x.html, xhtml", "x.xhtml, xhtml", "d.rtf, rtf",
            "t.txt, txt", "p.tex, latex", "dir.v1/file.json, json"})
    void outputFormatComesFromTheExtension(String path, String expected) {
        assertThat(registry.forOutput(Path.of(path)).id()).isEqualTo(expected);
    }

    @Test
    void unsupportedOrMissingExtensions() {
        assertThatThrownBy(() -> registry.forOutput(Path.of("out.pdf")))
                .isInstanceOf(UnsupportedFormatException.class).hasMessageContaining(".pdf").hasMessageContaining(".json");
        assertThatThrownBy(() -> registry.forOutput(Path.of("noext"))).hasMessageContaining("no extension");
        assertThatThrownBy(() -> registry.forOutput(Path.of("dot."))).hasMessageContaining("no extension");
        assertThatThrownBy(() -> registry.require("docx")).hasMessageContaining("Unknown format 'docx'");
        assertThat(registry.byId("markdown")).map(TextFormat::id).contains("md");
    }

    @Test
    void registryInvariants() {
        TextFormat dummy = new TextFormat() {
            public String id() { return "md"; }
            public String displayName() { return "Dummy"; }
            public List<String> extensions() { return List.of("x"); }
            public Set<Capability> capabilities() { return Set.of(); }
        };
        assertThatThrownBy(() -> new FormatRegistry(List.of(dummy, dummy))).hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> new FormatRegistry(List.of()).detect("x")).isInstanceOf(UnsupportedFormatException.class);
        assertThatThrownBy(() -> dummy.writer(WriterOptions.DEFAULT)).isInstanceOf(UnsupportedFormatException.class);
        assertThat(dummy.detect("anything")).isZero();
        assertThat(WriterOptions.DEFAULT.withIndent(IndentStyle.tabs()).indent()).isEqualTo(IndentStyle.tabs());
    }
}
