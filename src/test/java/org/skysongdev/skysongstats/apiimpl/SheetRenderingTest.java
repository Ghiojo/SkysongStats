package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.CharacterSheets;
import org.skysongdev.skysongstats.api.SheetLayout;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.character.AfflictionFixtures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * sheets().render of the running plugin, for an NPC plugin's character, with the default
 * messages.yml and the shared affliction fixtures.
 */
class SheetRenderingTest {
    private static final String GUARD_NAME = "Town Guard";

    private SkysongStatsApi api;
    private CharacterSheets sheets;
    private CharacterId guard;

    @BeforeEach
    void setUp() throws IOException {
        MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        Files.writeString(new File(plugin.getDataFolder(), "conditions.yml").toPath(), AfflictionFixtures.CONDITIONS_FILE);
        Files.writeString(new File(plugin.getDataFolder(), "injuries.yml").toPath(), AfflictionFixtures.INJURIES_FILE);
        assertEquals(List.of(), plugin.reloadSettings());
        api = SkysongStatsApi.get();
        sheets = api.sheets();
        Plugin npcs = MockBukkit.createMockPlugin("SkysongNPCs");
        guard = new CharacterId(UUID.randomUUID(), "npc");
        api.characters().create(npcs, guard, Map.of(api.stats().find("str").orElseThrow(), 4,
                api.stats().find("con").orElseThrow(), 2));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(List<Component> lines) {
        return String.join("\n", lines.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList());
    }

    private String rendered(SheetLayout layout) {
        return plain(sheets.render(guard, layout, GUARD_NAME).orElseThrow());
    }

    @Test
    void theStatSheetShowsTheCharactersStatsUnderTheGivenName() {
        String sheet = rendered(SheetLayout.STATS);

        assertTrue(sheet.contains(GUARD_NAME), sheet);
        assertTrue(sheet.contains("80"), sheet);
        assertTrue(sheet.contains("4"), sheet);
    }

    @Test
    void theStatSheetHasTheSameLinesAsTheCommandsLayout() {
        CharacterId other = new CharacterId(UUID.randomUUID(), "npc");
        api.characters().create(MockBukkit.createMockPlugin("Shops"), other);

        List<Component> lines = sheets.render(other, SheetLayout.STATS, GUARD_NAME).orElseThrow();

        assertEquals(sheets.render(guard, SheetLayout.STATS, GUARD_NAME).orElseThrow().size(), lines.size());
    }

    @Test
    void theCharacterSheetShowsItsFields() {
        api.injuries().award(guard, "broken-leg");

        String sheet = rendered(SheetLayout.CHARACTER);

        assertTrue(sheet.contains("[Not Set]"), sheet);
        assertFalse(sheet.contains("Broken Leg"), "afflictions are on the stat sheet: " + sheet);
    }

    @Test
    void theStatSheetShowsTheAfflictions() {
        api.injuries().award(guard, "broken-leg");

        String sheet = rendered(SheetLayout.STATS);

        assertTrue(sheet.contains("Injuries: [Broken Leg]"), sheet);
        assertTrue(sheet.contains("Conditions: [Weakened], [Dazed 2]"), sheet);
    }

    @Test
    void theProficiencySheetRenders() {
        String sheet = rendered(SheetLayout.PROFICIENCIES);

        assertTrue(sheet.contains(GUARD_NAME), sheet);
        assertFalse(sheet.contains("{proficiencies}"), sheet);
    }

    @Test
    void aPluginsOwnLinesGetTheSamePlaceholders() {
        api.injuries().award(guard, "bruise");

        List<Component> lines = sheets.render(guard, List.of(
                "{player}: Str {str} (base {str_base}), HP {hp}/{max_hp}",
                "<red>{injuries}</red> {name}",
                "{focus}"), GUARD_NAME).orElseThrow();

        assertEquals(3, lines.size());
        assertEquals("Town Guard: Str 4 (base 4), HP 80/80", PlainTextComponentSerializer.plainText().serialize(lines.get(0)));
        assertEquals("Bruise [Not Set]", PlainTextComponentSerializer.plainText().serialize(lines.get(1)));
        assertFalse(plain(lines).contains("{focus}"));
    }

    @Test
    void placeholderApiPlaceholdersAreLeftAsWritten() {
        List<Component> lines = sheets.render(guard, List.of("%vault_eco_balance%"), GUARD_NAME).orElseThrow();

        assertEquals("%vault_eco_balance%", plain(lines));
    }

    @Test
    void theNameIsShownAsTextNotFormatting() {
        List<Component> lines = sheets.render(guard, List.of("{player}"), "<red>Guard").orElseThrow();

        assertEquals("<red>Guard", plain(lines));
    }

    @Test
    void aCharacterThatDoesNotExistRendersNothing() {
        CharacterId nobody = new CharacterId(UUID.randomUUID(), "npc");

        assertEquals(Optional.empty(), sheets.render(nobody, SheetLayout.STATS, GUARD_NAME));
        assertEquals(Optional.empty(), sheets.render(nobody, List.of("{str}"), GUARD_NAME));
    }

    @Test
    void callsFromAnotherThreadAreRefused() {
        CharacterSheetsImpl offThread = new CharacterSheetsImpl((uuid, profile) -> null, profile -> { },
                new SheetRenderer() {
                    @Override
                    public Optional<List<Component>> render(org.skysongdev.skysongstats.storage.ProfileKey character,
                                                            SheetLayout layout, String name) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<List<Component>> render(org.skysongdev.skysongstats.storage.ProfileKey character,
                                                            List<String> lines, String name) {
                        return Optional.empty();
                    }
                }, new MainThread(() -> false), character -> false);
        List<String> lines = List.of("{str}");

        assertThrows(IllegalStateException.class, () -> offThread.render(guard, SheetLayout.STATS, GUARD_NAME));
        assertThrows(IllegalStateException.class, () -> offThread.render(guard, lines, GUARD_NAME));
    }
}
