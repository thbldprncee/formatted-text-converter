package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.ParseException;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonArray;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonBoolean;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNull;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNumber;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonObject;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonReaderTest {

    @Test
    void parsesAllValueTypes() {
        JsonValue value = JsonReader.parse("""
                \uFEFF{"s": "a\\"b\\\\\\/\\b\\f\\n\\r\\t\\u0416\\ud83d\\ude00", "n": -12.5e2, "z": 0,
                 "t": true, "f": false, "x": null, "a": [1, [], {}]}""");
        assertThat(value).isEqualTo(new JsonObject(Map.of(
                "s", new JsonString("a\"b\\/\b\f\n\r\tЖ\uD83D\uDE00"),
                "n", new JsonNumber(new BigDecimal("-12.5e2")),
                "z", new JsonNumber(BigDecimal.ZERO),
                "t", new JsonBoolean(true),
                "f", new JsonBoolean(false),
                "x", new JsonNull(),
                "a", new JsonArray(List.of(JsonNumber.of(1), new JsonArray(List.of()), new JsonObject(Map.of())))))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "[1,]", "{\"a\" 1}", "{a: 1}", "[01]", "[1.]", "[1e]", "\"open", "[\"\\x\"]",
            "[\"\\u12G4\"]", "tru", "[1] 2", "@", "[\"a\nb\"]", "{\"a\":1,}"})
    void rejectsMalformedJson(String json) {
        assertThatThrownBy(() -> JsonReader.parse(json)).isInstanceOf(ParseException.class);
    }

    @Test
    void reportsPosition() {
        assertThatThrownBy(() -> JsonReader.parse("[\n  1,\n  x]"))
                .isInstanceOfSatisfying(ParseException.class, e -> {
                    assertThat(e.line()).isEqualTo(3);
                    assertThat(e.column()).isEqualTo(3);
                });
    }

    @Test
    void rejectsExcessiveNesting() {
        assertThatThrownBy(() -> JsonReader.parse("[".repeat(600))).hasMessageContaining("nested too deeply");
    }

    @Test
    void serializerIndentsAndEscapes() {
        JsonValue value = JsonObject.builder()
                .put("k", "q\"\\\n\r\t\b\f\u0001")
                .put("n", 3)
                .put("b", true)
                .put("e", new JsonArray(List.of()))
                .put("o", new JsonObject(Map.of()))
                .put("x", new JsonNull())
                .put("d", new JsonNumber(new BigDecimal("1.500")))
                .build();
        assertThat(JsonSerializer.toString(value, IndentStyle.tabs())).isEqualTo("""
                {
                \t"k": "q\\"\\\\\\n\\r\\t\\b\\f\\u0001",
                \t"n": 3,
                \t"b": true,
                \t"e": [],
                \t"o": {},
                \t"x": null,
                \t"d": 1.5
                }
                """);
        assertThat(value.typeName()).isEqualTo("object");
        assertThat(new JsonNull().typeName()).isEqualTo("null");
    }
}
