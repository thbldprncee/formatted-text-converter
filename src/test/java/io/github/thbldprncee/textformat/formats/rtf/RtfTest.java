package io.github.thbldprncee.textformat.formats.rtf;

import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
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
import io.github.thbldprncee.textformat.common.ParseException;
import io.github.thbldprncee.textformat.format.WriterOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RtfTest {

    private final RtfParser parser = new RtfParser();
    private final RtfFormat format = new RtfFormat();

    @Test
    void readsRtfFromOtherProducers() {
        String rtf = """
                {\\rtf1\\ansi\\ansicpg1251\\deff0
                {\\fonttbl{\\f0\\froman Times New Roman;}{\\f2\\fnil Consolas;}}
                {\\colortbl;\\red0\\green0\\blue0;}
                {\\*\\generator Word;}{\\info{\\title t}}
                \\pard\\outlinelevel1\\b\\fs32 Heading\\b0\\par
                \\pard Plain {\\b bold} {\\i italic} {\\strike gone}\\~and {\\f2 code} \\'c6\\'e5 \\u1046?\\uc0\\u1078 tab\\tab end\\par
                \\pard Quotes \\ldblquote x\\rdblquote \\emdash  \\{\\}\\\\ \\_\\-\\par
                {\\field{\\*\\fldinst{HYPERLINK "https://e.com"}}{\\fldrslt link}}\\par
                {\\field{\\*\\fldinst PAGE}{\\fldrslt 7}}\\par
                \\pard\\bullet\\tab first\\par
                \\pard\\bullet\\tab second\\par
                \\trowd\\cellx1000\\cellx2000
                \\pard\\intbl a\\cell b\\par c\\cell\\row
                \\pard after
                }""";
        assertThat(parser.parse(rtf).blocks()).containsExactly(
                Heading.of(2, new Text("Heading")),
                Paragraph.of(new Text("Plain "), Bold.of("bold"), new Text(" "), Italic.of("italic"), new Text(" "),
                        Strikethrough.of("gone"), new Text("\u00A0and "), new InlineCode("code"),
                        new Text(" Же Жжtab\tend")),
                Paragraph.of(new Text("Quotes \u201Cx\u201D\u2014 {}\\ -")),
                Paragraph.of(Link.of("https://e.com", "link")),
                Paragraph.of(new Text("7")),
                ListBlock.bullet(ListItem.of(new Text("first")), ListItem.of(new Text("second"))),
                Table.of(TableRow.of(TableCell.of("a"), TableCell.of("b\nc"))),
                Paragraph.of(new Text("after")));
    }

    @Test
    void numberedListsFromOtherProducersAndImages() {
        assertThat(parser.parse("{\\rtf1 \\pard 1.\\tab one\\par 2) two\\par {\\field{\\*\\fldinst INCLUDEPICTURE \"p.png\"}{\\fldrslt Pic}}\\par}")
                .blocks()).containsExactly(
                ListBlock.numbered(ListItem.of(new Text("one")), ListItem.of(new Text("two"))),
                Paragraph.of(new Image("p.png", "Pic")));
    }

    @Test
    void rejectsNonRtfAndUnclosedGroups() {
        assertThatThrownBy(() -> parser.parse("hello")).isInstanceOf(ParseException.class);
        assertThatThrownBy(() -> parser.parse("{\\rtf1 {\\fonttbl x")).isInstanceOf(ParseException.class);
        assertThat(parser.parse("{\\rtf1 x}}").blocks()).containsExactly(Paragraph.of(new Text("x")));
    }

    @Test
    void writesUnicodeAndSpecialsAsAnsiSafeEscapes() {
        String rtf = format.writer(WriterOptions.DEFAULT).render(Document.of(
                Paragraph.of(new Text("Ж{}\\\t\n\r\uD83D\uDE00"))));
        assertThat(rtf).startsWith("{\\rtf1\\ansi\\ansicpg1252")
                .contains("\\u1046?\\{\\}\\\\\\tab \\line \\u-10179?\\u-8704?")
                .isEqualTo(rtf.chars().allMatch(ch -> ch < 0x80) ? rtf : "non-ascii");
        assertThat(format.writer(WriterOptions.DEFAULT).render(Document.of())).endsWith("}\n");
    }

    @Test
    void detection() {
        assertThat(format.detect("  {\\rtf1}")).isEqualTo(100);
        assertThat(format.detect("{}")).isZero();
        assertThat(ListItem.of(new Text("x")).accept(new RtfWriter())).contains("\\bullet");
    }
}
