package io.github.thbldprncee.textformat.formats.xhtml;

import io.github.thbldprncee.textformat.ast.Block;
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
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.formats.xhtml.XmlNode.Element;
import io.github.thbldprncee.textformat.formats.xhtml.XmlNode.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps (X)HTML to the model. Everything Markdown can express is recognised; unknown containers
 * ({@code div}, {@code span}, ...) are transparent and non-content elements ({@code head},
 * {@code script}, {@code style}) are skipped. Whitespace in text is collapsed like browsers do.
 */
public final class XhtmlParser implements DocumentParser {

    private static final Set<String> SKIPPED = Set.of("head", "script", "style", "title", "template");
    private static final Set<String> INLINE = Set.of("b", "strong", "i", "em", "s", "del", "strike", "code", "kbd",
            "samp", "tt", "a", "img", "br", "span", "u", "small", "sub", "sup", "mark", "abbr", "cite", "q", "label");

    @Override
    public Document parse(String content) {
        return new Document(blocks(XmlReader.parse(content).children()));
    }

    private List<Block> blocks(List<XmlNode> nodes) {
        List<Block> blocks = new ArrayList<>();
        List<XmlNode> pendingInline = new ArrayList<>();
        for (XmlNode node : nodes) {
            if (isInline(node)) {
                pendingInline.add(node);
                continue;
            }
            flushParagraph(pendingInline, blocks);
            if (node instanceof Element element) {
                blocks.addAll(block(element));
            }
        }
        flushParagraph(pendingInline, blocks);
        return blocks;
    }

    private static boolean isInline(XmlNode node) {
        return switch (node) {
            case TextNode ignored -> true;
            case Element element -> INLINE.contains(element.name());
        };
    }

    /** Loose inline content between blocks (e.g. text directly inside {@code <li>}) becomes a paragraph. */
    private void flushParagraph(List<XmlNode> inline, List<Block> blocks) {
        List<Inline> content = trimmed(inlines(inline));
        if (!content.isEmpty()) {
            blocks.add(new Paragraph(content));
        }
        inline.clear();
    }

    private List<Block> block(Element element) {
        return switch (element.name()) {
            case "p" -> List.of(new Paragraph(trimmed(inlines(element.children()))));
            case "h1", "h2", "h3", "h4", "h5", "h6" ->
                    List.of(new Heading(element.name().charAt(1) - '0', trimmed(inlines(element.children()))));
            case "pre" -> List.of(codeBlock(element));
            case "ul", "ol" -> List.of(new ListBlock(element.name().equals("ol"), childElements(element, "li")
                    .map(li -> new ListItem(blocks(li.children())))
                    .toList()));
            case "blockquote" -> List.of(new BlockQuote(blocks(element.children())));
            case "hr" -> List.of(new ThematicBreak());
            case "table" -> List.of(new Table(rows(element)));
            default -> SKIPPED.contains(element.name()) ? List.of() : blocks(element.children());
        };
    }

    private CodeBlock codeBlock(Element pre) {
        List<Element> codes = childElements(pre, "code").toList();
        String language = codes.isEmpty() ? "" : codes.getFirst().attribute("class")
                .flatMap(classes -> Stream.of(classes.split("\\s+"))
                        .filter(name -> name.startsWith("language-"))
                        .map(name -> name.substring("language-".length()))
                        .findFirst())
                .orElse("");
        String code = rawText(pre);
        if (code.startsWith("\n")) {
            code = code.substring(1);
        }
        return new CodeBlock(code.endsWith("\n") ? code.substring(0, code.length() - 1) : code, language);
    }

    private List<TableRow> rows(Element table) {
        return table.children().stream()
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .flatMap(child -> switch (child.name()) {
                    case "tr" -> Stream.of(child);
                    case "thead", "tbody", "tfoot" -> childElements(child, "tr");
                    default -> Stream.empty();
                })
                .map(tr -> new TableRow(tr.children().stream()
                        .filter(Element.class::isInstance)
                        .map(Element.class::cast)
                        .filter(cell -> cell.name().equals("td") || cell.name().equals("th"))
                        .map(cell -> new TableCell(trimmed(inlines(cell.children()))))
                        .toList()))
                .toList();
    }

    private List<Inline> inlines(List<XmlNode> nodes) {
        return nodes.stream().flatMap(node -> inline(node).stream()).toList();
    }

    private List<Inline> inline(XmlNode node) {
        return switch (node) {
            case TextNode text -> List.of(new Text(text.text().replaceAll("\\s+", " ")));
            case Element element -> switch (element.name()) {
                case "b", "strong" -> List.of(new Bold(inlines(element.children())));
                case "i", "em" -> List.of(new Italic(inlines(element.children())));
                case "s", "del", "strike" -> List.of(new Strikethrough(inlines(element.children())));
                case "code", "kbd", "samp", "tt" -> List.of(new InlineCode(rawText(element)));
                case "a" -> element.attribute("href")
                        .<List<Inline>>map(href -> List.of(new Link(href, inlines(element.children()))))
                        .orElseGet(() -> inlines(element.children()));
                case "img" -> List.of(new Image(element.attribute("src").orElse(""), element.attribute("alt").orElse("")));
                case "br" -> List.of(new Text(" "));
                default -> SKIPPED.contains(element.name()) ? List.of() : inlines(element.children());
            };
        };
    }

    /** Merges adjacent text, collapses double spaces at joins and trims the edges of a block. */
    private static List<Inline> trimmed(List<Inline> inlines) {
        List<Inline> merged = new ArrayList<>();
        for (Inline inline : inlines) {
            if (inline instanceof Text text && !merged.isEmpty() && merged.getLast() instanceof Text previous) {
                merged.set(merged.size() - 1, new Text((previous.value() + text.value()).replaceAll(" {2,}", " ")));
            } else {
                merged.add(inline);
            }
        }
        if (!merged.isEmpty() && merged.getFirst() instanceof Text first) {
            merged.set(0, new Text(first.value().stripLeading()));
        }
        if (!merged.isEmpty() && merged.getLast() instanceof Text last) {
            merged.set(merged.size() - 1, new Text(last.value().stripTrailing()));
        }
        return merged.stream().filter(inline -> !(inline instanceof Text text && text.value().isEmpty())).toList();
    }

    private static Stream<Element> childElements(Element parent, String name) {
        return parent.children().stream()
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(child -> child.name().equals(name));
    }

    private static String rawText(XmlNode node) {
        return switch (node) {
            case TextNode text -> text.text();
            case Element element -> element.children().stream().map(XhtmlParser::rawText).collect(Collectors.joining());
        };
    }
}
