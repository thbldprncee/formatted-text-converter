package io.github.thbldprncee.textformat.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonTest {

    @Test
    void indentStyles() {
        assertThat(IndentStyle.parse("4").at(2)).isEqualTo("        ");
        assertThat(IndentStyle.parse("TAB").unit()).isEqualTo("\t");
        assertThat(IndentStyle.parse("tabs").at(-1)).isEmpty();
        assertThatThrownBy(() -> IndentStyle.parse("wide")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IndentStyle.spaces(17)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IndentStyle('-', 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indentedOutput() {
        StringBuilder target = new StringBuilder();
        new IndentedOutput(target, IndentStyle.spaces(2))
                .line("a").indent().line("b").write("").indent().write("c").write("d").newline().dedent().dedent().dedent()
                .line("e");
        assertThat(target).hasToString("a\n  b\n    cd\ne\n");

        Appendable failing = new Appendable() {
            public Appendable append(CharSequence csq) throws IOException { throw new IOException("disk full"); }
            public Appendable append(CharSequence csq, int s, int e) throws IOException { throw new IOException(); }
            public Appendable append(char c) throws IOException { throw new IOException(); }
        };
        assertThatThrownBy(() -> new IndentedOutput(failing, IndentStyle.DEFAULT).write("x"))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void charCursor() {
        CharCursor cursor = new CharCursor("ab\ncd");
        assertThat(cursor.consume('a')).isTrue();
        assertThat(cursor.consume('x')).isFalse();
        assertThat(cursor.consume("b\n")).isTrue();
        assertThat(cursor.line()).isEqualTo(2);
        assertThat(cursor.consume("zz")).isFalse();
        assertThat(cursor.readUntil("!")).isEqualTo("cd");
        assertThat(cursor.atEnd()).isTrue();
        assertThat(cursor.peek()).isEqualTo(CharCursor.EOF);
        assertThatThrownBy(cursor::next).isInstanceOf(ParseException.class).hasMessageContaining("line 2");
        assertThatThrownBy(() -> cursor.expect('x')).hasMessageContaining("end of input");
        assertThatThrownBy(() -> cursor.expect("x")).hasMessageContaining("Expected \"x\"");
        assertThat(new TextFormatException("m", new RuntimeException()).getCause()).isNotNull();
    }
}
