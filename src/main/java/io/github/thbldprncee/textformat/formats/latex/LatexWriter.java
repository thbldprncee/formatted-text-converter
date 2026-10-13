package io.github.thbldprncee.textformat.formats.latex;

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
import io.github.thbldprncee.textformat.format.AbstractTextWriter;

import java.util.Map;
import java.util.stream.Collectors;

/** A complete, compilable LaTeX document (UTF-8, Cyrillic-ready via babel). */
public final class LatexWriter extends AbstractTextWriter {

    private static final String[] SECTIONS = {"section", "subsection", "subsubsection", "paragraph", "subparagraph", "subparagraph"};
    private static final Map<Character, String> SPECIAL = Map.ofEntries(
            Map.entry('\\', "\\textbackslash{}"), Map.entry('{', "\\{"), Map.entry('}', "\\}"),
            Map.entry('$', "\\$"), Map.entry('&', "\\&"), Map.entry('#', "\\#"), Map.entry('%', "\\%"),
            Map.entry('_', "\\_"), Map.entry('^', "\\textasciicircum{}"), Map.entry('~', "\\textasciitilde{}"));

    @Override
    public String visitDocument(Document document) {
        return """
                \\documentclass{article}
                \\usepackage[T2A]{fontenc}
                \\usepackage[utf8]{inputenc}
                \\usepackage[english,russian]{babel}
                \\usepackage[normalem]{ulem}
                \\usepackage{hyperref}
                \\usepackage{graphicx}

                \\begin{document}

                %s
                \\end{document}
                """.formatted(blocks(document.blocks()) + (document.isEmpty() ? "" : "\n"));
    }

    @Override
    public String visitParagraph(Paragraph paragraph) {
        return inlines(paragraph.children());
    }

    @Override
    public String visitHeading(Heading heading) {
        return "\\" + SECTIONS[heading.level() - 1] + "*{" + inlines(heading.children()) + "}";
    }

    @Override
    public String visitCodeBlock(CodeBlock code) {
        return "\\begin{verbatim}\n" + code.code() + "\n\\end{verbatim}";
    }

    @Override
    public String visitList(ListBlock list) {
        String environment = list.ordered() ? "enumerate" : "itemize";
        return list.items().stream()
                .map(item -> item.accept(this))
                .collect(Collectors.joining("\n", "\\begin{" + environment + "}\n", "\n\\end{" + environment + "}"));
    }

    @Override
    public String visitListItem(ListItem item) {
        return "  \\item " + item.blocks().stream()
                .map(block -> block.accept(this))
                .collect(Collectors.joining("\n\n")).replace("\n", "\n  ");
    }

    @Override
    public String visitTable(Table table) {
        String columns = "|" + "l|".repeat(Math.max(1, table.columnCount()));
        return table.rows().stream()
                .map(row -> row.accept(this))
                .collect(Collectors.joining("\n", "\\begin{tabular}{" + columns + "}\n\\hline\n", "\n\\end{tabular}"));
    }

    @Override
    public String visitTableRow(TableRow row) {
        return row.cells().stream().map(cell -> cell.accept(this)).collect(Collectors.joining(" & ")) + " \\\\ \\hline";
    }

    @Override
    public String visitTableCell(TableCell cell) {
        return inlines(cell.children());
    }

    @Override
    public String visitBlockQuote(BlockQuote quote) {
        return "\\begin{quote}\n" + blocks(quote.blocks()) + "\n\\end{quote}";
    }

    @Override
    public String visitThematicBreak(ThematicBreak thematicBreak) {
        return "\\noindent\\rule{\\linewidth}{0.4pt}";
    }

    @Override
    public String visitText(Text text) {
        return escape(text.value());
    }

    @Override
    public String visitBold(Bold bold) {
        return "\\textbf{" + inlines(bold.children()) + "}";
    }

    @Override
    public String visitItalic(Italic italic) {
        return "\\emph{" + inlines(italic.children()) + "}";
    }

    @Override
    public String visitStrikethrough(Strikethrough strikethrough) {
        return "\\sout{" + inlines(strikethrough.children()) + "}";
    }

    @Override
    public String visitInlineCode(InlineCode code) {
        return "\\texttt{" + escape(code.code()) + "}";
    }

    @Override
    public String visitLink(Link link) {
        return "\\href{" + link.url().replace("%", "\\%").replace("#", "\\#") + "}{" + inlines(link.children()) + "}";
    }

    @Override
    public String visitImage(Image image) {
        return "\\includegraphics{" + image.url() + "}";
    }

    static String escape(String text) {
        return text.chars()
                .mapToObj(ch -> SPECIAL.getOrDefault((char) ch, String.valueOf((char) ch)))
                .collect(Collectors.joining());
    }
}
