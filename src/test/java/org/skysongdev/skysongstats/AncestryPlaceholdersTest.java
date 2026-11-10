package org.skysongdev.skysongstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.messages.AncestryPlaceholders;
import org.skysongdev.skysongstats.messages.AncestrySheetTags;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ancestry placeholders and sheet tags with the shipped ancestries.yml and
 * messages.yml, for Alice's "Default" character.
 */
class AncestryPlaceholdersTest {
    private SkysongStats plugin;
    private PlayerMock alice;
    private ProfileKey character;
    private AncestryPlaceholders placeholders;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        alice = server.addPlayer("Alice");
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(alice.getUniqueId().toString(), "Default")));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(character);
        placeholders = plugin.getAncestryPlaceholders();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private AncestryDefinition ancestry(String id) {
        return plugin.getAncestryDefinitions().get(id).orElseThrow();
    }

    private String resolve(String name) {
        return placeholders.resolve(character, name).orElseThrow();
    }

    @Test
    void withoutAnAncestry() {
        assertEquals("", resolve("ancestry"));
        assertEquals("<gray>None</gray>", resolve("ancestry_name"));
        assertEquals("", resolve("ancestry_id"));
        assertEquals("false", resolve("ancestry_pending"));
        assertEquals("<gray>None</gray>", resolve("ancestry_adjustments"));
        assertEquals("0", resolve("ancestry_con"));
    }

    @Test
    void withAnAncestry() {
        plugin.getAncestryService().set(character, ancestry("dwarf"), Optional.of(AncestryPicks.none()));

        assertEquals("Dwarf", resolve("ancestry"));
        assertEquals("Dwarf", resolve("ANCESTRY_NAME"));
        assertEquals("dwarf", resolve("ancestry_id"));
        assertEquals("false", resolve("ancestry_pending"));
        assertEquals("+1 Constitution<gray>, </gray>-1 Speed", resolve("ancestry_adjustments"));
        assertEquals("1", resolve("ancestry_con"));
        assertEquals("-1", resolve("ancestry_spd"));
        assertEquals("0", resolve("ancestry_str"));
        assertEquals("1", resolve("ancestry_constitution"), "by alias too");
    }

    @Test
    void choicesAddUpPerStat() {
        // Half-giant: str +1, and Mighty +1 that may stack on str.
        AncestryDefinition giant = ancestry("giant");
        plugin.getAncestryService().set(character, giant, Optional.of(AncestryPicks.none().withStats(0, List.of("str"))));

        assertEquals("2", resolve("ancestry_str"));
    }

    @Test
    void pendingChoicesAreShown() {
        plugin.getAncestryService().set(character, ancestry("goblin"), Optional.empty());

        assertEquals("true", resolve("ancestry_pending"));
        assertEquals("+1 Dexterity <yellow>(choices to make: /ancestry choose)</yellow>", resolve("ancestry_adjustments"));
    }

    @Test
    void anUnknownStatOrOtherNameIsNotAnAncestryPlaceholder() {
        assertTrue(placeholders.resolve(character, "ancestry_nonsense").isEmpty());
        assertTrue(placeholders.resolve(character, "injuries").isEmpty());
        assertFalse(AncestryPlaceholders.isAncestryName("ancestryx"));
    }

    @Test
    void sheetTagsUseTheSameNamesButNotThePlainAncestry() {
        plugin.getAncestryService().set(character, ancestry("dwarf"), Optional.of(AncestryPicks.none()));
        AncestrySheetTags tags = new AncestrySheetTags(placeholders, player -> Optional.of(character));

        Map<String, Component> found =
                tags.tags(alice, "{ancestry} {ancestry_name}: {ancestry_adjustments} ({ancestry_con})");

        assertEquals(List.of("ancestry_adjustments", "ancestry_con", "ancestry_name"),
                found.keySet().stream().sorted().toList());
        assertEquals("Dwarf", PlainTextComponentSerializer.plainText().serialize(found.get("ancestry_name")));
        assertEquals("+1 Constitution, -1 Speed",
                PlainTextComponentSerializer.plainText().serialize(found.get("ancestry_adjustments")));
    }

    @Test
    void everySheetGetsTheTags() {
        plugin.getAncestryService().set(character, ancestry("dwarf"), Optional.of(AncestryPicks.none()));

        assertEquals("Dwarf gave +1", PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessages().about(alice)
                        .renderLines(List.of("{ancestry_name} gave +{ancestry_con}"), Map.of(), Map.of()).get(0)));
    }
}
