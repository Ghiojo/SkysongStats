package org.skysongdev.skysongstats.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The /setup point rules with the default stats.yml: 7 points, 3 negative points,
 * each stat from 3 below to 4 above its baseline.
 */
class PointAllocationTest {
    private PointAllocation allocation;
    private StatDefinitions stats;

    @BeforeEach
    void setUp() {
        StatsConfig config = SetupTestData.defaultConfig();
        stats = config.stats();
        allocation = new PointAllocation(stats.setupStats(), config.setupRules());
    }

    private StatDefinition stat(String id) {
        return stats.get(id).orElseThrow();
    }

    private void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }

    @Test
    void everyStatStartsAtItsBaselineWithEveryPointUnspent() {
        assertEquals(Map.of("str", 0, "dex", 0, "con", 0, "foc", 0, "spd", 6), allocation.values());
        assertEquals(7, allocation.points());
        assertEquals(3, allocation.negativePoints());
    }

    @Test
    void raisingCostsAPointUpToTheMax() {
        repeat(4, () -> assertTrue(allocation.raise(stat("str"))));
        assertFalse(allocation.raise(stat("str")));
        assertEquals(4, allocation.value(stat("str")));
        assertEquals(3, allocation.points());
    }

    // Speed is counted from its baseline of 6, as before.
    @Test
    void theRangeIsRelativeToTheBaseline() {
        repeat(4, () -> allocation.raise(stat("spd")));
        assertFalse(allocation.raise(stat("spd")));
        assertEquals(10, allocation.value(stat("spd")));
    }

    @Test
    void noPointsLeftMeansNoRaising() {
        repeat(4, () -> allocation.raise(stat("str")));
        repeat(3, () -> allocation.raise(stat("dex")));
        assertEquals(0, allocation.points());
        assertFalse(allocation.raise(stat("con")));
        assertEquals(0, allocation.value(stat("con")));
    }

    @Test
    void goingBelowTheBaselineGivesAPointAndUsesANegativePoint() {
        assertTrue(allocation.lower(stat("str")));
        assertEquals(-1, allocation.value(stat("str")));
        assertEquals(8, allocation.points());
        assertEquals(2, allocation.negativePoints());

        assertTrue(allocation.raise(stat("str")));
        assertEquals(7, allocation.points());
        assertEquals(3, allocation.negativePoints());
    }

    @Test
    void loweringARaisedStatOnlyGivesThePointBack() {
        allocation.raise(stat("str"));
        assertTrue(allocation.lower(stat("str")));
        assertEquals(7, allocation.points());
        assertEquals(3, allocation.negativePoints());
    }

    @Test
    void negativePointsAndTheMinLimitLowering() {
        repeat(3, () -> assertTrue(allocation.lower(stat("str"))));
        assertFalse(allocation.lower(stat("str")), "at the min");
        assertFalse(allocation.lower(stat("dex")), "no negative points left");
        assertEquals(-3, allocation.value(stat("str")));
        assertEquals(0, allocation.value(stat("dex")));
        assertEquals(10, allocation.points());
    }

    @Test
    void confirmingNeedsEveryPointSpent() {
        assertFalse(allocation.canConfirm());
        repeat(4, () -> allocation.raise(stat("str")));
        repeat(3, () -> allocation.raise(stat("dex")));
        assertTrue(allocation.canConfirm());
    }

    // The old screen refused to confirm with exactly one negative point left.
    @Test
    void confirmingIsRefusedWithExactlyOneNegativePointLeft() {
        allocation.lower(stat("str"));
        allocation.lower(stat("dex"));
        repeat(4, () -> allocation.raise(stat("con")));
        repeat(4, () -> allocation.raise(stat("foc")));
        allocation.raise(stat("spd"));
        assertEquals(0, allocation.points());
        assertEquals(1, allocation.negativePoints());
        assertFalse(allocation.canConfirm());
    }

    @Test
    void resetGoesBackToTheStart() {
        allocation.lower(stat("str"));
        allocation.raise(stat("dex"));
        allocation.reset();
        assertEquals(Map.of("str", 0, "dex", 0, "con", 0, "foc", 0, "spd", 6), allocation.values());
        assertEquals(7, allocation.points());
        assertEquals(3, allocation.negativePoints());
    }

    @Test
    void statsWithoutSetupAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> allocation.value(stat("hp")));
    }
}
