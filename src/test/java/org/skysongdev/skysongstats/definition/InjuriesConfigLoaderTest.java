package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.STATS;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.assertHasError;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.yaml;

/**
 * Plain JUnit tests (no server needed) for {@link InjuriesConfigLoader} and
 * {@link InjuryDefinitions}.
 */
class InjuriesConfigLoaderTest {
    private static final ConditionDefinitions CONDITIONS = ConditionsConfigLoader.load(
            yaml("conditions:\n  bleeding: {}\n  weakened: {}\n"), STATS, name -> true);
    private static final String SEVERITIES = "severities: [minor, moderate, severe, permanent]\n";

    private static InjuryDefinitions load(String text) {
        return InjuriesConfigLoader.load(yaml(SEVERITIES + text), STATS, CONDITIONS, name -> true);
    }

    private static List<String> errors(String text) {
        return assertThrows(DefinitionException.class, () -> load(text)).getErrors();
    }

    private static InjuryDefinition only(String text) {
        return load(text).all().getFirst();
    }

    @Test
    void readsAnInjuryWithEverySetting() {
        InjuryDefinition leg = load("""
                injuries:
                  broken-leg:
                    name: "<red>Broken Leg"
                    description: "The bone is snapped below the knee."
                    icon: BONE
                    severity: Severe
                    message: "{character} feels their leg buckle, they now have a {injury}"
                    effects:
                      modifiers: "dex-2, max_hp-5"
                      conditions: [weakened, "bleeding:2"]
                """).get("broken-leg").orElseThrow();
        assertEquals(new InjuryDefinition("broken-leg", "<red>Broken Leg", "The bone is snapped below the knee.",
                Icon.parse("BONE"), "severe", "{character} feels their leg buckle, they now have a {injury}",
                List.of(new EffectModifier("dex", -2, false), new EffectModifier("max_hp", -5, false)),
                Map.of("weakened", 1, "bleeding", 2)), leg);
    }

    @Test
    void aPlainInjuryGetsDefaultsAndNoEffects() {
        InjuryDefinition ribs = only("injuries:\n  bruised-ribs: {severity: minor}\n");
        assertEquals("bruised-ribs", ribs.name());
        assertEquals("", ribs.description());
        assertEquals("", ribs.message());
        assertEquals(Icon.parse(ConditionsConfigLoader.DEFAULT_ICON), ribs.icon());
        assertEquals(List.of(), ribs.modifiers());
        assertEquals(Map.of(), ribs.conditions());
    }

    @Test
    void anEmptyFileHasNoInjuriesButKeepsItsSeverities() {
        InjuryDefinitions injuries = load("injuries: {}\n");
        assertEquals(List.of(), injuries.all());
        assertEquals(List.of("minor", "moderate", "severe", "permanent"), injuries.severities());
    }

    @Test
    void notEverySeverityNeedsInjuries() {
        InjuryDefinitions injuries = load("injuries:\n  burn: {severity: severe}\n  cut: {severity: minor}\n"
                + "  scrape: {severity: minor}\n");
        assertEquals(List.of("minor", "severe"), injuries.usedSeverities());
        assertEquals(List.of("cut", "scrape"),
                injuries.ofSeverity("MINOR").stream().map(InjuryDefinition::id).toList());
        assertEquals(List.of(), injuries.ofSeverity("permanent"));
    }

    @Test
    void underscoresAreFineInInjuryIds() {
        assertEquals("deep_cut", only("injuries:\n  deep_cut: {severity: minor}\n").id());
    }

    @Test
    void aMissingSeverityIsAnError() {
        assertHasError(errors("injuries:\n  cut: {}\n"), "injuries.cut.severity: missing, one of [minor");
    }

    @Test
    void anUnknownSeverityIsAnError() {
        assertHasError(errors("injuries:\n  cut: {severity: deadly}\n"), "'deadly' is not in severities");
    }

    @Test
    void severitiesAreRequiredAndUnique() {
        assertHasError(assertThrows(DefinitionException.class, () -> InjuriesConfigLoader.load(
                yaml("injuries: {}\n"), STATS, CONDITIONS, name -> true)).getErrors(), "severities: missing");
        assertHasError(assertThrows(DefinitionException.class, () -> InjuriesConfigLoader.load(
                yaml("severities: [minor, Minor]\ninjuries: {}\n"), STATS, CONDITIONS, name -> true)).getErrors(),
                "'minor' is listed twice");
        assertHasError(assertThrows(DefinitionException.class, () -> InjuriesConfigLoader.load(
                yaml("severities: minor\ninjuries: {}\n"), STATS, CONDITIONS, name -> true)).getErrors(),
                "severities: must be a list");
    }

    @Test
    void anUnknownConditionIsAnError() {
        assertHasError(errors("injuries:\n  cut: {severity: minor, effects: {conditions: [poisoned]}}\n"),
                "injuries.cut.effects.conditions: 'poisoned' is not a condition id in conditions.yml");
    }

    @Test
    void aBadConditionLevelIsAnError() {
        List<String> errors = errors("injuries:\n  cut: {severity: minor, effects: {conditions: [\"bleeding:0\"]}}\n"
                + "  gash: {severity: minor, effects: {conditions: [\"bleeding:lots\"]}}\n");
        assertHasError(errors, "'bleeding:0': the level after ':' must be a whole number of 1 or more");
        assertHasError(errors, "'bleeding:lots': the level after ':'");
    }

    @Test
    void aConditionListedTwiceIsAnError() {
        assertHasError(errors("injuries:\n  cut: {severity: minor, effects: {conditions: [bleeding, \"bleeding:2\"]}}\n"),
                "'bleeding' is listed twice");
    }

    @Test
    void conditionsMustBeAList() {
        assertHasError(errors("injuries:\n  cut: {severity: minor, effects: {conditions: bleeding}}\n"),
                "injuries.cut.effects.conditions: must be a list");
    }

    @Test
    void injuriesHaveNoLevelForModifiers() {
        assertHasError(errors("injuries:\n  cut: {severity: minor, effects: {modifiers: \"con-$\"}}\n"),
                "$ (the level) only works in conditions.yml");
    }

    @Test
    void anUnknownEffectIsAnError() {
        assertHasError(errors("injuries:\n  cut: {severity: minor, effects: {modifier: \"str-1\"}}\n"),
                "injuries.cut.effects.modifier: unknown effect");
    }

    @Test
    void aBadIdIsAnError() {
        assertHasError(errors("injuries:\n  Broken Leg: {severity: minor}\n"), "ids may only use lowercase letters");
    }

    @Test
    void aMissingSectionIsAnError() {
        assertHasError(errors("other: {}\n"), "injuries: missing");
    }
}
