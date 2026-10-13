package io.github.thbldprncee.textformat.ast;

import java.util.List;

/** The first row is the header row. */
public record Table(List<TableRow> rows) implements Block {

    public Table {
        rows = List.copyOf(rows);
    }

    public static Table of(TableRow... rows) {
        return new Table(List.of(rows));
    }

    public int columnCount() {
        return rows.stream().mapToInt(row -> row.cells().size()).max().orElse(0);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitTable(this);
    }
}
