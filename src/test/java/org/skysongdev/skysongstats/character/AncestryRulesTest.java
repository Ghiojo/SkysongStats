package org.skysongdev.skysongstats.character;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.storage.StoredAncestryAdjustment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.character.AncestryFixtures.ancestry;

/**
 * Plain JUnit tests for {@link AncestryRules}: what picks are allowed, and what they give.
 */
class AncestryRulesTest {
    private static final AncestryDefinition HUMAN = ancestry("human");
    private static final AncestryDefinition ORC = ancestry("orc");

    private static void assertProblem(List<String> problems, String expectedPart) {
        if (problems.stream().noneMatch(problem -> problem.contains(expectedPart))) {
            throw new AssertionError("expected a problem containing \"" + expectedPart + "\" but got " + problems);
        }
    }

    private static StoredAncestryAdjustment adjustment(String stat, int amount, String source) {
        return new StoredAncestryAdjustment(stat, amount, source);
    }

    @Test
    void validPicksHaveNoProblems() {
        AncestryPicks picks = AncestryPicks.none().withStats(0, List.of("str", "con")).withStats(1, List.of("str"));
        assertEquals(List.of(), AncestryRules.problems(HUMAN, picks));
        assertEquals(List.of(), AncestryRules.problems(ancestry("dwarf"), AncestryPicks.none()));
    }

    @Test
    void everyChoiceMustBePicked() {
        assertProblem(AncestryRules.problems(HUMAN, AncestryPicks.none()), "choice 1 of human is not picked");
        assertProblem(AncestryRules.problems(HUMAN, AncestryPicks.none()), "choice 2 of human is not picked");
    }

    @Test
    void picksForChoicesThatDontExistAreProblems() {
        assertProblem(AncestryRules.problems(ancestry("dwarf"), AncestryPicks.none().withPackage(0, 0)),
                "choice 1 doesn't exist, dwarf has 0 choice(s)");
    }

    @Test
    void statPicksMustMatchTheChoice() {
        List<String> problems = AncestryRules.problems(HUMAN, AncestryPicks.none()
                .withStats(0, List.of("str", "str")).withStats(1, List.of("dex")));
        assertProblem(problems, "choice 1 of human picks 'str' twice");
        assertProblem(problems, "choice 2 of human can't pick 'dex', only [str, con]");
        assertProblem(AncestryRules.problems(HUMAN, AncestryPicks.none().withStats(0, List.of("str"))
                .withStats(1, List.of("con"))), "choice 1 of human needs 2 stat(s), got 1");
        assertProblem(AncestryRules.problems(HUMAN, AncestryPicks.none().withPackage(0, 0)
                .withStats(1, List.of("con"))), "choice 1 of human picks stats, not a package");
    }

    @Test
    void aBoostCantLandOnAStatTheFixedStatsAlreadyRaise() {
        assertProblem(AncestryRules.problems(HUMAN, AncestryPicks.none().withStats(0, List.of("dex", "str"))
                .withStats(1, List.of("con"))), "can't pick 'dex': this ancestry already raises it");
    }

    @Test
    void aFlawOnABoostedStatIsFine() {
        AncestryPicks picks = AncestryPicks.none().withStats(0, List.of("str", "con")).withStats(1, List.of("con"));
        assertEquals(List.of(), AncestryRules.problems(HUMAN, picks));
    }

    @Test
    void twoChoicesCantBoostTheSameStat() {
        // Orc: Brawler gives con +2, so the second choice can't also boost con.
        AncestryPicks picks = AncestryPicks.none().withPackage(0, 0).withStats(1, List.of("con"));
        assertProblem(AncestryRules.problems(ORC, picks), "choice 2 of orc can't pick 'con'");
        assertProblem(AncestryRules.problems(ORC, picks), "choice 1 of orc can't take package 'Brawler'");
        assertEquals(List.of(), AncestryRules.problems(ORC, picks.withStats(1, List.of("dex")).withPackage(0, 0)));
    }

    @Test
    void packagePicksMustExist() {
        assertProblem(AncestryRules.problems(ORC, AncestryPicks.none().withPackage(0, 2).withStats(1, List.of("dex"))),
                "choice 1 of orc has no package 3, it has 2");
        assertProblem(AncestryRules.problems(ORC, AncestryPicks.none().withStats(0, List.of("dex"))
                .withStats(1, List.of("dex"))), "choice 1 of orc picks a package, not stats");
    }

    @Test
    void aStackingChoiceMayLandOnABoostedStat() {
        assertEquals(List.of(), AncestryRules.problems(ancestry("giant"),
                AncestryPicks.none().withStats(0, List.of("str"))));
        assertFalse(AncestryRules.conflicts(ancestry("giant"), AncestryPicks.none(), 0, "str", 1));
    }

    @Test
    void conflictsIgnoresTheChoicesOwnPicks() {
        AncestryPicks picks = AncestryPicks.none().withStats(0, List.of("str", "con"));
        assertFalse(AncestryRules.conflicts(HUMAN, picks, 0, "str", 1));
        assertTrue(AncestryRules.conflicts(HUMAN, picks, 0, "dex", 1));
        assertFalse(AncestryRules.conflicts(HUMAN, picks, 1, "str", -1));
    }

    @Test
    void theAdjustmentsNameTheirSource() {
        AncestryPicks picks = AncestryPicks.none().withPackage(0, 1).withStats(1, List.of("con"));
        assertEquals(List.of(adjustment("str", 1, "fixed")), AncestryRules.fixedAdjustments(ORC));
        assertEquals(List.of(adjustment("dex", 1, "choice:1"), adjustment("spd", 1, "choice:1"),
                adjustment("con", 1, "choice:2")), AncestryRules.choiceAdjustments(ORC, picks));
    }

    @Test
    void picksAreValues() {
        AncestryPicks picks = AncestryPicks.none().withStats(0, List.of("str"));
        assertEquals(picks, AncestryPicks.none().withStats(0, List.of("str")));
        assertEquals(picks.hashCode(), AncestryPicks.none().withStats(0, List.of("str")).hashCode());
        assertEquals(AncestryPicks.none(), picks.without(0));
        assertTrue(picks.toString().contains("str"));
    }
}
