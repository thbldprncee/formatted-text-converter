package io.github.thbldprncee.textformat.formats.markdown;

import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class MarkdownFormat implements TextFormat {

    private static final Pattern MARKDOWN_SIGNS = Pattern.compile(
            "(?m)^(#{1,6}\\s|[-*+]\\s|\\d+\\.\\s|>|```|\\|)|\\*\\*|__|`[^`]+`|\\[[^]]+]\\(");

    @Override
    public String id() {
        return "md";
    }

    @Override
    public String displayName() {
        return "Markdown";
    }

    @Override
    public List<String> extensions() {
        return List.of("md", "markdown");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.READ, Capability.WRITE);
    }

    @Override
    public DocumentParser parser() {
        return new MarkdownParser();
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new MarkdownWriter();
    }

    /** Fallback for anything that is not RTF, JSON or XML; stronger when Markdown syntax is present. */
    @Override
    public int detect(String content) {
        return MARKDOWN_SIGNS.matcher(content).find() ? 30 : 20;
    }
}
