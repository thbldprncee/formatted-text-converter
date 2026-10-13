package io.github.thbldprncee.textformat.formats.rtf;

import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.List;
import java.util.Set;

public final class RtfFormat implements TextFormat {

    @Override
    public String id() {
        return "rtf";
    }

    @Override
    public String displayName() {
        return "Rich Text Format";
    }

    @Override
    public List<String> extensions() {
        return List.of("rtf");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.READ, Capability.WRITE);
    }

    @Override
    public DocumentParser parser() {
        return new RtfParser();
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new RtfWriter();
    }

    @Override
    public int detect(String content) {
        return content.stripLeading().startsWith("{\\rtf") ? 100 : 0;
    }
}
