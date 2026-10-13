package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.format.AbstractStructuredWriter;
import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class JsonFormat implements TextFormat {

    @Override
    public String id() {
        return "json";
    }

    @Override
    public String displayName() {
        return "JSON";
    }

    @Override
    public List<String> extensions() {
        return List.of("json");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.READ, Capability.WRITE);
    }

    @Override
    public DocumentParser parser() {
        return content -> DocumentJsonMapper.fromJson(JsonReader.parse(content));
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new AbstractStructuredWriter(options) {
            @Override
            public void write(io.github.thbldprncee.textformat.ast.Document document, Appendable out) throws IOException {
                JsonSerializer.write(DocumentJsonMapper.toJson(document), out, indent());
            }
        };
    }

    @Override
    public int detect(String content) {
        String start = content.stripLeading();
        if (start.startsWith("{\\rtf")) {
            return 0;
        }
        return start.startsWith("[") || start.startsWith("{") ? 90 : 0;
    }
}
