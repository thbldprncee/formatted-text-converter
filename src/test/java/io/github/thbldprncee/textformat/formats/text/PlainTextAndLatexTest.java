package io.github.thbldprncee.textformat.formats.text;

import io.github.thbldprncee.textformat.TestDocuments;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.Link;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.common.UnsupportedFormatException;
import io.github.thbldprncee.textformat.format.WriterOptions;
import io.github.thbldprncee.textformat.formats.latex.LatexFormat;
import io.github.thbldprncee.textformat.formats.latex.LatexWriter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainTextAndLatexTest {

    private final PlainTextFormat text = new PlainTextFormat();
    private final LatexFormat latex = new LatexFormat();

    @Test
    void plainTextDropsMarkupButKeepsStructure() {
        String output = text.writer(WriterOptions.DEFAULT).render(TestDocuments.full());
        assertThat(output)
                .startsWith("Title\n=====\n\nPlain bold and italic, gone with code() and a link (https://example.com/a?b=1).")
                .contains("Section two\n-----------", "[Alt text]", "* One\n* Two\n  1. Two.1\n  2. Two.2",
                        "    int main() {", "Name  Value\na     1\nb     x|y", "| Quoted\n|\n| | Nested",
                        "-".repeat(40))
                .doesNotContain("**", "<");
        assertThat(text.writer(WriterOptions.DEFAULT).render(Document.of(
                Heading.of(4, new Text("h4")), Paragraph.of(Link.of("u", "u"), new Image("i", ""))))).isEqualTo("h4\n\nu[image]\n");
        assertThat(ListItem.of(new Text("x")).accept(new PlainTextWriter())).isEqualTo("x");
        assertThat(TableRow.of(TableCell.of("a"), TableCell.of("b")).accept(new PlainTextWriter())).isEqualTo("a  b");
    }

    @Test
    void plainTextReadsChunksAsParagraphs() {
        assertThat(text.parser().parse("first line\nsame paragraph\n\n\n**not bold**\r\n").blocks()).containsExactly(
                Paragraph.of(new Text("first line same paragraph")), Paragraph.of(new Text("**not bold**")));
        assertThat(text.detect("anything")).isEqualTo(1);
    }

    @Test
    void latexIsACompleteDocument() {
        String output = latex.writer(WriterOptions.DEFAULT).render(TestDocuments.full());
        assertThat(output)
                .startsWith("\\documentclass{article}")
                .contains("\\section*{Title}", "\\textbf{bold}", "\\emph{italic}", "\\sout{gone}", "\\texttt{code()}",
                        "\\href{https://example.com/a?b=1}{a link}", "\\includegraphics{img/logo.png}",
                        "\\begin{itemize}", "\\begin{enumerate}", "\\begin{verbatim}", "\\begin{tabular}{|l|l|}",
                        "\\textbf{a} & 1 \\\\ \\hline", "\\begin{quote}", "\\rule")
                .endsWith("\\end{document}\n");
        assertThat(LatexWriterAccess.escape("50% of $5 & #1_a^b~{}\\")).isEqualTo(
                "50\\% of \\$5 \\& \\#1\\_a\\textasciicircum{}b\\textasciitilde{}\\{\\}\\textbackslash{}");
        assertThat(ListItem.of(new Text("x")).accept(new LatexWriter())).isEqualTo("  \\item x");
        assertThat(TableRow.of(TableCell.of("a")).accept(new LatexWriter())).isEqualTo("a \\\\ \\hline");
    }

    @Test
    void latexCannotBeRead() {
        assertThat(latex.canRead()).isFalse();
        assertThat(latex.detect("\\documentclass")).isZero();
        assertThatThrownBy(latex::parser).isInstanceOf(UnsupportedFormatException.class);
    }

    /** Package-private helper exposure for the escape test. */
    static final class LatexWriterAccess {
        static String escape(String value) {
            return new LatexWriter().visitText(new Text(value));
        }
    }
}
