package io.github.thbldprncee.textformat.formats;

import io.github.thbldprncee.textformat.TestDocuments;
import io.github.thbldprncee.textformat.ast.Document;
import io.github.thbldprncee.textformat.common.IndentStyle;
import io.github.thbldprncee.textformat.format.FormatRegistry;
import io.github.thbldprncee.textformat.format.TextFormat;
import io.github.thbldprncee.textformat.format.WriterOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** write(document) followed by parse() must give back the very same model. */
class RoundTripTest {

    private static final FormatRegistry REGISTRY = FormatRegistry.loadInstalled();

    static Stream<Arguments> lossless() {
        return Stream.of("md", "json", "xhtml", "rtf").flatMap(format -> Stream.of(
                Arguments.of(format, IndentStyle.DEFAULT),
                Arguments.of(format, IndentStyle.tabs()),
                Arguments.of(format, IndentStyle.spaces(4))));
    }

    @ParameterizedTest(name = "{0} with indent \"{1}\"")
    @MethodSource("lossless")
    void documentSurvivesWriteAndRead(String formatId, IndentStyle indent) {
        TextFormat format = REGISTRY.require(formatId);
        Document original = formatId.equals("rtf") ? TestDocuments.rtfSubset() : TestDocuments.full();

        String written = format.writer(new WriterOptions(indent)).render(original);
        Document read = format.parser().parse(written);

        assertThat(read).isEqualTo(original);
    }

    @ParameterizedTest(name = "{0} -> {1} -> model")
    @MethodSource("pairs")
    void conversionChainsKeepTheModel(String first, String second) {
        Document original = TestDocuments.full();
        String step1 = REGISTRY.require(first).writer(WriterOptions.DEFAULT).render(original);
        Document viaFirst = REGISTRY.require(first).parser().parse(step1);
        String step2 = REGISTRY.require(second).writer(WriterOptions.DEFAULT).render(viaFirst);

        assertThat(REGISTRY.require(second).parser().parse(step2)).isEqualTo(original);
    }

    static Stream<Arguments> pairs() {
        return Stream.of("md", "json", "xhtml")
                .flatMap(a -> Stream.of("md", "json", "xhtml").filter(b -> !b.equals(a)).map(b -> Arguments.of(a, b)));
    }
}
