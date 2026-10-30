package org.skysongdev.skysongstats.placeholders;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;

import static org.junit.jupiter.api.Assertions.assertNull;

/** PlaceholderAPI asks without a player for offline players and player-less text. */
class StatsExpansionTest {
    private StatsExpansion expansion;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
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
}
