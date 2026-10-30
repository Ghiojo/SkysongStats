package org.skysongdev.skysongstats.skill;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A player's skill level nodes follow their active character on join and profile switch. */
class SkillLevelSyncTest {
    private final List<Set<String>> granted = new ArrayList<>();
    private final Map<Player, ProfileKey> active = new HashMap<>();
    private SkillsConfig config;
    private SkillService skills;
    private SkillLevelSync sync;
    private PlayerMock alice;
    private ProfileKey main;
    private ProfileKey alt;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        alice = server.addPlayer("Alice");
        main = new ProfileKey(alice.getUniqueId(), "main");
        alt = new ProfileKey(alice.getUniqueId(), "alt");
        config = SkillTestConfig.load();
        SkillLevelPermissions permissions = new SkillLevelPermissions(
                (player, grant, revoke) -> granted.add(grant), () -> config, character -> false);
        skills = new SkillService(config, new UnsavedSkillRepository(line -> { }), permissions);
        skills.createCharacter(main);
        skills.createCharacter(alt);
        skills.setXp(alt, config.skills().get("farming").orElseThrow(), 40);
        sync = new SkillLevelSync(permissions, skills, player -> Optional.ofNullable(active.get(player)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joiningGivesTheActiveCharactersLevels() {
        active.put(alice, alt);
        sync.onJoin(new PlayerJoinEvent(alice, Component.empty()));
        assertEquals(List.of(Set.of("skysongstats.skill.farming.novice", "skysongstats.skill.farming.competent")),
                granted);
    }

    @Test
    void switchingProfileGivesTheNewCharactersLevels() {
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
