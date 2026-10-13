package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Strikethrough;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownParserTest {

    private final MarkdownParser parser = new MarkdownParser();

    private Document parse(String markdown) {
        return parser.parse(markdown);
    }

    private List<Inline> inline(String markdown) {
        return ((Paragraph) parse(markdown).blocks().getFirst()).children();
    }

    @Nested
    class Blocks {

        @ParameterizedTest
        @CsvSource({"'# One', 1", "'### Three ###', 3", "'###### Six', 6"})
        void atxHeadings(String line, int level) {
            assertThat(parse(line).blocks()).singleElement()
                    .isInstanceOfSatisfying(Heading.class, heading -> assertThat(heading.level()).isEqualTo(level));
        }

        @Test
        void setextHeadings() {
            assertThat(parse("Title\n=====\n\nSub\n---").blocks()).containsExactly(
                    Heading.of(1, new Text("Title")), Heading.of(2, new Text("Sub")));
        }

        @Test
        void sevenHashesIsAParagraph() {
            assertThat(parse("####### no").blocks().getFirst()).isInstanceOf(Paragraph.class);
        }

        @Test
        void paragraphLinesAreJoined() {
            assertThat(parse("first\nsecond\n\nthird").blocks()).containsExactly(
                    Paragraph.of(new Text("first second")), Paragraph.of(new Text("third")));
        }

        @Test
        void fencedCodeKeepsContentAndLanguage() {
            assertThat(parse("```java\nint x = 1;\n  **not bold**\n```").blocks()).containsExactly(
                    new CodeBlock("int x = 1;\n  **not bold**", "java"));
        }

        @Test
        void tildeFenceAndUnclosedFence() {
            assertThat(parse("~~~\na\n~~~").blocks()).containsExactly(new CodeBlock("a"));
            assertThat(parse("```\nopen").blocks()).containsExactly(new CodeBlock("open"));
        }

        @Test
        void thematicBreaks() {
            assertThat(parse("---\n\n***\n\n_ _ _").blocks())
                    .containsExactly(new ThematicBreak(), new ThematicBreak(), new ThematicBreak());
        }

        @Test
        void nestedBlockQuotesWithLazyContinuation() {
            assertThat(parse("> quoted\nlazy\n>\n> > deeper").blocks()).containsExactly(BlockQuote.of(
                    Paragraph.of(new Text("quoted lazy")), BlockQuote.of(Paragraph.of(new Text("deeper")))));
        }

        @Test
        void bulletAndOrderedListsAreSeparateLists() {
            assertThat(parse("- a\n- b\n\n1. c\n2. d").blocks()).containsExactly(
                    ListBlock.bullet(ListItem.of(new Text("a")), ListItem.of(new Text("b"))),
                    ListBlock.numbered(ListItem.of(new Text("c")), ListItem.of(new Text("d"))));
        }

        @Test
        void nestedListsAndMultiParagraphItems() {
            String markdown = """
                    - one
                      - one.a
                        1. deep
                    - two

                      second paragraph
                    * other bullet
                    """;
            assertThat(parse(markdown).blocks()).containsExactly(
                    ListBlock.bullet(
                            ListItem.of(Paragraph.of(new Text("one")), ListBlock.bullet(ListItem.of(
                                    Paragraph.of(new Text("one.a")), ListBlock.numbered(ListItem.of(new Text("deep")))))),
                            ListItem.of(Paragraph.of(new Text("two")), Paragraph.of(new Text("second paragraph")))),
                    ListBlock.bullet(ListItem.of(new Text("other bullet"))));
        }

        @Test
        void listItemLazyContinuationAndTabs() {
            assertThat(parse("-\titem\ncontinued").blocks()).containsExactly(
                    ListBlock.bullet(ListItem.of(new Text("item continued"))));
        }

        @Test
        void tables() {
            String markdown = """
                    | Name | `a|b` | Esc \\| pipe |
                    | :--- | :---: | ---: |
                    | x | **y** |
                    """;
            assertThat(parse(markdown).blocks()).containsExactly(Table.of(
                    TableRow.of(TableCell.of("Name"), TableCell.of(new InlineCode("a|b")), TableCell.of("Esc | pipe")),
                    TableRow.of(TableCell.of("x"), TableCell.of(Bold.of("y")))));
        }

        @Test
        void pipeWithoutSeparatorIsText() {
            assertThat(parse("a | b").blocks()).containsExactly(Paragraph.of(new Text("a | b")));
        }

        @Test
        void windowsLineEndingsAndEmptyInput() {
            assertThat(parse("# T\r\n\r\ntext\r\n").blocks()).hasSize(2);
            assertThat(parse("").isEmpty()).isTrue();
            assertThat(parse("#").blocks()).containsExactly(new Heading(1, List.of()));
        }
    }

    @Nested
    class Inlines {

        @Test
        void emphasisVariants() {
            assertThat(inline("*a* _b_ **c** __d__ ***e*** ~~f~~")).containsExactly(
                    Italic.of("a"), new Text(" "), Italic.of("b"), new Text(" "), Bold.of("c"), new Text(" "),
                    Bold.of("d"), new Text(" "), Bold.of(Italic.of("e")), new Text(" "), Strikethrough.of("f"));
        }

        @Test
        void nestedEmphasis() {
            assertThat(inline("**bold *and italic* inside**")).containsExactly(
                    Bold.of(new Text("bold "), Italic.of("and italic"), new Text(" inside")));
            assertThat(inline("*a **b** c*")).containsExactly(Italic.of(new Text("a "), Bold.of("b"), new Text(" c")));
        }

        @Test
        void nonEmphasis() {
            assertThat(inline("snake_case_name and 2 * 3 * 4 and **unclosed"))
                    .containsExactly(new Text("snake_case_name and 2 * 3 * 4 and **unclosed"));
        }

        @Test
        void codeSpans() {
            assertThat(inline("`a` ``b ` c`` ` `` ` ` d `")).containsExactly(
                    new InlineCode("a"), new Text(" "), new InlineCode("b ` c"), new Text(" "), new InlineCode("``"),
                    new Text(" "), new InlineCode("d"));
            assertThat(inline("``unclosed")).containsExactly(new Text("``unclosed"));
        }

        @Test
        void emphasisDoesNotCloseInsideCode() {
            assertThat(inline("*a `*` b*")).containsExactly(
                    Italic.of(new Text("a "), new InlineCode("*"), new Text(" b")));
        }

        @Test
        void linksImagesAndAutolinks() {
            assertThat(inline("[**x**](https://e.com \"t\") ![pic *1*](p.png) <https://a.b> [y](<u v>)")).containsExactly(
                    new Link("https://e.com", List.of(Bold.of("x"))), new Text(" "), new Image("p.png", "pic 1"),
                    new Text(" "), Link.of("https://a.b", "https://a.b"), new Text(" "), Link.of("u v", "y"));
        }

        @Test
        void brokenLinksAreText() {
            assertThat(inline("[a] [b](c <notalink> ![x]")).containsExactly(new Text("[a] [b](c <notalink> ![x]"));
        }

        @Test
        void backslashEscapes() {
            assertThat(inline("\\*not\\* \\# \\` \\\\ \\q")).containsExactly(new Text("*not* # ` \\ \\q"));
        }
    }
}
