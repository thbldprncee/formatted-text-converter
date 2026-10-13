package io.github.thbldprncee.textformat.formats.xhtml;

import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.List;
import java.util.Set;

public final class XhtmlFormat implements TextFormat {

    @Override
    public String id() {
        return "xhtml";
    }

    @Override
    public String displayName() {
        return "XHTML";
    }

    @Override
    public List<String> extensions() {
        return List.of("xhtml", "html", "htm", "xml");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.READ, Capability.WRITE);
    }

    @Override
    public DocumentParser parser() {
        return new XhtmlParser();
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new XhtmlWriter(options);
    }

    @Override
    public int detect(String content) {
        String start = content.stripLeading();
        if (start.startsWith("<?xml") || start.regionMatches(true, 0, "<!DOCTYPE", 0, 9)
                || start.regionMatches(true, 0, "<html", 0, 5)) {
            return 95;
        }
        // "<https://...>" is a Markdown autolink, not markup.
        boolean markup = start.matches("(?s)<[A-Za-z][\\w:-]*[\\s/>].*")
                && (content.contains("</") || content.contains("/>"));
        return markup ? 80 : 0;
    }
}
