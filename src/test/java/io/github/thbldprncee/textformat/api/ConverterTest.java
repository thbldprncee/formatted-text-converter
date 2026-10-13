package io.github.thbldprncee.textformat.api;

import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.UnsupportedFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterTest {

    @Test
    void stripsStylesAndIndents() {
        Converter converter = Converter.builder().stripStyles(true).indent(IndentStyle.tabs()).build();
        assertThat(converter.convert("**bold** *it*", "json")).isEqualTo("""
                [
                \t{
                \t\t"type": "paragraph",
                \t\t"children": [
                \t\t\t{
                \t\t\t\t"type": "text",
                \t\t\t\t"content": "bold it"
                \t\t\t}
                \t\t]
                \t}
                ]
                """);
    }

    @Test
    void sourceFormatOverridesDetection() {
        Converter asText = Converter.builder().sourceFormat("txt").build();
        assertThat(asText.read("# not a heading")).isEqualTo(Document.of(Paragraph.of(new Text("# not a heading"))));
        assertThatThrownBy(() -> Converter.builder().sourceFormat("latex").build().read("x"))
                .isInstanceOf(UnsupportedFormatException.class).hasMessageContaining("cannot be used as input");
    }

    @Test
    void customTransformerRunsAfterParsing() {
        Converter converter = Converter.defaults().toBuilder()
                .transformer(document -> Document.of(new ThematicBreak()))
                .build();
        assertThat(converter.convert("anything", "md")).isEqualTo("---\n");
        assertThat(converter.registry().formats()).hasSize(6);
    }

    @Test
    void convertsFiles(@TempDir Path dir) throws IOException {
        Path input = Files.writeString(dir.resolve("input.data"), "# Привет\n\nмир");
        Path output = dir.resolve("out.html");

        Converter.Result result = Converter.defaults().convert(input, output);

        assertThat(result.source().id()).isEqualTo("md");
        assertThat(result.target().id()).isEqualTo("xhtml");
        assertThat(Files.readString(output)).contains("<h1>Привет</h1>", "<p>мир</p>");
    }
}
