package io.github.thbldprncee.textformat;

import io.github.thbldprncee.textformat.api.Converter;
import io.github.thbldprncee.textformat.ast.Bold;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.ast.Heading;
import io.github.thbldprncee.textformat.ast.InlineCode;
import io.github.thbldprncee.textformat.ast.Italic;
import io.github.thbldprncee.textformat.ast.Paragraph;
import io.github.thbldprncee.textformat.ast.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The example from the assignment: in.md -> out.json, character for character. */
class TaskExampleTest {

    static final String IN_MD = """
            # Это тестовый файл, который показывает что парсер может

            ## Заголовки и форматирование

            **Жирный текст** и *курсивный текст*.

            `Инлайн-код:` `std::string s = "hello";`
            """;

    static final String OUT_JSON = """
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
                  {
                    "type": "text",
                    "content": " и "
                  },
                  {
                    "type": "italic",
                    "text": "курсивный текст"
                  },
                  {
                    "type": "text",
                    "content": "."
                  }
                ]
              },
              {
                "type": "code_inline",
                "code": "Инлайн-код:"
              },
              {
                "type": "paragraph",
                "children": [
                  {
                    "type": "text",
                    "content": " "
                  },
                  {
                    "type": "code_inline",
                    "code": "std::string s = \\"hello\\";"
                  }
                ]
              }
            ]
            """;

    private final Converter converter = Converter.defaults();

    @Test
    @DisplayName("in.md is converted to exactly the out.json of the assignment")
    void markdownToJsonMatchesTheSpecification() {
        assertThat(converter.convert(IN_MD, "json")).isEqualTo(OUT_JSON);
    }

    @Test
    @DisplayName("out.json is read back into the same document as in.md")
    void jsonReadsBackIntoTheSameModel() {
        Document expected = Document.of(
                Heading.of(1, new Text("Это тестовый файл, который показывает что парсер может")),
                Heading.of(2, new Text("Заголовки и форматирование")),
                Paragraph.of(Bold.of("Жирный текст"), new Text(" и "), Italic.of("курсивный текст"), new Text(".")),
                Paragraph.of(new InlineCode("Инлайн-код:"), new Text(" "), new InlineCode("std::string s = \"hello\";")));

        assertThat(converter.read(IN_MD)).isEqualTo(expected);
        assertThat(converter.read(OUT_JSON)).isEqualTo(expected);
    }
}
