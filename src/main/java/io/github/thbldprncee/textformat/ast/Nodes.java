package io.github.thbldprncee.textformat.ast;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Read-only queries over the model, built on pattern matching and streams. */
@UtilityClass
public class Nodes {

    /** Concatenated visible text of a node, without any markup. */
    public static String plainText(Node node) {
        return switch (node) {
            case Text text -> text.value();
            case InlineCode code -> code.code();
            case Image image -> image.alt();
            case CodeBlock block -> block.code();
            case ThematicBreak ignored -> "";
            case InlineContainer container -> join(container.children().stream(), "");
            case Document document -> join(document.blocks().stream(), "\n");
            case ListBlock list -> join(list.items().stream(), "\n");
            case ListItem item -> join(item.blocks().stream(), "\n");
            case BlockQuote quote -> join(quote.blocks().stream(), "\n");
            case Table table -> join(table.rows().stream(), "\n");
            case TableRow row -> join(row.cells().stream(), " ");
        };
    }

    /** Every node of the tree in depth-first pre-order, as a stream. */
    public static Stream<Node> descendantsAndSelf(Node node) {
        return Stream.concat(Stream.of(node), children(node).stream().flatMap(Nodes::descendantsAndSelf));
    }

    public static List<? extends Node> children(Node node) {
        return switch (node) {
            case Document document -> document.blocks();
            case InlineContainer container -> container.children();
            case ListBlock list -> list.items();
            case ListItem item -> item.blocks();
            case BlockQuote quote -> quote.blocks();
            case Table table -> table.rows();
            case TableRow row -> row.cells();
            case Text ignored -> List.of();
            case InlineCode ignored -> List.of();
            case Image ignored -> List.of();
            case CodeBlock ignored -> List.of();
            case ThematicBreak ignored -> List.of();
        };
    }

    private static String join(Stream<? extends Node> nodes, String separator) {
        return nodes.map(Nodes::plainText).collect(Collectors.joining(separator));
    }
}
