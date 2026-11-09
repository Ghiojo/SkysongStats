package org.skysongdev.skysongstats.placeholders;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** PlaceholderAPI asks without a player for offline players and player-less text. */
class StatsExpansionTest {
    private StatsExpansion expansion;
    private SkysongStats plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        expansion = new StatsExpansion(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void statPlaceholderWithoutAPlayerStaysUnresolved() {
        assertNull(expansion.onPlaceholderRequest(null, "hp"));
    }

    @Test
    void downtimePlaceholderWithoutAPlayerStaysUnresolved() {
        assertNull(expansion.onPlaceholderRequest(null, "character_downtime"));
    }

    @Test
    void characterPlaceholderWithoutAPlayerStaysUnresolved() {
        assertNull(expansion.onPlaceholderRequest(null, "character_name"));
    }

    @Test
    void proficiencyPlaceholderWithoutAPlayerStaysUnresolved() {
        assertNull(expansion.onPlaceholderRequest(null, "proficiency_axes"));
    }

    @Test
    void injuryAndConditionListsAreForTheActiveCharacter() {
        PlayerMock alice = MockBukkit.getMock().addPlayer("Alice");
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(alice.getUniqueId().toString(), "Default")));
        assertEquals("<gray>None</gray>", expansion.onPlaceholderRequest(alice, "injuries_hover_50"));
        assertEquals("0", expansion.onPlaceholderRequest(alice, "conditions_count"));
        assertNull(expansion.onPlaceholderRequest(null, "injuries"));
    }
}
