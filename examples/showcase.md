# Formatted Text Converter

One document model, many formats. **Bold**, *italic*, ~~strikethrough~~, `inline code` and [links](https://github.com/thbldprncee) survive every conversion.

## Lists

- Markdown
- XHTML
  1. nested lists
  2. keep their numbering
- JSON, RTF, plain text and LaTeX

## Code

```java
Converter.defaults().convert(Path.of("in.md"), Path.of("out.json"));
```

## Tables

| Format   | Read | Write |
| -------- | ---- | ----- |
| Markdown | yes  | yes   |
| LaTeX    | no   | yes   |

> Quotes work too.
>
> > Even nested ones.

---

Кириллица и «типографика» — тоже.
