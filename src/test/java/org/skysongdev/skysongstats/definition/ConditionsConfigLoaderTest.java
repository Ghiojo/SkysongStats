package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.STATS;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.assertHasError;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.yaml;

/**
 * Plain JUnit tests (no server needed) for {@link ConditionsConfigLoader}, the
 * effect modifiers it reads with {@link EffectModifierParser}, and
 * {@link ConditionDefinitions}.
 */
class ConditionsConfigLoaderTest {

    private static ConditionDefinitions load(String text) {
        return ConditionsConfigLoader.load(yaml(text), STATS, name -> true);
    }

    private static List<String> errors(String text) {
        return assertThrows(DefinitionException.class, () -> load(text)).getErrors();
    }

    private static ConditionDefinition only(String text) {
        return load(text).all().getFirst();
    }

    @Test
    void readsAConditionWithEverySetting() {
        ConditionDefinition bleeding = load("""
                conditions:
                  bleeding:
                    name: "<dark_red>Bleeding"
                    description: "Loses $ HP every turn."
                    icon: REDSTONE
                    effects:
                      modifiers: "con-$, str+1"
                """).get("Bleeding").orElseThrow();
        assertEquals(new ConditionDefinition("bleeding", "<dark_red>Bleeding", "Loses $ HP every turn.",
                Icon.parse("REDSTONE"), List.of(new EffectModifier("con", -1, true), new EffectModifier("str", 1, false)),
                Map.of()), bleeding);
    }

    @Test
    void aPlainConditionGetsDefaultsAndNoEffects() {
        ConditionDefinition tired = only("conditions:\n  tired: {}\n");
        assertEquals("tired", tired.name());
        assertEquals("", tired.description());
        assertEquals(Icon.parse(ConditionsConfigLoader.DEFAULT_ICON), tired.icon());
        assertEquals(List.of(), tired.modifiers());
        assertEquals(Map.of(), tired.conditions());
    }

    @Test
    void anEmptyFileHasNoConditions() {
        assertEquals(List.of(), load("conditions: {}\n").all());
    }

    @Test
    void conditionsKeepFileOrder() {
        List<String> ids = load("conditions:\n  b: {}\n  a: {}\n  c: {}\n").all().stream()
                .map(ConditionDefinition::id).toList();
        assertEquals(List.of("b", "a", "c"), ids);
    }

    @Test
    void anUnknownIdIsNotFound() {
        assertTrue(load("conditions:\n  tired: {}\n").get("bleeding").isEmpty());
    }

    @Test
    void modifiersCanBeAList() {
        assertEquals(List.of(new EffectModifier("str", -1, false), new EffectModifier("dex", 2, false)),
                only("conditions:\n  weak: {effects: {modifiers: [str-1, \"dex + 2\"]}}\n").modifiers());
    }

    @Test
    void anAliasBecomesTheStatsId() {
        assertEquals("str", only("conditions:\n  weak: {effects: {modifiers: \"Strength-1\"}}\n")
                .modifiers().getFirst().target());
    }

    @Test
    void aBarsMaxIsATarget() {
        assertEquals(new EffectModifier("max_hp", -5, false),
                only("conditions:\n  frail: {effects: {modifiers: \"max_hp-5\"}}\n").modifiers().getFirst());
    }

    @Test
    void aStatIdWithADashStillSplitsAtTheSign() {
        assertEquals(new EffectModifier("temp-hp", -3, false),
                only("conditions:\n  drained: {effects: {modifiers: \"temp-hp-3\"}}\n").modifiers().getFirst());
    }

    @Test
    void aPerLevelModifierScalesWithTheLevel() {
        EffectModifier modifier = only("conditions:\n  bleeding: {effects: {modifiers: \"con-$\"}}\n")
                .modifiers().getFirst();
        assertEquals(-3, modifier.amountAt(3));
        assertEquals(-2, new EffectModifier("con", -2, false).amountAt(3));
    }

    @Test
    void anUnknownStatIsAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: \"luck-1\"}}\n"),
                "conditions.weak.effects.modifiers: 'luck-1': 'luck' is not a stat id or alias");
    }

    @Test
    void aMaxOfANonBarIsAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: \"max_str-1\"}}\n"),
                "'max_str' is not a stat id or alias");
    }

    @Test
    void anEntryWithoutASignIsAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: \"str1, dex\"}}\n"),
                "'str1' must be <stat><+ or -><amount>");
    }

    @Test
    void anAmountOfZeroIsAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: \"str+0\"}}\n"), "an amount of 0 does nothing");
    }

    @Test
    void aHugeAmountIsAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: \"str+99999999999\"}}\n"), "too big");
    }

    @Test
    void modifiersThatAreNeitherTextNorAListAreAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: {modifiers: {str: 1}}}\n"),
                "conditions.weak.effects.modifiers: must be a text");
    }

    @Test
    void conditionsCantGiveInjuries() {
        assertHasError(errors("conditions:\n  weak: {effects: {injuries: [broken-leg]}}\n"),
                "conditions.weak.effects.injuries: unknown effect");
    }

    @Test
    void aConditionCanGiveOtherConditionsEvenOnesFurtherDown() {
        ConditionDefinitions conditions = load("""
                conditions:
                  bleeding: {effects: {conditions: [weakened, "dizzy:2"]}}
                  weakened: {}
                  dizzy: {}
                """);
        assertEquals(List.of("weakened", "dizzy"),
                List.copyOf(conditions.get("bleeding").orElseThrow().conditions().keySet()));
        assertEquals(2, conditions.get("bleeding").orElseThrow().conditions().get("dizzy"));
    }

    @Test
    void chainsWithoutALoopAreFine() {
        assertEquals(3, load("conditions:\n  a: {effects: {conditions: [b]}}\n  b: {effects: {conditions: [c]}}\n"
                + "  c: {}\n").all().size());
    }

    @Test
    void anUnknownGivenConditionIsAnError() {
        assertHasError(errors("conditions:\n  bleeding: {effects: {conditions: [poisoned]}}\n"),
                "conditions.bleeding.effects.conditions: 'poisoned' is not a condition id in conditions.yml");
    }

    @Test
    void conditionsGivingEachOtherInALoopAreAnError() {
        List<String> errors = errors("conditions:\n  a: {effects: {conditions: [b]}}\n  b: {effects: {conditions: [c]}}\n"
                + "  c: {effects: {conditions: [a]}}\n");
        assertEquals(List.of("conditions: a -> b -> c -> a: conditions can't give each other in a loop"), errors);
    }

    @Test
    void aConditionGivingItselfIsAnError() {
        assertHasError(errors("conditions:\n  a: {effects: {conditions: [a]}}\n"), "conditions: a -> a:");
    }

    @Test
    void effectsThatAreNotASectionAreAnError() {
        assertHasError(errors("conditions:\n  weak: {effects: \"str-1\"}\n"), "conditions.weak.effects: must be a section");
    }

    @Test
    void anUnderscoreInAnIdIsAnError() {
        assertHasError(errors("conditions:\n  deep_cut: {}\n"), "'_' separates the level in chat");
    }

    @Test
    void anUppercaseIdIsAnError() {
        assertHasError(errors("conditions:\n  Bleeding: {}\n"), "conditions.Bleeding: condition ids may only use");
    }

    @Test
    void aConditionThatIsNotASectionIsAnError() {
        assertHasError(errors("conditions:\n  bleeding: yes\n"), "conditions.bleeding: must be a section");
    }

    @Test
    void aBadIconIsAnError() {
        List<String> errors = assertThrows(DefinitionException.class, () -> ConditionsConfigLoader.load(
                yaml("conditions:\n  weak: {icon: NOT_A_THING}\n"), STATS, name -> false)).getErrors();
        assertHasError(errors, "conditions.weak.icon: 'NOT_A_THING' is not an item Material");
    }

    @Test
    void aMissingSectionIsAnError() {
        assertHasError(errors("other: {}\n"), "conditions: missing");
    }

    @Test
    void everyMistakeIsReportedAtOnce() {
        assertEquals(2, errors("conditions:\n  a: {effects: {modifiers: \"luck-1\"}}\n  b_c: {}\n").size());
    }
}
