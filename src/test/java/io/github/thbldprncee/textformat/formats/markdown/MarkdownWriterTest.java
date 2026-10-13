package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.TestDocuments;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownWriterTest {

    private final MarkdownWriter writer = new MarkdownWriter();
    private final MarkdownParser parser = new MarkdownParser();

    private String write(io.github.thbldprncee.textformat.ast.Block... blocks) {
        return writer.render(Document.of(blocks));
    }

    @Test
    void writesReadableMarkdown() {
        assertThat(writer.render(TestDocuments.full())).isEqualTo("""
                # Title

                Plain **bold** and *italic*, ~~gone~~ with `code()` and [a link](https://example.com/a?b=1).

                ## Section *two*

                ![Alt text](img/logo.png)

                - One
                - Two
                  1. Two.1
                  2. Two.2
                - Three

                1. First
                2. **Second**

                ```cpp
                int main() {
                    return 0;
                }
                ```

                | Name  | Value  |
                | ----- | ------ |
                | **a** | 1      |
                | b     | `x\\|y` |

                > Quoted
                >
                > > Nested

                ---

                Кириллица: «ёлка» — 5 €
                """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"# not a heading", "- not a list", "+ plus", "> not a quote", "1. not a list",
            "*stars*", "under_score", "`tick`", "[brackets]", "~~strike~~", "<tag>", "back\\slash"})
    void specialTextSurvivesARoundTrip(String text) {
        Document document = Document.of(Paragraph.of(new Text(text)));
        assertThat(parser.parse(writer.render(document))).isEqualTo(document);
    }

    @Test
    void codeWithBackticksGetsLongerFences() {
        assertThat(write(Paragraph.of(new InlineCode("a`b")))).isEqualTo("``a`b``\n");
        assertThat(write(Paragraph.of(new InlineCode("`x`")))).isEqualTo("`` `x` ``\n");
        assertThat(write(new CodeBlock("```\ninner\n```"))).startsWith("````\n");
        assertThat(write(new CodeBlock(""))).isEqualTo("```\n```\n");
    }

    @Test
    void urlsWithSpacesAreBracketed() {
        assertThat(write(Paragraph.of(Link.of("a b", "x")))).isEqualTo("[x](<a b>)\n");
    }

    @Test
    void edgeCases() {
        assertThat(writer.render(Document.of())).isEmpty();
        assertThat(write(new Table(List.of()))).isEmpty();
        assertThat(write(Paragraph.of(new Text("line\nbreak")))).isEqualTo("line break\n");
        assertThat(write(ListBlock.bullet(new ListItem(List.of())))).isEqualTo("- \n");
        assertThat(ListItem.of(new Text("x")).accept(writer)).isEqualTo("- x");
        assertThat(Table.of(io.github.thbldprncee.textformat.ast.TableRow.of(
                io.github.thbldprncee.textformat.ast.TableCell.of("a"))).rows().getFirst().accept(writer)).isEqualTo("| a |");
    }
}
