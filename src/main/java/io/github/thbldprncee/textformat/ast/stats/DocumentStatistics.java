package io.github.thbldprncee.textformat.ast.stats;

import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Node;
import io.github.thbldprncee.textformat.ast.Nodes;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Summary of a document, computed with the Stream API over all nodes.
 *
 * @param nodeCounts number of nodes of each type, by simple class name
 */
public record DocumentStatistics(long words, long characters, long charactersWithoutSpaces,
                                 Map<String, Long> nodeCounts) {

    private static final int WORDS_PER_MINUTE = 200;

    public DocumentStatistics {
        nodeCounts = Map.copyOf(nodeCounts);
    }

    public static DocumentStatistics of(Document document) {
        String text = Nodes.plainText(document);
        long words = Arrays.stream(text.split("\\s+")).filter(word -> !word.isBlank()).count();
        long withoutSpaces = text.codePoints().filter(cp -> !Character.isWhitespace(cp)).count();
        Map<String, Long> counts = Nodes.descendantsAndSelf(document)
                .filter(node -> !(node instanceof Document))
                .map(Node::getClass)
                .collect(Collectors.groupingBy(Class::getSimpleName, TreeMap::new, Collectors.counting()));
        return new DocumentStatistics(words, text.codePoints().count(), withoutSpaces, counts);
    }

    public long count(Class<? extends Node> type) {
        return nodeCounts.getOrDefault(type.getSimpleName(), 0L);
    }

    public long readingMinutes() {
        return Math.max(1, (words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE);
    }

    public String report() {
        String nodes = new TreeMap<>(nodeCounts).entrySet().stream()
                .map(entry -> "  %-14s %d".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        return """
                Words:                 %d
                Characters:            %d
                Characters (no space): %d
                Reading time:          ~%d min
                Elements:
                %s
                """.formatted(words, characters, charactersWithoutSpaces, readingMinutes(), nodes);
    }
}
