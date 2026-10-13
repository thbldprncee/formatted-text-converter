package io.github.thbldprncee.textformat.formats.xhtml;

import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
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
import io.github.thbldprncee.textformat.ast.ThematicBreak;
import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.ParseException;
import io.github.thbldprncee.textformat.format.WriterOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XhtmlTest {

    private final XhtmlParser parser = new XhtmlParser();
    private final XhtmlFormat format = new XhtmlFormat();

    @Test
    void readsHtmlDocument() {
        String html = """
                <?xml version="1.0"?>
                <!DOCTYPE html>
                <html><head><title>ignored</title><style>p {}</style></head>
                <body>
                  <!-- comment -->
                  <h2>Hello,   <em>world</em></h2>
                  <div><p>
                     A &amp; B &lt;tag&gt; &#1046; &#x416; &nbsp;x <b>b</b><strong>s</strong><i>i</i><s>x</s><del>y</del>
                     <a href="https://e.com?a=1&amp;b=2">link</a> <a>plain</a> <img src="p.png" alt="P"/><br>end
                     <span>span</span> <code>a  &lt; b</code>
                  </p></div>
                  loose text
                  <pre><code class="hljs language-java">int x;
                  y;
                </code></pre>
                  <ul><li>one</li><li>two<ol><li>deep</li></ol></li></ul>
                  <table><thead><tr><th>H</th></tr></thead><tbody><tr><td> c </td><td></td></tr></tbody><tr><td>d</td></tr></table>
                  <blockquote><p>q</p></blockquote><hr><script>alert(1)</script>
                </body></html>
                """;
        assertThat(parser.parse(html).blocks()).containsExactly(
                Heading.of(2, new Text("Hello, "), Italic.of("world")),
                Paragraph.of(new Text("A & B <tag> Ж Ж \u00A0x "), Bold.of("b"), Bold.of("s"), Italic.of("i"),
                        Strikethrough.of("x"), Strikethrough.of("y"), new Text(" "),
                        Link.of("https://e.com?a=1&b=2", "link"), new Text(" plain "), new Image("p.png", "P"),
                        new Text(" end span "), new InlineCode("a  < b")),
                Paragraph.of(new Text("loose text")),
                new CodeBlock("int x;\n  y;", "java"),
                ListBlock.bullet(ListItem.of(new Text("one")),
                        ListItem.of(Paragraph.of(new Text("two")), ListBlock.numbered(ListItem.of(new Text("deep"))))),
                Table.of(TableRow.of(TableCell.of("H")), TableRow.of(TableCell.of("c"), TableCell.of("")),
                        TableRow.of(TableCell.of("d"))),
                BlockQuote.of(Paragraph.of(new Text("q"))),
                new ThematicBreak());
    }

    @Test
    void readsTheLegacyDocumentWrapperAndCdata() {
        assertThat(parser.parse("<document><p><![CDATA[<raw>]]></p>text</document>").blocks()).containsExactly(
                Paragraph.of(new Text("<raw>")), Paragraph.of(new Text("text")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<p>open", "<p></b>", "</p>", "<p &>", "<p a=\"x>", "<p>&unknown;</p>", "<>"})
    void rejectsMalformedMarkup(String html) {
        assertThatThrownBy(() -> parser.parse(html)).isInstanceOf(ParseException.class);
    }

    @Test
    void keepsStrayAmpersandsAndUnquotedAttributes() {
        assertThat(parser.parse("<p>a & b <a href=x.html>l</a></p>").blocks())
                .containsExactly(Paragraph.of(new Text("a & b "), Link.of("x.html", "l")));
    }

    @Test
    void writesEscapedIndentedXhtml() {
        Document document = Document.of(
                Heading.of(1, new Text("T<1>")),
                ListBlock.bullet(ListItem.of(new Text("a & b"))),
                new CodeBlock("x < y\nz", "c\""));
        String xhtml = format.writer(new WriterOptions(IndentStyle.tabs())).render(document);
        assertThat(xhtml).contains(
                "<title>T&lt;1&gt;</title>",
                "\t<body>\n\t\t<h1>T&lt;1&gt;</h1>\n\t\t<ul>\n\t\t\t<li>a &amp; b</li>\n\t\t</ul>",
                "<pre><code class=\"language-c&quot;\">x &lt; y\nz</code></pre>");
        assertThat(format.writer(WriterOptions.DEFAULT).render(Document.of())).contains("<title>Document</title>");
    }

    @Test
    void detection() {
        assertThat(format.detect("<?xml version='1.0'?><a/>")).isEqualTo(95);
        assertThat(format.detect("<!doctype html>")).isEqualTo(95);
        assertThat(format.detect("<html></html>")).isEqualTo(95);
        assertThat(format.detect("<p>x</p>")).isEqualTo(80);
        assertThat(format.detect("<https://link.example>")).isZero();
        assertThat(format.detect("plain")).isZero();
        assertThat(new Bold(List.of()).children()).isEmpty();
    }
}
