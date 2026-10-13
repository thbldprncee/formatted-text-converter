package io.github.thbldprncee.textformat.formats.latex;

import io.github.thbldprncee.textformat.format.Capability;
import io.github.thbldprncee.textformat.format.DocumentWriter;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;

import java.util.List;
import java.util.Set;

/** Export only: LaTeX is not read back. */
public final class LatexFormat implements TextFormat {

    @Override
    public String id() {
        return "latex";
    }

    @Override
    public String displayName() {
        return "LaTeX";
    }

    @Override
    public List<String> extensions() {
        return List.of("tex", "latex");
    }

    @Override
    public Set<Capability> capabilities() {
        return Set.of(Capability.WRITE);
    }

    @Override
    public DocumentWriter writer(WriterOptions options) {
        return new LatexWriter();
    }
}
