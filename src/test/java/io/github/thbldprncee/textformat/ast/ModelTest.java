package io.github.thbldprncee.textformat.ast;

import io.github.thbldprncee.textformat.TestDocuments;
import io.github.thbldprncee.textformat.ast.stats.DocumentStatistics;
import io.github.thbldprncee.textformat.ast.transform.DocumentTransformer;
import io.github.thbldprncee.textformat.ast.transform.StyleRemover;
import io.github.thbldprncee.textformat.ast.transform.TextMerger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelTest {

    @Test
    void nodesAreImmutableValues() {
        List<Inline> children = new java.util.ArrayList<>(List.of(new Text("a")));
        Paragraph paragraph = new Paragraph(children);
        children.add(new Text("b"));

        assertThat(paragraph.children()).hasSize(1);
        assertThat(paragraph).isEqualTo(Paragraph.of(new Text("a")));
        assertThatThrownBy(() -> paragraph.children().add(new Text("c"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validation() {
        assertThatThrownBy(() -> Heading.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Heading.of(7)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Heading.clampLevel(0)).isEqualTo(1);
        assertThat(Heading.clampLevel(42)).isEqualTo(6);
        assertThatThrownBy(() -> new Text(null)).isInstanceOf(NullPointerException.class);
        assertThat(new CodeBlock("x", null).hasLanguage()).isFalse();
        assertThat(new Image("u", null).alt()).isEmpty();
    }

    @Test
    void builderAndQueries() {
        Document document = Document.builder()
                .heading(1, "T")
                .paragraph("p")
                .paragraph(Bold.of("b"), new Text("!"))
                .code("x", "sh")
                .add(new ThematicBreak())
                .build();
        assertThat(document.blocks()).hasSize(5);
        assertThat(Nodes.plainText(document)).isEqualTo("T\np\nb!\nx\n");
        assertThat(Nodes.descendantsAndSelf(document).count()).isEqualTo(11);
        assertThat(Paragraph.of(Bold.of("b")).isPlainText()).isFalse();
        assertThat(Table.of(TableRow.of(TableCell.of("a")), TableRow.of(TableCell.of("a"), TableCell.of(""))).columnCount())
                .isEqualTo(2);
        assertThat(Nodes.plainText(TestDocuments.full())).contains("Plain bold and italic", "Two.1", "Name Value", "x|y");
    }

    @Test
    void styleRemoverKeepsTextAndStructure() {
        Document styled = Document.of(
                Heading.of(1, new Text("A "), Bold.of(new Text("b "), Italic.of("c"))),
                ListBlock.bullet(ListItem.of(Paragraph.of(Strikethrough.of("s"), new Text("!")))),
                Table.of(TableRow.of(TableCell.of(Bold.of("cell")))),
                BlockQuote.of(Paragraph.of(Link.of("u", "l"), Italic.of(new InlineCode("x")))),
                new CodeBlock("**kept**"),
                new ThematicBreak());

        assertThat(StyleRemover.instance().apply(styled)).isEqualTo(Document.of(
                Heading.of(1, new Text("A b c")),
                ListBlock.bullet(ListItem.of(Paragraph.of(new Text("s!")))),
                Table.of(TableRow.of(TableCell.of("cell"))),
                BlockQuote.of(Paragraph.of(Link.of("u", "l"), new InlineCode("x"))),
                new CodeBlock("**kept**"),
                new ThematicBreak()));
    }

    @Test
    void transformersCompose() {
        DocumentTransformer addRule = document -> Document.of(new ThematicBreak());
        assertThat(DocumentTransformer.chain().apply(Document.of())).isEqualTo(Document.of());
        assertThat(DocumentTransformer.chain(new TextMerger(), addRule).apply(Document.of(Paragraph.of())))
                .isEqualTo(Document.of(new ThematicBreak()));
        assertThat(new TextMerger().apply(Document.of(Paragraph.of(new Text("a"), new Text(""), new Text("b")))))
                .isEqualTo(Document.of(Paragraph.of(new Text("ab"))));
    }

    @Test
    void statistics() {
        DocumentStatistics stats = DocumentStatistics.of(Document.of(
                Heading.of(1, new Text("Hello world")), Paragraph.of(new Text("one "), Bold.of("two")))) ;
        assertThat(stats.words()).isEqualTo(4);
        assertThat(stats.characters()).isEqualTo(19);
        assertThat(stats.charactersWithoutSpaces()).isEqualTo(16);
        assertThat(stats.count(Bold.class)).isEqualTo(1);
        assertThat(stats.count(Table.class)).isZero();
        assertThat(stats.readingMinutes()).isEqualTo(1);
        assertThat(stats.report()).contains("Words:                 4", "Bold", "Heading");
    }
}
