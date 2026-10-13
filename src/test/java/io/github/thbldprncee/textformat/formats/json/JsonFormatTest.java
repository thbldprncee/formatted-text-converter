package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.TextFormatException;
import io.github.thbldprncee.textformat.format.WriterOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFormatTest {

    private final JsonFormat format = new JsonFormat();

    private Document read(String json) {
        return format.parser().parse(json);
    }

    @Test
    void formattedContentUsesChildrenInsteadOfText() {
        String json = format.writer(WriterOptions.DEFAULT).render(Document.of(
                Heading.of(2, new Text("A "), Italic.of("b"))));
        assertThat(json).contains("\"level\": 2,\n    \"children\": [").contains("\"type\": \"italic\",\n        \"text\": \"b\"");
    }

    @Test
    void indentationIsConfigurable() {
        Document document = Document.of(Paragraph.of(new Text("x")));
        assertThat(format.writer(new WriterOptions(IndentStyle.spaces(4))).render(document))
                .startsWith("[\n    {\n        \"type\": \"paragraph\"");
        assertThat(format.writer(new WriterOptions(IndentStyle.tabs())).render(document))
                .startsWith("[\n\t{\n\t\t\"type\": \"paragraph\"");
    }

    @Test
    void topLevelInlineJoinsOnlyAContinuingParagraph() {
        assertThat(read("""
                [{"type": "bold", "text": "A"}, {"type": "paragraph", "children": [{"type": "text", "content": "Next"}]},
                 {"type": "italic", "text": "B"}, {"type": "paragraph", "children": [{"type": "text", "content": ", tail"}]},
                 {"type": "code_inline", "code": "c"}]
                """).blocks()).containsExactly(
                Paragraph.of(Bold.of("A")),
                Paragraph.of(new Text("Next")),
                Paragraph.of(Italic.of("B"), new Text(", tail")),
                Paragraph.of(new InlineCode("c")));
    }

    @Test
    void lenientInputVariants() {
        assertThat(read("""
                {"type": "document", "children": [
                  {"type": "heading", "level": "9", "text": "Big"},
                  {"type": "list", "ordered": "true", "children": [
                    {"type": "list_item", "children": [{"type": "text", "content": "inline"}, {"type": "horizontal_rule"},
                                                       {"type": "text", "content": "after"}]}]},
                  {"type": "code_block", "code": "x"},
                  {"type": "blockquote", "children": []},
                  {"type": "image", "url": "u"},
                  {"type": "bold"}
                ]}""").blocks()).containsExactly(
                Heading.of(6, new Text("Big")),
                ListBlock.numbered(ListItem.of(Paragraph.of(new Text("inline")), new ThematicBreak(),
                        Paragraph.of(new Text("after")))),
                new CodeBlock("x"),
                BlockQuote.of(),
                Paragraph.of(new Image("u", "")),
                Paragraph.of(new Bold(List.of())));
        assertThat(read("{\"type\": \"paragraph\", \"children\": []}").blocks()).containsExactly(new Paragraph(List.of()));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "42 | must be an array",
            "[1] | expected an object",
            "[{}] | missing \"type\"",
            "[{\"type\": 1}] | expected a string",
            "[{\"type\": \"nope\"}] | unknown element type 'nope'",
            "[{\"type\": \"text\"}] | missing \"content\"",
            "[{\"type\": \"paragraph\", \"children\": {}}] | expected an array",
            "[{\"type\": \"paragraph\", \"children\": [{\"type\": \"horizontal_rule\"}]}] | block element inside inline",
            "[{\"type\": \"list\", \"children\": [{\"type\": \"text\", \"content\": \"x\"}]}] | expected 'list_item'",
            "[{\"type\": \"table\", \"children\": [{\"type\": \"table_row\", \"children\": [{\"type\": \"text\", \"content\": \"\"}]}]}] | expected 'table_cell'",
            "[{\"type\": \"header\", \"level\": true}] | expected a number"})
    void invalidDocumentsExplainWhereAndWhy(String json, String message) {
        assertThatThrownBy(() -> read(json)).isInstanceOf(TextFormatException.class).hasMessageContaining(message);
    }

    @Test
    void errorMessagesContainTheJsonPath() {
        assertThatThrownBy(() -> read("[{\"type\": \"paragraph\", \"children\": [{\"type\": \"x\"}]}]"))
                .hasMessageContaining("$[0].children[0]");
    }

    @Test
    void detection() {
        assertThat(format.detect("  [ ]")).isEqualTo(90);
        assertThat(format.detect("{\"a\":1}")).isEqualTo(90);
        assertThat(format.detect("{\\rtf1}")).isZero();
        assertThat(format.detect("# md")).isZero();
    }
}
