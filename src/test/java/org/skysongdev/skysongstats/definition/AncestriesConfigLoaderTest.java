package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.assertHasError;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.yaml;

/**
 * Plain JUnit tests (no server needed) for {@link AncestriesConfigLoader} and
 * {@link AncestryDefinitions}.
 */
class AncestriesConfigLoaderTest {
    // str, dex and con are /setup stats; hp and luck are not.
    private static final StatDefinitions STATS = StatsConfigLoader.load(yaml("""
            stats:
              str: {aliases: [strength], setup: {icon: IRON_SWORD, slot: 9, min: -3, max: 4}}
              dex: {setup: {icon: BOW, slot: 11, min: -3, max: 4}}
              con: {setup: {icon: APPLE, slot: 13, min: -3, max: 4}}
              luck: {}
              hp: {type: bar, max: "60 + {con} * 10", start: max}
            """), name -> true).stats();

    private static AncestryDefinitions load(String text) {
        return AncestriesConfigLoader.load(yaml(text), STATS, name -> true);
    }

    private static List<String> errors(String text) {
        return assertThrows(DefinitionException.class, () -> load(text)).getErrors();
    }

    // An ancestry in slot 10 with the given extra settings.
    private static String ancestry(String settings) {
        return "ancestries:\n  elf:\n    icon: BOW\n    slot: 10\n" + settings.indent(4);
    }

    private static AncestryDefinition only(String settings) {
        return load(ancestry(settings)).get("elf").orElseThrow();
    }

    @Test
    void readsAnAncestryWithEverySetting() {
        AncestryDefinition elf = only("""
                name: "<green>Elf"
                lore: ["<gray>Long-lived."]
                permission: skysong.ancestry.elf
                stats: {dex: 1, strength: -1}
                choices:
                  - name: "Grace"
                    amount: 1
                    picks: 1
                    from: [dex, con]
                    stack: true
                  - name: "Upbringing"
                    options:
                      - {name: "Brawler", icon: IRON_AXE, stats: {str: 2}}
                      - {name: "Runner", stats: {con: 1, str: 1}}
                """);
        assertEquals(new AncestryDefinition("elf", "<green>Elf", Icon.parse("BOW"), 10, List.of("<gray>Long-lived."),
                Optional.of("skysong.ancestry.elf"),
                List.of(new AncestryAdjustment("dex", 1), new AncestryAdjustment("str", -1)),
                List.of(new StatPickChoice("Grace", 1, 1, List.of("dex", "con"), false, true),
                        new PackageChoice("Upbringing", List.of(
                                new AncestryPackage("Brawler", Icon.parse("IRON_AXE"),
                                        List.of(new AncestryAdjustment("str", 2))),
                                new AncestryPackage("Runner", Icon.parse("PAPER"),
                                        List.of(new AncestryAdjustment("con", 1), new AncestryAdjustment("str", 1)))),
                                false))), elf);
        assertTrue(elf.hasChoices());
        assertEquals(-1, elf.fixedAmount("str"));
        assertEquals(0, elf.fixedAmount("con"));
    }

    @Test
    void aPlainAncestryGetsDefaults() {
        AncestryDefinition elf = only("");
        assertEquals("elf", elf.name());
        assertEquals(List.of(), elf.lore());
        assertEquals(Optional.empty(), elf.permission());
        assertEquals(List.of(), elf.adjustments());
        assertFalse(elf.hasChoices());
    }

    @Test
    void fromAnyMeansEverySetupStatInStatsOrder() {
        StatPickChoice choice = (StatPickChoice) only("choices: [{amount: 1, picks: 2, from: ANY}]").choices().getFirst();
        assertEquals(List.of("str", "dex", "con"), choice.from());
        assertTrue(choice.anyStat());
        assertEquals(2, choice.picks());
        assertEquals("", choice.name());
    }

    @Test
    void picksDefaultsToOne() {
        StatPickChoice choice = (StatPickChoice) only("choices: [{amount: -1, from: [con]}]").choices().getFirst();
        assertEquals(1, choice.picks());
        assertFalse(choice.isBoost());
    }

    @Test
    void anEmptyFileHasNoAncestries() {
        AncestryDefinitions ancestries = load("ancestries: {}\n");
        assertTrue(ancestries.isEmpty());
        assertEquals(List.of(), ancestries.all());
        assertTrue(AncestryDefinitions.none().isEmpty());
    }

    @Test
    void ancestriesKeepFileOrderAndAreFoundIgnoringCase() {
        AncestryDefinitions ancestries = load("""
                ancestries:
                  orc: {icon: IRON_AXE, slot: 12}
                  dwarf: {icon: IRON_PICKAXE, slot: 11}
                """);
        assertEquals(List.of("orc", "dwarf"), ancestries.all().stream().map(AncestryDefinition::id).toList());
        assertTrue(ancestries.get(" DWARF ").isPresent());
        assertTrue(ancestries.get("elf").isEmpty());
        assertTrue(ancestries.get(null).isEmpty());
    }

    @Test
    void theAncestriesSectionIsRequired() {
        assertHasError(errors("{}\n"), "ancestries: missing");
    }

    @Test
    void badIdsIconsAndSlotsAreReported() {
        List<String> errors = errors("""
                ancestries:
                  Half Elf: {icon: BOW, slot: 10}
                  dwarf: {slot: 3}
                  orc: {icon: IRON_AXE, slot: 45}
                  goblin: {icon: STICK, slot: 9}
                  gnome: {icon: STICK, slot: 9}
                  imp: {icon: STICK}
                  human: "Human"
                """);
        assertHasError(errors, "ancestries.Half Elf: ids may only use");
        assertHasError(errors, "ancestries.dwarf.icon: missing");
        assertHasError(errors, "ancestries.dwarf.slot: 3 is outside the ancestry list, use 9-44");
        assertHasError(errors, "ancestries.orc.slot: 45 is outside");
        assertHasError(errors, "ancestries.gnome.slot: 9 is already used by ancestry 'goblin'");
        assertHasError(errors, "ancestries.imp.slot: missing");
        assertHasError(errors, "ancestries.human: must be a section");
    }

    @Test
    void iconsMustBeItems() {
        List<String> errors = assertThrows(DefinitionException.class, () -> AncestriesConfigLoader.load(
                yaml(ancestry("choices: [{options: [{name: A, icon: WATER, stats: {str: 1}}, {name: B, stats: {dex: 1}}]}]")),
                STATS, name -> !name.equals("WATER") && !name.equals("BOW"))).getErrors();
        assertHasError(errors, "ancestries.elf.icon: 'BOW' is not an item Material");
        assertHasError(errors, "ancestries.elf.choices[1].options[1].icon: 'WATER' is not an item Material");
    }

    @Test
    void theNameMustFitTheCharacterField() {
        assertHasError(errors(ancestry("name: \"" + "x".repeat(51) + "\"")), "is 51 characters, at most 50");
        assertHasError(errors(ancestry("name: \" \"")), "ancestries.elf.name: can't be blank");
        assertEquals("x".repeat(50), only("name: \"" + "x".repeat(50) + "\"").name());
    }

    @Test
    void aPermissionIsOneNode() {
        assertHasError(errors(ancestry("permission: \"skysong ancestry\"")), "ancestries.elf.permission:");
        assertHasError(errors(ancestry("permission: \"\"")), "ancestries.elf.permission:");
    }

    @Test
    void adjustmentsOnlyNameSetupStatsWithNonZeroAmounts() {
        List<String> errors = errors(ancestry("stats: {luck: 1, hp: 2, wis: 1, dex: 0, con: big, str: 1, strength: 2}"));
        assertHasError(errors, "ancestries.elf.stats.luck: 'luck' has no setup: section");
        assertHasError(errors, "ancestries.elf.stats.hp: 'hp' has no setup: section");
        assertHasError(errors, "ancestries.elf.stats.wis: 'wis' is not a stat");
        assertHasError(errors, "ancestries.elf.stats.dex: '0' must be a whole number other than 0");
        assertHasError(errors, "ancestries.elf.stats.con: 'big' must be a whole number");
        assertHasError(errors, "ancestries.elf.stats.strength: adjusts 'str' a second time");
        assertHasError(errors(ancestry("stats: [str]")), "ancestries.elf.stats: must be stat: amount pairs");
    }

    @Test
    void choicesMustBeAListOfAtMostFive() {
        assertHasError(errors(ancestry("choices: {amount: 1}")), "ancestries.elf.choices: must be a list");
        assertHasError(errors(ancestry("choices: [boost]")), "ancestries.elf.choices[1]: must be a choice");
        String six = "choices:\n" + "  - {amount: 1, from: [str]}\n".repeat(6);
        assertHasError(errors(ancestry(six)), "has 6 choices, at most 5");
    }

    @Test
    void statPickMistakesAreReported() {
        List<String> errors = errors(ancestry("""
                choices:
                  - {from: [str]}
                  - {amount: 0, from: [str]}
                  - {amount: 1, picks: 0, from: [str]}
                  - {amount: 1, picks: 3, from: [str, dex]}
                  - {amount: 1}
                  - {amount: 1, from: [str, strength]}
                  - {amount: 1, from: [luck]}
                  - {amount: 1, from: [str], stack: yes please}
                  - {amount: 1, from: [str], colour: red}
                """));
        assertHasError(errors, "choices[1].amount: missing");
        assertHasError(errors, "choices[2].amount: '0'");
        assertHasError(errors, "choices[3].picks: '0' must be a whole number of at least 1");
        assertHasError(errors, "choices[4].picks: 3 is more than the 2 stats it picks from");
        assertHasError(errors, "choices[5].from: missing, must be 'any' or a list");
        assertHasError(errors, "choices[6].from: lists 'str' twice");
        assertHasError(errors, "choices[7].from: 'luck' has no setup: section");
        assertHasError(errors, "choices[8].stack: 'yes please' must be true or false");
        assertHasError(errors, "choices[9].colour: unknown setting");
    }

    @Test
    void aChoiceRowFitsEightStats() {
        StringBuilder stats = new StringBuilder("stats:\n");
        for (int i = 0; i < 9; i++) {
            stats.append("  s").append(i).append(": {setup: {icon: STONE, slot: ").append(i + 9).append(", min: -3, max: 4}}\n");
        }
        StatDefinitions nine = StatsConfigLoader.load(yaml(stats.toString()), name -> true).stats();
        List<String> errors = assertThrows(DefinitionException.class, () -> AncestriesConfigLoader.load(
                yaml(ancestry("choices: [{amount: 1, from: any}]")), nine, name -> true)).getErrors();
        assertHasError(errors, "choices[1].from: has 9 stats, at most 8 fit a row");
    }

    @Test
    void withoutStackAChoiceMustStillBeCompletable() {
        assertHasError(errors(ancestry("stats: {str: 1}\nchoices: [{amount: 1, from: [str]}]")),
                "choices[1]: can never be completed, only 0 of its stats aren't already raised");
        assertHasError(errors(ancestry("stats: {str: -1, dex: -1}\nchoices: [{amount: -2, picks: 2, from: any}]")),
                "only 1 of its stats aren't already lowered");
        // The other direction, a free stat left, or stack: true are all fine.
        assertEquals(1, only("stats: {str: 1}\nchoices: [{amount: -1, from: [str]}]").choices().size());
        assertEquals(1, only("stats: {str: 1}\nchoices: [{amount: 1, from: [str, dex]}]").choices().size());
        assertEquals(1, only("stats: {str: 1}\nchoices: [{amount: 1, from: [str], stack: true}]").choices().size());
    }

    @Test
    void packageMistakesAreReported() {
        List<String> errors = errors(ancestry("""
                stats: {str: 1}
                choices:
                  - {options: [{name: A, stats: {dex: 1}}]}
                  - {options: [{name: A, stats: {dex: 1}}, "B"], amount: 1}
                  - options:
                      - {stats: {dex: 1}}
                      - {name: B}
                      - {name: C, stats: {}}
                      - {name: D, stats: {str: 2}}
                      - {name: E, stats: {luck: 1}, smell: nice}
                """));
        assertHasError(errors, "choices[1].options: must be a list of at least two packages");
        assertHasError(errors, "choices[2].amount: a choice with options: is a package choice");
        assertHasError(errors, "choices[2].options[2]: must be a package");
        assertHasError(errors, "choices[3].options[1].name: missing");
        assertHasError(errors, "choices[3].options[2].stats: missing");
        assertHasError(errors, "choices[3].options[4]: can never be picked, this ancestry's stats: already raise 'str'");
        assertHasError(errors, "choices[3].options[5].stats.luck: 'luck' has no setup: section");
        assertHasError(errors, "choices[3].options[5].smell: unknown setting");
    }

    @Test
    void aPackageMayStackWhenTheChoiceSaysSo() {
        PackageChoice choice = (PackageChoice) only("""
                stats: {str: 1}
                choices: [{stack: true, options: [{name: A, stats: {str: 1}}, {name: B, stats: {dex: 1}}]}]
                """).choices().getFirst();
        assertTrue(choice.stack());
        assertEquals(2, choice.options().size());
    }

    @Test
    void aPackageChoiceFitsEightOptions() {
        String options = "{name: X, stats: {dex: 1}}, ".repeat(9);
        assertHasError(errors(ancestry("choices: [{options: [" + options + "]}]")),
                "choices[1].options: has 9 packages, at most 8");
    }
}
