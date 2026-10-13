package io.github.thbldprncee.textformat.common;

/**
 * Indentation of nested elements in the XML (XHTML) and JSON outputs.
 *
 * @param character {@code ' '} or {@code '\t'}
 * @param width     how many characters one level adds
 */
public record IndentStyle(char character, int width) {

    public static final IndentStyle DEFAULT = spaces(2);

    public IndentStyle {
        if (character != ' ' && character != '\t') {
            throw new IllegalArgumentException("Indent must use spaces or tabs");
        }
        if (width < 0 || width > 16) {
            throw new IllegalArgumentException("Indent width must be 0..16, got " + width);
        }
    }

    public static IndentStyle spaces(int width) {
        return new IndentStyle(' ', width);
    }

    public static IndentStyle tabs() {
        return new IndentStyle('\t', 1);
    }

    /** Parses CLI notation: a number of spaces ({@code 4}) or {@code tab}. */
    public static IndentStyle parse(String value) {
        if ("tab".equalsIgnoreCase(value) || "tabs".equalsIgnoreCase(value)) {
            return tabs();
        }
        try {
            return spaces(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Indent must be a number of spaces or 'tab', got '" + value + "'");
        }
    }

    public String unit() {
        return String.valueOf(character).repeat(width);
    }

    public String at(int level) {
        return unit().repeat(Math.max(0, level));
    }
}
