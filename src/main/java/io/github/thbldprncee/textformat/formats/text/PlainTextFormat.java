package io.github.thbldprncee.textformat.formats.text;

import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Text;
import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentParser;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Plain text: each blank-line separated chunk is a paragraph; no markup is interpreted. */
public final class PlainTextFormat implements TextFormat {

    @Override
    public String id() {
        return "txt";
    }

    @Override
    public String displayName() {
        return "Plain text";
    }

    @Override
    public List<String> extensions() {
        return List.of("txt", "text");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.READ, Capability.WRITE);
    }

    @Override
    public DocumentParser parser() {
        return content -> new Document(Arrays.stream(content.replace("\r\n", "\n").split("\\n\\s*\\n"))
                .map(chunk -> chunk.strip().replaceAll("\\s*\\n\\s*", " "))
                .filter(chunk -> !chunk.isEmpty())
                .map(chunk -> (io.github.thbldprncee.textformat.ast.Block) Paragraph.of(new Text(chunk)))
                .toList());
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new PlainTextWriter();
    }

    /** Never preferred automatically: use {@code --from txt} to read text literally. */
    @Override
    public int detect(String content) {
        return 1;
    }
}
