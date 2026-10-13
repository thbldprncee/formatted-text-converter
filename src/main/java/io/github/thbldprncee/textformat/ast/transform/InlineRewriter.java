package io.github.thbldprncee.textformat.ast.transform;

import io.github.thbldprncee.textformat.ast.Block;
import io.github.thbldprncee.textformat.ast.BlockQuote;
import io.github.thbldprncee.textformat.ast.CodeBlock;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.Inline;
import io.github.thbldprncee.textformat.ast.InlineContainer;
import io.github.thbldprncee.textformat.ast.ListBlock;
import io.github.thbldprncee.textformat.ast.ListItem;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Table;
import io.github.thbldprncee.textformat.ast.TableCell;
import io.github.thbldprncee.textformat.ast.TableRow;
import io.github.thbldprncee.textformat.ast.ThematicBreak;

import java.util.List;

/**
 * Template Method for transformations of inline content: the block structure is copied
 * unchanged, and every inline sequence goes through {@link #rewriteInlines}. Subclasses decide
 * what one inline element becomes (possibly several elements, or none).
 */
public abstract class InlineRewriter implements DocumentTransformer {

    @Override
    public final Document apply(Document document) {
        return new Document(document.blocks().stream().map(this::rewriteBlock).toList());
    }

    /** One inline element in, zero or more out. The default keeps it and rewrites its children. */
    protected List<Inline> rewrite(Inline inline) {
        if (inline instanceof InlineContainer container) {
            return List.of((Inline) container.withChildren(rewriteInlines(container.children())));
        }
        return List.of(inline);
    }

    protected List<Inline> rewriteInlines(List<Inline> inlines) {
        return inlines.stream().flatMap(inline -> rewrite(inline).stream()).toList();
    }

    private Block rewriteBlock(Block block) {
        return switch (block) {
            case Paragraph paragraph -> paragraph.withChildren(rewriteInlines(paragraph.children()));
            case Heading heading -> heading.withChildren(rewriteInlines(heading.children()));
            case ListBlock list -> new ListBlock(list.ordered(), list.items().stream().map(this::rewriteItem).toList());
            case BlockQuote quote -> new BlockQuote(quote.blocks().stream().map(this::rewriteBlock).toList());
            case Table table -> new Table(table.rows().stream().map(this::rewriteRow).toList());
            case CodeBlock code -> code;
            case ThematicBreak thematicBreak -> thematicBreak;
        };
    }

    private ListItem rewriteItem(ListItem item) {
        return new ListItem(item.blocks().stream().map(this::rewriteBlock).toList());
    }

    private TableRow rewriteRow(TableRow row) {
        return new TableRow(row.cells().stream()
                .map(cell -> new TableCell(rewriteInlines(cell.children())))
                .toList());
    }
}
