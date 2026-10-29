package org.skysongdev.skysongstats.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping a player's skill level nodes in step with their active character.
 */
class SkillLevelPermissionsTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final ProfileKey MAIN = new ProfileKey(ALICE, "main");
    private static final ProfileKey ALT = new ProfileKey(ALICE, "alt");

    // One update() call.
    private record Update(UUID player, Set<String> grant, Set<String> revoke) {
    }

    private final List<Update> updates = new ArrayList<>();
    private final Set<ProfileKey> active = new HashSet<>(Set.of(MAIN));
    private SkillsConfig config;
    private SkillService skills;
    private SkillLevelPermissions permissions;

    @BeforeEach
    void setUp() {
        config = SkillTestConfig.load();
        permissions = new SkillLevelPermissions(
                (player, grant, revoke) -> updates.add(new Update(player, grant, revoke)), () -> config, active::contains);
        skills = new SkillService(config, new UnsavedSkillRepository(line -> { }), permissions);
        skills.createCharacter(MAIN);
        skills.createCharacter(ALT);
    }

    private SkillDefinition skill(String id) {
        return config.skills().get(id).orElseThrow();
    }

    @Test
    void reachingALevelGivesItAndTheLevelsBelow() {
        skills.setXp(MAIN, skill("tande"), 40);
        assertEquals(List.of(new Update(ALICE,
                Set.of("skysongstats.skill.tande.novice", "skysongstats.skill.tande.competent"),
                Set.of("skysongstats.skill.tande.expert"))), updates);
    }

    @Test
    void droppingBelowEveryLevelTakesAllTheSkillsNodes() {
        skills.setXp(MAIN, skill("tande"), 200);
        skills.setXp(MAIN, skill("tande"), 3);
        Update last = updates.getLast();
        assertTrue(last.grant().isEmpty());
        assertEquals(SkillPermissions.all(skill("tande"), config.levels()), last.revoke());
    }

    @Test
    void onlyTheChangedSkillsNodesAreTouched() {
        skills.setXp(MAIN, skill("farming"), 16);
        Update update = updates.getFirst();
        assertTrue(update.grant().stream().allMatch(node -> node.startsWith("skysongstats.skill.farming.")));
        assertTrue(update.revoke().stream().allMatch(node -> node.startsWith("skysongstats.skill.farming.")));
    }

    @Test
    void changesToACharacterThePlayerIsNotPlayingLeaveTheNodesAlone() {
        skills.setXp(ALT, skill("tande"), 128);
        assertTrue(updates.isEmpty());
    }

    @Test
    void syncSetsEverySkillToTheCharactersLevels() {
        skills.setXp(ALT, skill("tande"), 128);
        skills.setXp(ALT, skill("farming"), 16);
        permissions.sync(ALT, skills);
        assertEquals(List.of(new Update(ALICE,
                Set.of("skysongstats.skill.farming.novice", "skysongstats.skill.tande.novice",
                        "skysongstats.skill.tande.competent", "skysongstats.skill.tande.expert"),
                Set.of("skysongstats.skill.farming.competent", "skysongstats.skill.farming.expert"))), updates);
    }

    @Test
    void syncUsesTheReloadedLevels() {
        skills.setXp(MAIN, skill("farming"), 20);
        updates.clear();
        config = SkillTestConfig.load(SkillTestConfig.SKILLS.replace("novice: {name: Novice, xp: 16}", "novice: {name: Novice, xp: 30}"));
        skills.setConfig(config);
        permissions.sync(MAIN, skills);
        assertTrue(updates.getFirst().revoke().contains("skysongstats.skill.farming.novice"));
        assertTrue(updates.getFirst().grant().isEmpty());
    }
}
