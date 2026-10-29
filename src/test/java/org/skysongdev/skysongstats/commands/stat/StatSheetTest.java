package org.skysongdev.skysongstats.commands.stat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.StatAction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The /stats view sheet: stats.sheet from messages.yml with the stat values filled
 * in, an automatic line for each stat the sheet doesn't mention, then the footer.
 */
class StatSheetTest {
    private StatCommandFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
        fixture.stats.apply(StatCommandFixture.character(fixture.alice),
                fixture.stats.definitions().get("str").orElseThrow(), StatAction.SET, 2);
        fixture.stats.addModifier(StatCommandFixture.character(fixture.alice), "str", 1);
        fixture.stats.addModifier(StatCommandFixture.character(fixture.alice), "max_hp", 5);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private CommandTargets.Target alice() {
        return new CommandTargets.Target(fixture.alice, StatCommandFixture.character(fixture.alice));
    }

    // Every stat of the default stats.yml, in order, so no line is automatic.
    private static List<String> everyStat() {
        return StatCommandFixture.defaultStats().all().stream().map(stat -> "{" + stat.id() + "}").toList();
    }

    @Test
    void fillsInThePlayerAndEveryStatPlaceholder() {
        fixture.messages.texts.put(StatSheet.SHEET, everyStat());
        fixture.sheet.show(fixture.bob, alice());
        StatCommandFixture.Sent sheet = fixture.messages.sent.get(0);
        assertEquals(StatSheet.SHEET, sheet.key());
        assertEquals("Bob", sheet.to());
        Map<String, String> values = sheet.placeholders();
        assertEquals("Alice", values.get("player"));
        assertEquals("3", values.get("str"));
        assertEquals("2", values.get("str_base"));
        assertEquals("1", values.get("str_modifiers"));
        assertEquals("5", values.get("max_hp_modifiers"));
        assertEquals(Integer.toString(fixture.value(fixture.alice, "max_hp")), values.get("max_hp"));
    }

    @Test
    void aSheetThatShowsEveryStatHasNoAutomaticLines() {
        fixture.messages.texts.put(StatSheet.SHEET, everyStat());
        fixture.sheet.show(fixture.alice, alice());
        assertEquals(List.of(StatSheet.SHEET, StatSheet.FOOTER), fixture.messages.keys());
    }

    @Test
    void statsTheSheetDoesNotMentionGetALineAtTheBottom() {
        List<String> lines = everyStat().stream()
                .filter(line -> !"{str}".equals(line) && !"{hp}".equals(line))
                .toList();
        fixture.messages.texts.put(StatSheet.SHEET, lines);
        fixture.sheet.show(fixture.alice, alice());
        assertEquals(List.of(StatSheet.SHEET, StatSheet.AUTO_VALUE, StatSheet.AUTO_BAR, StatSheet.FOOTER),
                fixture.messages.keys());

        Map<String, String> str = fixture.messages.sent.get(1).placeholders();
        assertEquals("str", str.get("stat"));
        assertEquals("3", str.get("value"));
        assertEquals("2", str.get("base"));
        assertEquals("1", str.get("modifiers"));

        Map<String, String> hp = fixture.messages.sent.get(2).placeholders();
        assertEquals("hp", hp.get("stat"));
        assertEquals(Integer.toString(fixture.value(fixture.alice, "max_hp")), hp.get("max"));
        assertEquals("5", hp.get("max_modifiers"));
    }

    @Test
    void anyOfAStatsPlaceholdersCountsAsShowingIt() {
        List<String> lines = everyStat().stream()
                .map(line -> "{str}".equals(line) ? "Str mods: {str_modifiers}" : line)
                .map(line -> "{hp}".equals(line) ? "<red>Max: {max_hp}" : line)
                .toList();
        fixture.messages.texts.put(StatSheet.SHEET, lines);
        fixture.sheet.show(fixture.alice, alice());
        assertEquals(List.of(StatSheet.SHEET, StatSheet.FOOTER), fixture.messages.keys());
    }

    @Test
    void anEmptySheetGetsALineForEveryStat() {
        fixture.sheet.show(fixture.alice, alice());
        int stats = fixture.stats.definitions().all().size();
        assertEquals(stats + 2, fixture.messages.sent.size());
        assertEquals("Alice", fixture.messages.sent.get(stats + 1).placeholders().get("player"));
    }
}
