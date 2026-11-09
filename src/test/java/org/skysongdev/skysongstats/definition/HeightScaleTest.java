package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "scale:" block of stats.yml, which makes a stat a height, and the scale a
 * height gives.
 */
class HeightScaleTest {
    private static final HeightScale DEFAULT = new HeightScale(180, 80, 360);

    private static StatsConfig load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(config, name -> true);
    }

    private static void assertError(String yaml, String expectedPart) {
        List<String> errors = assertThrows(DefinitionException.class, () -> load(yaml)).getErrors();
        assertTrue(errors.stream().anyMatch(error -> error.contains(expectedPart)),
                "expected an error containing \"" + expectedPart + "\" but got " + errors);
    }

    @Test
    void theScaleIsLinearFromTheNormalHeight() {
        assertEquals(1.0, DEFAULT.scaleFor(180));
        assertEquals(2.0, DEFAULT.scaleFor(360));
        // 3 m is exactly 3 blocks: a scale-1 player is 1.8 blocks tall.
        assertEquals(3.0, DEFAULT.scaleFor(300) * 1.8, 1e-9);
        assertEquals(80 / 180.0, DEFAULT.scaleFor(80));
    }

    @Test
    void modifiersCantPushTheScalePastMinOrMax() {
        assertEquals(2.0, DEFAULT.scaleFor(400));
        assertEquals(80 / 180.0, DEFAULT.scaleFor(10));
    }

    @Test
    void allowsHeightsFromMinToMax() {
        assertTrue(DEFAULT.allows(80));
        assertTrue(DEFAULT.allows(360));
        assertFalse(DEFAULT.allows(79));
        assertFalse(DEFAULT.allows(361));
    }

    @Test
    void readsTheScaleBlock() {
        StatDefinitions stats = load("""
                stats:
                  str: {}
                  tall:
                    baseline: 150
                    scale: {normal: 150, min: 50, max: 300}
                """).stats();
        StatDefinition tall = stats.get("tall").orElseThrow();
        assertEquals(new HeightScale(150, 50, 300), tall.scale());
        assertTrue(tall.isHeight());
        assertEquals("tall", stats.height().orElseThrow().id());
        assertFalse(stats.get("str").orElseThrow().isHeight());
    }

    @Test
    void aFileWithoutAScaleBlockHasNoHeight() {
        assertTrue(load("stats:\n  str: {}\n").stats().height().isEmpty());
    }

    @Test
    void onlyOneStatCanSetTheScale() {
        assertError("""
                stats:
                  a:
                    baseline: 180
                    scale: {normal: 180, min: 80, max: 360}
                  b:
                    baseline: 180
                    scale: {normal: 180, min: 80, max: 360}
                """, "only one stat can have a 'scale:' block");
    }

    @Test
    void barsCantBeHeights() {
        assertError("""
                stats:
                  hp:
                    type: bar
                    max: 10
                    scale: {normal: 180, min: 80, max: 360}
                """, "only plain values can be a height");
    }

    @Test
    void refusesBrokenScales() {
        assertError("stats:\n  h:\n    baseline: 180\n    scale: {min: 80, max: 360}\n", "stats.h.scale.normal");
        assertError("stats:\n  h:\n    baseline: 180\n    scale: {normal: 0, min: 80, max: 360}\n", "must be above 0");
        assertError("stats:\n  h:\n    baseline: 180\n    scale: {normal: 180, min: 300, max: 200}\n",
                "min can't be above max");
        assertError("stats:\n  h:\n    baseline: 180\n    scale: {normal: 180, min: 10, max: 360}\n",
                "Minecraft scales go from");
        assertError("stats:\n  h:\n    baseline: 180\n    scale: {normal: 180, min: 80, max: 3000}\n",
                "Minecraft scales go from");
        assertError("stats:\n  h:\n    scale: {normal: 180, min: 80, max: 360}\n",
                "baseline must be between scale.min and scale.max");
        assertError("stats:\n  h:\n    baseline: 180\n    scale: 2\n", "must be a section");
    }
}
