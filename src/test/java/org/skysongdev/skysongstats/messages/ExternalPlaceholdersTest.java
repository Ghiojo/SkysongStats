package org.skysongdev.skysongstats.messages;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalPlaceholdersTest {
    // Stands in for PlaceholderAPI: knows a few placeholders, leaves the rest as they are.
    private static final Map<String, String> KNOWN = Map.of(
            "%vault_eco_balance%", "1500",
            "%luckperms_prefix%", "<gold>[Noble]",
            "%legacy_section%", "§aGreen",
            "%legacy_ampersand%", "&cRed",
            "%legacy_hex%", "&#ff8800Orange");

    private final List<OfflinePlayer> askedFor = new ArrayList<>();
    private final BiFunction<OfflinePlayer, String, String> lookup = (player, token) -> {
        askedFor.add(player);
        return KNOWN.getOrDefault(token, token);
    };
    private final ExternalPlaceholders placeholders = new ExternalPlaceholders(lookup);

    @Test
    void fillsInKnownPlaceholders() {
        assertEquals("Money: 1500 coins", placeholders.fill(null, "Money: %vault_eco_balance% coins"));
    }

    @Test
    void leavesUnknownPlaceholdersAsWritten() {
        assertEquals("%nobody_knows% 1500", placeholders.fill(null, "%nobody_knows% %vault_eco_balance%"));
    }

    @Test
    void leavesPercentSignsInPlainTextAlone() {
        assertEquals("50% of 100% done", placeholders.fill(null, "50% of 100% done"));
    }

    @Test
    void keepsMiniMessageValues() {
        assertEquals("<gold>[Noble] Steve", placeholders.fill(null, "%luckperms_prefix% Steve"));
    }

    @Test
    void turnsLegacyColourCodesIntoMiniMessage() {
        assertEquals("<green>Green", placeholders.fill(null, "%legacy_section%"));
        assertEquals("<red>Red", placeholders.fill(null, "%legacy_ampersand%"));
        assertEquals("<#FF8800>Orange", placeholders.fill(null, "%legacy_hex%"));
    }

    @Test
    void asksForTheGivenPlayer() {
        OfflinePlayer steve = TestPlayers.offline("Steve");
        placeholders.fill(steve, "%vault_eco_balance%");
        assertEquals(List.of(steve), askedFor);
    }

    @Test
    void noneLeavesLinesUnchanged() {
        String line = "%vault_eco_balance%";
        assertEquals(line, ExternalPlaceholders.NONE.fill(null, line));
    }
}
