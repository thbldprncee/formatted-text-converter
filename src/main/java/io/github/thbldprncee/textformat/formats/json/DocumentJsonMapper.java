package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.ast.Block;
import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Image;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.InlineContainer;
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
import io.github.thbldprncee.textformat.common.TextFormatException;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonArray;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonBoolean;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNumber;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonObject;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonString;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Maps the document model to the JSON layout of the specification and back.
 * <p>
 * Layout rules, following the specification example:
 * <ul>
 *   <li>the document is a top-level array of elements;</li>
 *   <li>{@code header}, {@code bold}, {@code italic}, {@code strikethrough} and {@code link} use
 *       {@code "text"} when their content is plain text and {@code "children"} otherwise;</li>
 *   <li>a top-level paragraph that starts with a formatted element is written as that element
 *       followed by a paragraph with the rest ({@code **Bold** and *italic*} becomes
 *       {@code bold} + {@code paragraph}). The reader joins such pairs back when the rest
 *       continues the sentence, i.e. starts with whitespace or punctuation.</li>
 * </ul>
 */
@UtilityClass
class DocumentJsonMapper {

    // ---------------------------------------------------------------- model -> JSON

    static JsonArray toJson(Document document) {
        return new JsonArray(document.blocks().stream()
                .flatMap(block -> topLevel(block).stream())
                .toList());
    }

    private static List<JsonValue> topLevel(Block block) {
        if (block instanceof Paragraph paragraph
                && !paragraph.children().isEmpty()
                && !(paragraph.children().getFirst() instanceof Text)) {
            List<Inline> rest = paragraph.children().subList(1, paragraph.children().size());
            List<JsonValue> parts = new ArrayList<>();
            parts.add(inline(paragraph.children().getFirst()));
            if (!rest.isEmpty()) {
                parts.add(block(new Paragraph(rest)));
            }
            return parts;
        }
        return List.of(block(block));
    }

    private static JsonValue block(Block block) {
        return switch (block) {
            case Paragraph paragraph -> typed("paragraph").put("children", inlines(paragraph.children())).build();
            case Heading heading -> withContent(typed("header").put("level", heading.level()), heading);
            case CodeBlock code -> {
                JsonObject.Builder json = typed("code_block").put("code", code.code());
                if (code.hasLanguage()) {
                    json.put("language", code.language());
                }
                yield json.build();
            }
            case ListBlock list -> typed("list")
                    .put("ordered", list.ordered())
                    .put("children", new JsonArray(list.items().stream().map(DocumentJsonMapper::item).toList()))
                    .build();
            case Table table -> typed("table")
                    .put("children", new JsonArray(table.rows().stream().map(DocumentJsonMapper::row).toList()))
                    .build();
            case BlockQuote quote -> typed("blockquote").put("children", blocks(quote.blocks())).build();
            case ThematicBreak ignored -> typed("horizontal_rule").build();
        };
    }

    private static JsonValue item(ListItem item) {
        return typed("list_item").put("children", blocks(item.blocks())).build();
    }

    private static JsonValue row(TableRow row) {
        return typed("table_row")
                .put("children", new JsonArray(row.cells().stream().map(DocumentJsonMapper::cell).toList()))
                .build();
    }

    private static JsonValue cell(TableCell cell) {
        return withContent(typed("table_cell"), cell);
    }

    private static JsonValue inline(Inline inline) {
        return switch (inline) {
            case Text text -> typed("text").put("content", text.value()).build();
            case Bold bold -> withContent(typed("bold"), bold);
            case Italic italic -> withContent(typed("italic"), italic);
            case Strikethrough strike -> withContent(typed("strikethrough"), strike);
            case InlineCode code -> typed("code_inline").put("code", code.code()).build();
            case Link link -> withContent(typed("link").put("url", link.url()), link);
            case Image image -> typed("image").put("url", image.url()).put("alt", image.alt()).build();
        };
    }

    private static JsonObject withContent(JsonObject.Builder json, InlineContainer container) {
        if (container.isPlainText()) {
            return json.put("text", container.children().stream()
                    .map(child -> ((Text) child).value())
                    .collect(Collectors.joining())).build();
        }
        return json.put("children", inlines(container.children())).build();
    }

    private static JsonArray inlines(List<Inline> inlines) {
        return new JsonArray(inlines.stream().map(DocumentJsonMapper::inline).toList());
    }

    private static JsonArray blocks(List<Block> blocks) {
        return new JsonArray(blocks.stream().map(DocumentJsonMapper::block).toList());
    }

    private static JsonObject.Builder typed(String type) {
        return JsonObject.builder().put("type", type);
    }

    // ---------------------------------------------------------------- JSON -> model

    static Document fromJson(JsonValue root) {
        List<JsonValue> elements = switch (root) {
            case JsonArray array -> array.elements();
            case JsonObject object when object.get("type").map(DocumentJsonMapper::str).orElse("").equals("document") ->
                    array(object, "children", "$").elements();
            case JsonObject object -> List.of(object);
            default -> throw invalid("$", "the document must be an array of elements, got " + root.typeName());
        };
        return new Document(topLevelBlocks(elements, "$"));
    }

    /** Re-joins "inline element + continuing paragraph" pairs written by {@link #topLevel}. */
    private static List<Block> topLevelBlocks(List<JsonValue> elements, String path) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            String elementPath = path + "[" + i + "]";
            Node node = node(elements.get(i), elementPath);
            if (node instanceof InlineNode(Inline inline)) {
                List<Inline> content = new ArrayList<>(List.of(inline));
                if (i + 1 < elements.size()
                        && node(elements.get(i + 1), path + "[" + (i + 1) + "]") instanceof BlockNode(Paragraph next)
                        && continuesSentence(next)) {
                    content.addAll(next.children());
                    i++;
                }
                blocks.add(new Paragraph(content));
            } else {
                blocks.add(((BlockNode) node).block());
            }
        }
        return blocks;
    }

    private static boolean continuesSentence(Paragraph paragraph) {
        if (paragraph.children().isEmpty() || !(paragraph.children().getFirst() instanceof Text text)
                || text.value().isEmpty()) {
            return false;
        }
        char first = text.value().charAt(0);
        return Character.isWhitespace(first) || ".,;:!?)»…".indexOf(first) >= 0;
    }

    /** An element is either block-level or inline; known only after reading its type. */
    private sealed interface Node permits BlockNode, InlineNode {
    }

    private record BlockNode(Block block) implements Node {
    }

    private record InlineNode(Inline inline) implements Node {
    }

    private static Node node(JsonValue value, String path) {
        JsonObject object = object(value, path);
        String type = string(object, "type", path);
        return switch (type) {
            case "paragraph" -> new BlockNode(new Paragraph(inlineChildren(object, path)));
            case "header", "heading" -> new BlockNode(new Heading(level(object, path), content(object, path)));
            case "code_block" -> new BlockNode(new CodeBlock(string(object, "code", path),
                    object.get("language").map(DocumentJsonMapper::str).orElse("")));
            case "list" -> new BlockNode(new ListBlock(ordered(object), children(object, path).stream()
                    .map(child -> listItem(child.value(), child.path()))
                    .toList()));
            case "table" -> new BlockNode(new Table(children(object, path).stream()
                    .map(child -> tableRow(child.value(), child.path()))
                    .toList()));
            case "blockquote" -> new BlockNode(new BlockQuote(blockChildren(object, path)));
            case "horizontal_rule", "thematic_break" -> new BlockNode(new ThematicBreak());
            case "text" -> new InlineNode(new Text(string(object, "content", path)));
            case "bold" -> new InlineNode(new Bold(content(object, path)));
            case "italic" -> new InlineNode(new Italic(content(object, path)));
            case "strikethrough" -> new InlineNode(new Strikethrough(content(object, path)));
            case "code_inline" -> new InlineNode(new InlineCode(string(object, "code", path)));
            case "link" -> new InlineNode(new Link(string(object, "url", path), content(object, path)));
            case "image" -> new InlineNode(new Image(string(object, "url", path),
                    object.get("alt").map(DocumentJsonMapper::str).orElse("")));
            default -> throw invalid(path, "unknown element type '" + type + "'");
        };
    }

    private static ListItem listItem(JsonValue value, String path) {
        JsonObject object = object(value, path);
        requireType(object, "list_item", path);
        return new ListItem(blockChildren(object, path));
    }

    private static TableRow tableRow(JsonValue value, String path) {
        JsonObject object = object(value, path);
        requireType(object, "table_row", path);
        return new TableRow(children(object, path).stream().map(child -> {
            JsonObject cell = object(child.value(), child.path());
            requireType(cell, "table_cell", child.path());
            return new TableCell(content(cell, child.path()));
        }).toList());
    }

    /** Block children; bare inline children (e.g. a list item holding text) form a paragraph. */
    private static List<Block> blockChildren(JsonObject object, String path) {
        List<Block> blocks = new ArrayList<>();
        List<Inline> pending = new ArrayList<>();
        for (Child child : children(object, path)) {
            switch (node(child.value(), child.path())) {
                case InlineNode(Inline inline) -> pending.add(inline);
                case BlockNode(Block block) -> {
                    if (!pending.isEmpty()) {
                        blocks.add(new Paragraph(List.copyOf(pending)));
                        pending.clear();
                    }
                    blocks.add(block);
                }
            }
        }
        if (!pending.isEmpty()) {
            blocks.add(new Paragraph(pending));
        }
        return blocks;
    }

    private static List<Inline> inlineChildren(JsonObject object, String path) {
        return children(object, path).stream().map(child -> switch (node(child.value(), child.path())) {
            case InlineNode(Inline inline) -> inline;
            case BlockNode ignored -> throw invalid(child.path(), "block element inside inline content");
        }).toList();
    }

    /** {@code "text": "..."} or {@code "children": [...]}. */
    private static List<Inline> content(JsonObject object, String path) {
        if (object.get("text").isPresent()) {
            String text = string(object, "text", path);
            return text.isEmpty() ? List.of() : List.of(new Text(text));
        }
        return object.get("children").isPresent() ? inlineChildren(object, path) : List.of();
    }

    private record Child(JsonValue value, String path) {
    }

    private static List<Child> children(JsonObject object, String path) {
        if (object.get("children").isEmpty()) {
            return List.of();
        }
        List<JsonValue> elements = array(object, "children", path).elements();
        return IntStream.range(0, elements.size())
                .mapToObj(i -> new Child(elements.get(i), path + ".children[" + i + "]"))
                .toList();
    }

    private static int level(JsonObject object, String path) {
        return object.get("level").map(value -> switch (value) {
            case JsonNumber number -> Heading.clampLevel(number.value().intValue());
            case JsonString string when string.value().strip().matches("\\d+") ->
                    Heading.clampLevel(Integer.parseInt(string.value().strip()));
            default -> throw invalid(path + ".level", "expected a number, got " + value.typeName());
        }).orElse(1);
    }

    private static boolean ordered(JsonObject object) {
        return object.get("ordered").map(value -> switch (value) {
            case JsonBoolean bool -> bool.value();
            case JsonString string -> Boolean.parseBoolean(string.value());
            default -> false;
        }).orElse(false);
    }

    private static void requireType(JsonObject object, String expected, String path) {
        String type = string(object, "type", path);
        if (!type.equals(expected)) {
            throw invalid(path, "expected '%s' but found '%s'".formatted(expected, type));
        }
    }

    private static JsonObject object(JsonValue value, String path) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw invalid(path, "expected an object, got " + value.typeName());
    }

    private static JsonArray array(JsonObject object, String key, String path) {
        JsonValue value = object.get(key).orElseThrow(() -> invalid(path, "missing \"" + key + "\""));
        if (value instanceof JsonArray array) {
            return array;
        }
        throw invalid(path + "." + key, "expected an array, got " + value.typeName());
    }

    private static String string(JsonObject object, String key, String path) {
        JsonValue value = object.get(key).orElseThrow(() -> invalid(path, "missing \"" + key + "\""));
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw invalid(path + "." + key, "expected a string, got " + value.typeName());
    }

    private static String str(JsonValue value) {
        return value instanceof JsonString string ? string.value() : "";
    }

    private static TextFormatException invalid(String path, String message) {
        return new TextFormatException("Invalid JSON document at " + path + ": " + message);
    }
}
