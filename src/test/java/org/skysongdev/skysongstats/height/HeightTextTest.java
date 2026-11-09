package org.skysongdev.skysongstats.height;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading heights in metres or feet and inches, and writing them back.
 */
class HeightTextTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2 | 200",
            "1.8 | 180",
            "1.75 | 175",
            "1,75 | 175",
            "1.75m | 175",
            "0.8 | 80",
            "3.6 | 360",
            " 1.80 | 180"
    })
    void readsMetres(String text, int centimetres) {
        assertEquals(centimetres, HeightText.parse(text).orElseThrow());
    }

    // 5 ft 10 in = 70 in = 177.8 cm, rounded to 178.
    @ParameterizedTest
    @ValueSource(strings = {
            "5'10\"", "5'10''", "5'10",
            "5’10”", "5‘10“", "5′10″", "5´10˝", "5`10\"",
            "5’10’’", "5‛10„", "5'10‟"
    })
    void readsFeetAndInchesWithAnyMarks(String text) {
        assertEquals(178, HeightText.parse(text).orElseThrow());
    }

    @Test
    void readsFeetWithoutInches() {
        assertEquals(183, HeightText.parse("6'").orElseThrow());
        assertEquals(152, HeightText.parse("5'0\"").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.755", "-1.8", "tall", "", "5'12\"", "5'10.5\"", "5''", "10\"", "1.8.0", "5' 10\""})
    void refusesWhatIsNotAHeight(String text) {
        assertTrue(HeightText.parse(text).isEmpty(), text);
    }

    @Test
    void spotsTheInchesOfAHeightSplitByASpace() {
        assertTrue(HeightText.isLoneInches("10\""));
        assertTrue(HeightText.isLoneInches("10”"));
        assertTrue(HeightText.isLoneInches("10''"));
        assertFalse(HeightText.isLoneInches("Steve"));
        assertFalse(HeightText.isLoneInches("10"));
        assertFalse(HeightText.isLoneInches(null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "175 | 1.75 | 5'9\"",
            "180 | 1.80 | 5'11\"",
            "200 | 2.00 | 6'7\"",
            "80 | 0.80 | 2'7\"",
            "360 | 3.60 | 11'10\"",
            "183 | 1.83 | 6'0\""
    })
    void writesMetresAndFeet(int centimetres, String metres, String feet) {
        assertEquals(metres, HeightText.metres(centimetres));
        assertEquals(feet, HeightText.feet(centimetres));
    }

    @Test
    void writesScalesWithAtMostThreeDecimals() {
        assertEquals("1", HeightText.scale(1.0));
        assertEquals("2", HeightText.scale(2.0));
        assertEquals("0.972", HeightText.scale(175 / 180.0));
        assertEquals("0.444", HeightText.scale(80 / 180.0));
    }
}
