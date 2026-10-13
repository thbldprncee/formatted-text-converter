<div align="center">

# Formatted Text Converter

### One document model, six text formats

Read Markdown, XHTML, JSON, RTF or plain text into a single immutable AST,<br/>
transform it, and write it back out in any supported format, LaTeX included.

<br/>

[![CI](https://github.com/thbldprncee/formatted-text-converter/actions/workflows/ci.yml/badge.svg)](https://github.com/thbldprncee/formatted-text-converter/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.10-02303A?style=flat-square&logo=gradle&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?style=flat-square&logo=junit5&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-1.18-BC4521?style=flat-square)
![Coverage](https://img.shields.io/badge/coverage-96%25-brightgreen?style=flat-square)
![Dependencies](https://img.shields.io/badge/runtime%20dependencies-0-blue?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)

</div>

---

## Overview

Every format has its own syntax for the same ideas: a heading is `# Title` in Markdown, `<h1>` in XHTML, `{"type": "header"}` in JSON and `\outlinelevel0` in RTF. **Formatted Text Converter** parses each format into one format-independent **abstract syntax tree**, so any input can be converted to any output, and operations such as *remove all styles* are written once instead of once per format.

```bash
textconv in.md out.json        # the output extension picks the format
textconv report.rtf report.html --indent=tab --strip-styles
```

The input format is **recognised by content**, not by file extension: a file called `notes.data` that starts with `{\rtf` is read as RTF.

All parsers are hand-written: JSON, XML and RTF are read with recursive-descent parsers that report errors with line and column. There are no runtime dependencies.

---

## Key Features

- **Six formats.** Markdown, XHTML, JSON and RTF are read and written; plain text is read and written; LaTeX is written.
- **Lossless round trips.** Markdown → JSON / XHTML / RTF → Markdown reproduces the source byte for byte; every format is tested with `write → parse → equals`.
- **Spec-exact JSON.** The assignment's example input converts to its expected output character for character.
- **Nested lists and tables.** Supported in every format, including real RTF tables (`\trowd … \cell … \row`).
- **Style removal.** Strips bold, italic and strikethrough while keeping text, links, code and structure.
- **Configurable indentation.** Nested XHTML and JSON can use any number of spaces or tabs.
- **Content sniffing.** Each format reports a confidence score and the most confident one wins; `--from` overrides it.
- **Document statistics.** Words, characters, reading time and a count of every element, computed with the Stream API.
- **Unicode everywhere.** Cyrillic, typographic quotes and emoji survive RTF too, via `\uN?` escapes with an ANSI fallback.
- **Plugin formats.** Formats are discovered with `ServiceLoader`, so adding one requires no changes to existing code.

---

## Supported Formats

| Format | Id | Extensions | Read | Write | What is supported |
|--------|----|------------|:----:|:-----:|-------------------|
| **Markdown** | `md` | `.md` `.markdown` | ✅ | ✅ | ATX and setext headings, emphasis (`*` `_` `**` `__` `***`), `~~strike~~`, code spans of any length, fenced code with a language, nested lists, GFM tables, quotes, rules, links, images, autolinks, escapes |
| **XHTML** | `xhtml` | `.xhtml` `.html` `.htm` `.xml` | ✅ | ✅ | Everything Markdown can express; entities, CDATA, comments, `<thead>`/`<tbody>`, `language-*` classes; unknown containers are transparent |
| **JSON** | `json` | `.json` | ✅ | ✅ | The specification layout, error messages with JSON paths (`$[3].children[1]`) |
| **Rich Text Format** | `rtf` | `.rtf` | ✅ | ✅ | ANSI charset, code pages, `\uN`, `\'hh`, outline levels, monospace fonts, HYPERLINK and INCLUDEPICTURE fields, tables, bullets |
| **Plain text** | `txt` | `.txt` `.text` | ✅ | ✅ | Readable output: underlined headings, aligned tables, link targets in brackets |
| **LaTeX** | `latex` | `.tex` `.latex` | — | ✅ | A complete compilable `article` with Cyrillic support |

---

## Quick Start

### Prerequisites

- JDK 21 (only Java is needed: the Gradle wrapper downloads Gradle itself)

### Build and run

```bash
git clone https://github.com/thbldprncee/formatted-text-converter.git
cd formatted-text-converter
./gradlew build                                  # compile, 154 tests, coverage check

java -jar build/libs/formatted-text-converter-1.0.0.jar examples/showcase.md out.json
# or
./gradlew installDist && build/install/textconv/bin/textconv examples/showcase.md out.html
```

### Command line

```
Usage: textconv <in_file> <out_file> [options]

Options:
  --indent=<n|tab>  indentation of nested elements in XHTML/JSON (default: 2)
  --strip-styles    remove bold, italic and strikethrough
  --from=<format>   read the input as this format instead of detecting it
  --stats           print document statistics
  --list-formats    list supported formats
  -h, --help        show this help
```

| Exit code | Meaning |
|:---------:|---------|
| `0` | Converted |
| `1` | Wrong arguments: usage is printed |
| `2` | Input cannot be read, parsed or written: the reason is printed |

```text
$ textconv examples/showcase.rtf back.md --stats
examples/showcase.rtf (Rich Text Format) -> back.md (Markdown)
Words:                 56
Characters:            421
Characters (no space): 365
Reading time:          ~1 min
Elements:
  BlockQuote     2
  Bold           1
  CodeBlock      1
  Heading        4
  ...
```

---

## Example

The example from the assignment, [`examples/task-example.md`](examples/task-example.md):

```markdown
# Это тестовый файл, который показывает что парсер может

## Заголовки и форматирование

**Жирный текст** и *курсивный текст*.

`Инлайн-код:` `std::string s = "hello";`
```

`textconv in.md out.json` produces exactly the expected [`out.json`](examples/task-example.json):

```json
[
  {
    "type": "header",
    "level": 1,
    "text": "Это тестовый файл, который показывает что парсер может"
  },
  {
    "type": "header",
    "level": 2,
    "text": "Заголовки и форматирование"
  },
  {
    "type": "bold",
    "text": "Жирный текст"
  },
  {
    "type": "paragraph",
    "children": [
      { "type": "text", "content": " и " },
      { "type": "italic", "text": "курсивный текст" },
      { "type": "text", "content": "." }
    ]
  },
  ...
]
```

[`examples/showcase.md`](examples/showcase.md) uses every element; its conversions to [JSON](examples/showcase.json), [XHTML](examples/showcase.xhtml), [RTF](examples/showcase.rtf), [plain text](examples/showcase.txt) and [LaTeX](examples/showcase.tex) are in the same folder.

---

## Architecture

```
                 ┌──────────────────────────── cli.Main ─────────────────────────────┐
                 │      textconv <in> <out> [--indent] [--strip-styles] [--from]      │
                 └─────────────────────────────────┬─────────────────────────────────┘
                                                   │
                 ┌─────────────────────────── api.Converter ─────────────────────────┐
                 │   facade: detect format → parse → transform → write               │
                 └──────┬──────────────────────────┬──────────────────────────┬──────┘
                        │                          │                          │
            ┌───────────▼──────────┐   ┌───────────▼───────────┐  ┌───────────▼──────────┐
            │    FormatRegistry    │   │  DocumentTransformer  │  │     TextFormat       │
            │ ServiceLoader lookup │   │  StyleRemover ∘ ...   │  │ parser() / writer()  │
            │ by id / ext / content│   └───────────┬───────────┘  └───────────┬──────────┘
            └──────────────────────┘               │                          │
                                       ┌───────────▼──────────────────────────▼──────────┐
       Markdown ─┐                     │              Document  (sealed AST)              │                ┌─ Markdown
       XHTML    ─┤   DocumentParser    │  Block:  Paragraph Heading CodeBlock ListBlock   │ DocumentWriter ├─ XHTML
       JSON     ─┼───────────────────► │          Table BlockQuote ThematicBreak          ├───────────────►├─ JSON
       RTF      ─┤                     │  Inline: Text Bold Italic Strikethrough          │  (Visitor)     ├─ RTF
       Text     ─┘                     │          InlineCode Link Image                   │                ├─ Text
                                       └──────────────────────────────────────────────────┘                └─ LaTeX
```

The model is a **sealed hierarchy of records**: nodes are immutable values with structural `equals`, which is what makes round-trip tests one line long (`assertThat(parse(write(doc))).isEqualTo(doc)`), and `switch` over a node is checked for exhaustiveness by the compiler.

```
Node ─┬─ Document(blocks)
      ├─ Block ─┬─ Paragraph(children)          ListItem(blocks)
      │         ├─ Heading(level, children)     TableRow(cells)
      │         ├─ CodeBlock(code, language)    TableCell(children)
      │         ├─ ListBlock(ordered, items)
      │         ├─ Table(rows)
      │         ├─ BlockQuote(blocks)
      │         └─ ThematicBreak
      └─ Inline ┬─ Text(value)
                ├─ Style ─┬─ Bold │ Italic │ Strikethrough   ← removed by StyleRemover
                ├─ InlineCode(code)
                ├─ Link(url, children)
                └─ Image(url, alt)
```

### Shared functionality through inheritance

| Base class | Subclasses | What they share |
|------------|------------|-----------------|
| `AbstractTextWriter` | Markdown, RTF, plain text, LaTeX writers | Visitor-based rendering, block separation, inline concatenation |
| `AbstractStructuredWriter` | XHTML, JSON writers | Configurable `IndentStyle` for nested elements |
| `InlineRewriter` | `StyleRemover`, `TextMerger` | Copying the block structure while rewriting inline content |

---

## Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Composite** | `ast` — `Node` and its sealed subtypes | Blocks contain blocks, inlines contain inlines; one uniform tree |
| **Visitor** | `NodeVisitor<R>`, every writer | Rendering is added per format without touching the nodes; a new node type breaks every visitor until handled |
| **Strategy + Abstract Factory** | `TextFormat` | A format supplies a matching parser and writer and rates content it recognises |
| **Registry / Plugin** | `FormatRegistry` + `ServiceLoader` | New formats are added with a class and one line in `META-INF/services` |
| **Template Method** | `AbstractTextWriter`, `InlineRewriter` | The skeleton is fixed; subclasses fill in one step |
| **Decorator** | `IndentedOutput` over `Appendable` | Adds indentation tracking to any output target |
| **Chain / Pipeline** | `DocumentTransformer.then`, `chain(...)` | Transformations compose as pure functions |
| **Builder** | `Document.builder()`, `JsonObject.builder()`, Lombok `@Builder` on `Converter` | Readable construction of immutable objects |
| **Facade** | `api.Converter` | The whole library in one call: `convert(in, out)` |
| **Factory Method** | `Paragraph.of`, `ListBlock.bullet`, `IndentStyle.parse` | Intention-revealing creation with validation |

---

## Modern Java

- **Records** for every AST node, the JSON model, options and results; compact constructors validate input and make defensive copies with `List.copyOf`.
- **Sealed interfaces** for `Node`, `Block`, `Inline`, `Style`, `JsonValue` and `XmlNode`.
- **Pattern matching for `switch`**, including record deconstruction (`case InlineNode(Inline inline) ->`) and guards (`case JsonObject o when ...`).
- **Stream API** for statistics (`groupingBy` into a `TreeMap`), table column widths, format detection (`max` by confidence), tree traversal and rendering joins.
- **Text blocks** for templates and expected outputs in tests.
- **Lombok** where it removes boilerplate: `@Builder` for the facade, `@UtilityClass`, `@RequiredArgsConstructor`, `@Getter`; generated code is excluded from coverage.
- **`-Xlint:all -Werror`**: the build fails on any compiler warning.

---

## Using the Library

```java
// Convert files: the output format is the extension of the output path
Converter.defaults().convert(Path.of("in.md"), Path.of("out.json"));

// Convert strings, removing styles and indenting with tabs
String json = Converter.builder()
        .stripStyles(true)
        .indent(IndentStyle.tabs())
        .build()
        .convert("**Hello** *world*", "json");

// Work with the model directly
Document document = new MarkdownParser().parse(markdown);
long headings = Nodes.descendantsAndSelf(document).filter(Heading.class::isInstance).count();
Document plain = StyleRemover.instance().apply(document);
String html = new XhtmlWriter(new WriterOptions(IndentStyle.spaces(4))).render(plain);
```

### Adding a format

1. Implement `TextFormat`: an id, extensions and capabilities, plus `parser()` and/or `writer(options)` and `detect(content)`.
2. Register it in `src/main/resources/META-INF/services/io.github.thbldprncee.textformat.format.TextFormat`.

The CLI, the registry, `--list-formats` and the output extension lookup pick it up automatically.

---

## Format Notes

<details>
<summary><b>JSON layout</b></summary>

- The document is a top-level array.
- `header`, `bold`, `italic`, `strikethrough`, `link` and `table_cell` use `"text"` when their content is plain text and `"children"` otherwise.
- Following the specification example, a top-level paragraph that starts with a formatted element is written as that element followed by a paragraph holding the rest. The reader joins such a pair back when the rest continues the sentence, i.e. starts with whitespace or punctuation.
- Other element types: `text {content}`, `code_inline {code}`, `code_block {code, language?}`, `list {ordered, children}`, `list_item`, `table`, `table_row`, `blockquote`, `horizontal_rule`, `image {url, alt}`.
- The reader is lenient where it is safe: it accepts `"level": "2"` and `"ordered": "true"`, clamps heading levels to 1..6, and accepts a `{"type": "document"}` root.

</details>

<details>
<summary><b>RTF</b></summary>

- Output is RTF 1.9 with `\ansi\ansicpg1252`. Any non-ASCII character is written as `\uN?`, so Cyrillic and emoji survive in any reader.
- Visual structure uses standard control words: `\outlinelevel` for headings, `\li`/`\fi` indents for lists, real table rows, `HYPERLINK` and `INCLUDEPICTURE` fields, and a bottom border for rules.
- Structure that RTF cannot express is stored in ignorable `{\*\tfmeta …}` groups: list nesting, code block languages and quote depth. Word and other readers skip these groups; this converter uses them to round-trip exactly.
- RTF from other producers is interpreted by heuristics: monospace fonts from the font table become code, and `•`/`1.` paragraphs become lists.

</details>

<details>
<summary><b>XHTML</b></summary>

- Output is a well-formed XHTML 1.0 Strict document. The `<title>` is taken from the first heading. Text and attributes are escaped, and code is never re-indented.
- Input may be XHTML or HTML: void elements without a slash, whitespace collapsing like a browser, `<strong>`/`<b>`, `<em>`/`<i>`, `<del>`/`<s>`/`<strike>`. `head`, `script` and `style` are skipped.

</details>

---

## Testing

```bash
./gradlew test                     # 154 tests
./gradlew jacocoTestReport         # build/reports/jacoco/test/html/index.html
./gradlew check                    # fails below 85% instruction coverage
```

| Suite | What it proves |
|-------|----------------|
| `TaskExampleTest` | The assignment example converts to the expected JSON exactly, and back |
| `RoundTripTest` | For every readable format and indent style, `parse(write(doc)) == doc`; Markdown ↔ JSON ↔ XHTML chains keep the model |
| `MarkdownParserTest` | Headings, lists (nested, multi-paragraph, lazy), tables, quotes, fences, every inline construct, escapes |
| `JsonReaderTest` / `JsonFormatTest` | Strict RFC 8259 parsing, error positions, the spec layout, JSON-path error messages |
| `XhtmlTest` | Entities, whitespace, malformed markup errors, escaping, indentation |
| `RtfTest` | RTF from other producers: code pages, `\uN`, fields, tables, bullets |
| `ConverterTest` / `MainTest` | Style removal, indentation, `--from`, file conversion, exit codes, messages |

Current coverage: **96.7% of lines, 96.5% of instructions**.

---

## Project Structure

```
formatted-text-converter/
├── src/main/java/io/github/thbldprncee/textformat/
│   ├── ast/                  # sealed record model, Visitor, Nodes queries
│   │   ├── transform/        # DocumentTransformer, InlineRewriter, StyleRemover, TextMerger
│   │   └── stats/            # DocumentStatistics
│   ├── format/               # TextFormat, FormatRegistry, parser/writer contracts, base writers
│   ├── formats/
│   │   ├── markdown/         # block parser, inline parser, writer
│   │   ├── json/             # JsonValue model, reader, serializer, AST ↔ JSON mapper
│   │   ├── xhtml/            # XML reader, XHTML parser and writer
│   │   ├── rtf/              # RTF interpreter + structure assembler, writer
│   │   ├── text/             # plain text
│   │   └── latex/            # LaTeX export
│   ├── common/               # CharCursor, IndentStyle, IndentedOutput, exceptions
│   ├── api/                  # Converter facade
│   └── cli/                  # Main, CliOptions
├── src/main/resources/META-INF/services/   # format registration
├── src/test/java/...          # 154 tests
└── examples/                  # sample documents in every format
```

---

## Background

The project started as a C++ university assignment: read and write several text formats through a single AST, remove styles, and configure the indentation of XML and JSON output. This is a Java 21 reimplementation that adds strikethrough, links, images, quotes and rules, plain text and LaTeX output, content-based format detection, statistics, a library API and lossless round trips across every format.

## License

[MIT](LICENSE)
