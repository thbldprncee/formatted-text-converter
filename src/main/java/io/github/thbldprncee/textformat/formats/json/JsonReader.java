package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.common.CharCursor;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonArray;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonBoolean;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNull;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNumber;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonObject;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict RFC 8259 parser (recursive descent) with line/column error reporting. */
public final class JsonReader {

    private static final int MAX_DEPTH = 512;

    private final CharCursor cursor;
    private int depth;

    private JsonReader(String input) {
        this.cursor = new CharCursor(input);
    }

    public static JsonValue parse(String input) {
        JsonReader reader = new JsonReader(input.startsWith("﻿") ? input.substring(1) : input);
        reader.cursor.skipWhitespace();
        JsonValue value = reader.value();
        reader.cursor.skipWhitespace();
        if (!reader.cursor.atEnd()) {
            throw reader.cursor.error("Unexpected content after the JSON value");
        }
        return value;
    }

    private JsonValue value() {
        return switch (cursor.peek()) {
            case '{' -> nested(this::object);
            case '[' -> nested(this::array);
            case '"' -> new JsonString(string());
            case 't' -> literal("true", new JsonBoolean(true));
            case 'f' -> literal("false", new JsonBoolean(false));
            case 'n' -> literal("null", new JsonNull());
            default -> {
                char ch = cursor.peek();
                if (ch == '-' || Character.isDigit(ch)) {
                    yield number();
                }
                throw cursor.error(cursor.atEnd() ? "Unexpected end of JSON" : "Unexpected character '" + ch + "'");
            }
        };
    }

    private JsonValue nested(java.util.function.Supplier<JsonValue> parser) {
        if (++depth > MAX_DEPTH) {
            throw cursor.error("JSON is nested too deeply");
        }
        JsonValue value = parser.get();
        depth--;
        return value;
    }

    private JsonValue object() {
        cursor.expect('{');
        Map<String, JsonValue> members = new LinkedHashMap<>();
        cursor.skipWhitespace();
        if (cursor.consume('}')) {
            return new JsonObject(members);
        }
        do {
            cursor.skipWhitespace();
            if (cursor.peek() != '"') {
                throw cursor.error("Expected a string key");
            }
            String key = string();
            cursor.skipWhitespace();
            cursor.expect(':');
            cursor.skipWhitespace();
            members.put(key, value());
            cursor.skipWhitespace();
        } while (cursor.consume(','));
        cursor.expect('}');
        return new JsonObject(members);
    }

    private JsonValue array() {
        cursor.expect('[');
        List<JsonValue> elements = new ArrayList<>();
        cursor.skipWhitespace();
        if (cursor.consume(']')) {
            return new JsonArray(elements);
        }
        do {
            cursor.skipWhitespace();
            elements.add(value());
            cursor.skipWhitespace();
        } while (cursor.consume(','));
        cursor.expect(']');
        return new JsonArray(elements);
    }

    private String string() {
        cursor.expect('"');
        StringBuilder text = new StringBuilder();
        while (true) {
            if (cursor.atEnd()) {
                throw cursor.error("Unterminated string");
            }
            char ch = cursor.next();
            if (ch == '"') {
                return text.toString();
            }
            if (ch < 0x20) {
                throw cursor.error("Unescaped control character in string");
            }
            if (ch != '\\') {
                text.append(ch);
                continue;
            }
            char escaped = cursor.next();
            switch (escaped) {
                case '"' -> text.append('"');
                case '\\' -> text.append('\\');
                case '/' -> text.append('/');
                case 'b' -> text.append('\b');
                case 'f' -> text.append('\f');
                case 'n' -> text.append('\n');
                case 'r' -> text.append('\r');
                case 't' -> text.append('\t');
                case 'u' -> text.append(unicodeEscape());
                default -> throw cursor.error("Invalid escape '\\" + escaped + "'");
            }
        }
    }

    private char unicodeEscape() {
        int code = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(cursor.next(), 16);
            if (digit < 0) {
                throw cursor.error("Invalid \\u escape");
            }
            code = code * 16 + digit;
        }
        return (char) code; // surrogate pairs arrive as two escapes and combine in the StringBuilder
    }

    private JsonValue number() {
        StringBuilder number = new StringBuilder();
        if (cursor.peek() == '-') {
            number.append(cursor.next());
        }
        String integer = cursor.readWhile(Character::isDigit);
        if (integer.isEmpty() || (integer.length() > 1 && integer.charAt(0) == '0')) {
            throw cursor.error("Invalid number");
        }
        number.append(integer);
        if (cursor.consume('.')) {
            String fraction = cursor.readWhile(Character::isDigit);
            if (fraction.isEmpty()) {
                throw cursor.error("Invalid number: digits expected after '.'");
            }
            number.append('.').append(fraction);
        }
        if (cursor.peek() == 'e' || cursor.peek() == 'E') {
            number.append(cursor.next());
            if (cursor.peek() == '+' || cursor.peek() == '-') {
                number.append(cursor.next());
            }
            String exponent = cursor.readWhile(Character::isDigit);
            if (exponent.isEmpty()) {
                throw cursor.error("Invalid number: exponent expected");
            }
            number.append(exponent);
        }
        return new JsonNumber(new BigDecimal(number.toString()));
    }

    private JsonValue literal(String word, JsonValue value) {
        cursor.expect(word);
        return value;
    }
}
