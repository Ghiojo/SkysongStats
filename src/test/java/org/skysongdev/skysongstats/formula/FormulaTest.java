package org.skysongdev.skysongstats.formula;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests (no server needed) for {@link Formula}.
 */
class FormulaTest {

    private static ToDoubleFunction<String> stats(Map<String, Integer> values) {
        return id -> {
            Integer value = values.get(id);
            if (value == null) {
                throw new AssertionError("formula asked for unknown stat " + id);
            }
            return value;
        };
    }

    private static int eval(String formula) {
        return Formula.parse(formula).evaluate(id -> {
            throw new AssertionError("formula asked for stat " + id);
        });
    }

    // The maximums SkysongStats hardcodes today must come out the same from formulas.
    @ParameterizedTest
    @CsvSource({
            "-3, 30", "0, 60", "1, 70", "4, 100"
    })
    void hpMaxMatchesTodaysHardcodedValue(int con, int expected) {
        Formula formula = Formula.parse("60 + {con} * 10");
        assertEquals(expected, formula.evaluate(stats(Map.of("con", con))));
    }

    @Test
    void otherTodaysMaximumsMatch() {
        for (int value = -3; value <= 4; value++) {
            ToDoubleFunction<String> sheet = stats(Map.of("con", value, "foc", value));
            assertEquals(9 + value, Formula.parse("9 + {con}").evaluate(sheet));
            assertEquals(35 + 2 * value, Formula.parse("35 + 2 * {foc}").evaluate(sheet));
            assertEquals(10 + value, Formula.parse("10 + {foc}").evaluate(sheet));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1 + 2 * 3         | 7",
            "(1 + 2) * 3       | 9",
            "10 - 4 - 3        | 3",
            "24 / 4 / 2        | 3",
            "2 * (3 + (4 - 1)) | 12",
            "-5 + 2            | -3",
            "-(2 + 3)          | -5",
            "+4                | 4",
            "3 - -2            | 5",
            "2 * -3            | -6",
            "1.5 * 3           | 4",
            "  7  |  7"
    })
    void appliesPrecedenceParenthesesAndUnaryOperators(String formula, int expected) {
        assertEquals(expected, eval(formula));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "7 / 2      | 3",
            "-7 / 2     | -4",
            "1 / 3 * 3  | 1",
            "9 / 2 * 2  | 9"
    })
    void keepsDecimalsUntilTheEndThenRoundsDown(String formula, int expected) {
        assertEquals(expected, eval(formula));
    }

    @Test
    void statIdsAreCaseInsensitiveAndTrimmed() {
        Formula formula = Formula.parse("60 + { Con } * 10 + {CON}");
        assertEquals(List.of("con"), List.copyOf(formula.references()));
        assertEquals(71, formula.evaluate(stats(Map.of("con", 1))));
    }

    @Test
    void referencesListsEachStatOnceInOrderOfAppearance() {
        Formula formula = Formula.parse("{str} + {max_hp} * {str} - {arcrot-max}");
        assertEquals(List.of("str", "max_hp", "arcrot-max"), List.copyOf(formula.references()));
    }

    @Test
    void referencesIsEmptyForConstantFormulas() {
        assertTrue(Formula.parse("6").references().isEmpty());
    }

    @Test
    void referencesCannotBeModified() {
        Formula formula = Formula.parse("{str}");
        assertThrows(UnsupportedOperationException.class, () -> formula.references().add("dex"));
    }

    @Test
    void keepsItsSourceText() {
        Formula formula = Formula.parse("60 + {con} * 10");
        assertEquals("60 + {con} * 10", formula.getSource());
        assertEquals("60 + {con} * 10", formula.toString());
    }

    @Test
    void canBeEvaluatedManyTimesWithDifferentStats() {
        Formula formula = Formula.parse("{a} * 2");
        assertEquals(2, formula.evaluate(stats(Map.of("a", 1))));
        assertEquals(10, formula.evaluate(stats(Map.of("a", 5))));
    }

    @Test
    void divisionByZeroIsReportedWithTheFormula() {
        Formula formula = Formula.parse("10 / {spd}");
        ArithmeticException error = assertThrows(ArithmeticException.class,
                () -> formula.evaluate(stats(Map.of("spd", 0))));
        assertTrue(error.getMessage().contains("10 / {spd}"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "''            | 0 | empty",
            "'   '         | 3 | empty",
            "1 +           | 3 | ended",
            "(1 + 2        | 0 | Missing ')'",
            "1 + 2)        | 5 | Unexpected ')'",
            "60 + {con     | 5 | Missing '}'",
            "{}            | 0 | Empty stat reference",
            "{ }           | 0 | Empty stat reference",
            "{max hp}      | 0 | Invalid stat id",
            "1 2           | 2 | Unexpected '2'",
            "1.2.3         | 0 | Invalid number",
            "2 ^ 3         | 2 | Unexpected '^'",
            "* 2           | 0 | found '*'",
            "con * 10      | 0 | found 'c'"
    })
    void rejectsInvalidFormulasAndPointsAtTheProblem(String formula, int position, String messagePart) {
        FormulaException error = assertThrows(FormulaException.class, () -> Formula.parse(formula));
        assertEquals(position, error.getPosition());
        assertEquals(formula, error.getSource());
        assertTrue(error.getMessage().contains(messagePart), error.getMessage());
        assertTrue(error.getMessage().contains("position " + (position + 1)), error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{con}", "{con} + {foc}"})
    void evaluateRejectsANullLookup(String formula) {
        assertThrows(NullPointerException.class, () -> Formula.parse(formula).evaluate(null));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Formula.parse(null));
    }
}
