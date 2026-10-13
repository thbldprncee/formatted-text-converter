package io.github.thbldprncee.textformat.formats.json;

import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.common.IndentedOutput;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonArray;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonBoolean;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNull;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonNumber;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonObject;
import io.github.thbldprncee.textformat.formats.json.JsonValue.JsonString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Pretty printer: one element per line, nested levels indented with the given style. */
public final class JsonSerializer {

    private final IndentedOutput out;

    private JsonSerializer(Appendable target, IndentStyle indent) {
        this.out = new IndentedOutput(target, indent);
    }

    public static void write(JsonValue value, Appendable target, IndentStyle indent) {
        JsonSerializer serializer = new JsonSerializer(target, indent);
        serializer.value(value);
        serializer.out.newline();
    }

    public static String toString(JsonValue value, IndentStyle indent) {
        StringBuilder out = new StringBuilder();
        write(value, out, indent);
        return out.toString();
    }

    private void value(JsonValue value) {
        switch (value) {
            case JsonNull ignored -> out.write("null");
            case JsonBoolean bool -> out.write(String.valueOf(bool.value()));
            case JsonNumber number -> out.write(number.value().stripTrailingZeros().toPlainString());
            case JsonString string -> out.write(quote(string.value()));
            case JsonArray array -> array(array.elements());
            case JsonObject object -> object(object.members());
        }
    }

    private void array(List<JsonValue> elements) {
        if (elements.isEmpty()) {
            out.write("[]");
            return;
        }
        out.line("[").indent();
        for (Iterator<JsonValue> it = elements.iterator(); it.hasNext(); ) {
            value(it.next());
            out.write(it.hasNext() ? "," : "").newline();
        }
        out.dedent().write("]");
    }

    private void object(Map<String, JsonValue> members) {
        if (members.isEmpty()) {
            out.write("{}");
            return;
        }
        out.line("{").indent();
        for (Iterator<Map.Entry<String, JsonValue>> it = members.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, JsonValue> member = it.next();
            out.write(quote(member.getKey())).write(": ");
            value(member.getValue());
            out.write(it.hasNext() ? "," : "").newline();
        }
        out.dedent().write("}");
    }

    /** Non-ASCII characters are written as is (UTF-8 output), like in the task example. */
    static String quote(String value) {
        StringBuilder quoted = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                default -> {
                    if (ch < 0x20) {
                        quoted.append("\\u%04x".formatted((int) ch));
                    } else {
                        quoted.append(ch);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }
}
