package io.github.thbldprncee.textformat.ast;

import java.util.List;

public record TableRow(List<TableCell> cells) implements Node {

    public TableRow {
        cells = List.copyOf(cells);
    }

    public static TableRow of(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitTableRow(this);
    }
}
