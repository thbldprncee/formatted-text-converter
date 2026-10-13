package io.github.thbldprncee.textformat.formats.json;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Minimal JSON model; object members keep their insertion order. */
public sealed interface JsonValue {

    record JsonNull() implements JsonValue {
    }

    record JsonBoolean(boolean value) implements JsonValue {
    }

    record JsonNumber(BigDecimal value) implements JsonValue {

        public static JsonNumber of(long value) {
            return new JsonNumber(BigDecimal.valueOf(value));
        }
    }

    record JsonString(String value) implements JsonValue {
    }

    record JsonArray(List<JsonValue> elements) implements JsonValue {

        public JsonArray {
            elements = List.copyOf(elements);
        }
    }

    record JsonObject(Map<String, JsonValue> members) implements JsonValue {

        public JsonObject {
            members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
        }

        public Optional<JsonValue> get(String key) {
            return Optional.ofNullable(members.get(key));
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private final Map<String, JsonValue> members = new LinkedHashMap<>();

            public Builder put(String key, JsonValue value) {
                members.put(key, value);
                return this;
            }

            public Builder put(String key, String value) {
                return put(key, new JsonString(value));
            }

            public Builder put(String key, long value) {
                return put(key, JsonNumber.of(value));
            }

            public Builder put(String key, boolean value) {
                return put(key, new JsonBoolean(value));
            }

            public JsonObject build() {
                return new JsonObject(members);
            }
        }
    }

    /** Short name of the JSON type for error messages. */
    default String typeName() {
        return switch (this) {
            case JsonNull ignored -> "null";
            case JsonBoolean ignored -> "boolean";
            case JsonNumber ignored -> "number";
            case JsonString ignored -> "string";
            case JsonArray ignored -> "array";
            case JsonObject ignored -> "object";
        };
    }
}
