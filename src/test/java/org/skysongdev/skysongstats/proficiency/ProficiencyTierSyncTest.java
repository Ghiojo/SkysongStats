package org.skysongdev.skysongstats.proficiency;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A player's proficiency tier nodes follow their active character on join and profile switch. */
class ProficiencyTierSyncTest {
    private final List<Set<String>> granted = new ArrayList<>();
    private final Map<Player, ProfileKey> active = new HashMap<>();
    private ProficiencyTierSync sync;
    private PlayerMock alice;
    private ProfileKey main;
    private ProfileKey alt;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        alice = server.addPlayer("Alice");
        main = new ProfileKey(alice.getUniqueId(), "main");
        alt = new ProfileKey(alice.getUniqueId(), "alt");
        ProficienciesConfig config = ProficiencyTestConfig.load();
        ProficiencyTierPermissions permissions = new ProficiencyTierPermissions(
                (player, grant, revoke) -> granted.add(grant), () -> config, character -> false);
        ProficiencyService proficiencies = ProficiencyTestConfig.service(config, permissions);
        proficiencies.createCharacter(main);
        proficiencies.createCharacter(alt);
        proficiencies.setTier(alt, config.proficiencies().get("axes").orElseThrow(), config.tiers().get("adept").orElseThrow());
        sync = new ProficiencyTierSync(permissions, proficiencies, player -> Optional.ofNullable(active.get(player)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joiningGivesTheActiveCharactersTiers() {
        active.put(alice, alt);
        sync.onJoin(new PlayerJoinEvent(alice, Component.empty()));
        assertEquals(List.of(Set.of("skysongstats.proficiency.axes.proficient", "skysongstats.proficiency.axes.adept")),
                granted);
    }

    @Test
    void switchingProfileGivesTheNewCharactersTiers() {
        active.put(alice, main);
        sync.onProfileSwitch(new ProfileUpdateEvent(alice, alice.getUniqueId().toString(), "main"));
        assertEquals(List.of(Set.<String>of()), granted);
    }

    @Test
    void aPlayerWithoutACharacterIsLeftAlone() {
        sync.sync(alice);
        active.put(alice, new ProfileKey(alice.getUniqueId(), "missing"));
        sync.sync(alice);
        assertTrue(granted.isEmpty());
    }
}
