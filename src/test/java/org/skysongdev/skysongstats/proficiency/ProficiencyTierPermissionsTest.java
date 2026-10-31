package org.skysongdev.skysongstats.proficiency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping a player's proficiency tier nodes in step with their active character.
 */
class ProficiencyTierPermissionsTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final ProfileKey MAIN = new ProfileKey(ALICE, "main");
    private static final ProfileKey ALT = new ProfileKey(ALICE, "alt");

    // One update() call.
    private record Update(UUID player, Set<String> grant, Set<String> revoke) {
    }

    private final List<Update> updates = new ArrayList<>();
    private final Set<ProfileKey> active = new HashSet<>(Set.of(MAIN));
    private ProficienciesConfig config;
    private ProficiencyService proficiencies;
    private ProficiencyTierPermissions permissions;

    @BeforeEach
    void setUp() {
        config = ProficiencyTestConfig.load();
        permissions = new ProficiencyTierPermissions(
                (player, grant, revoke) -> updates.add(new Update(player, grant, revoke)), () -> config, active::contains);
        proficiencies = ProficiencyTestConfig.service(config, permissions);
        proficiencies.createCharacter(MAIN);
        proficiencies.createCharacter(ALT);
    }

    private ProficiencyDefinition proficiency(String id) {
        return config.proficiencies().get(id).orElseThrow();
    }

    private ProficiencyTier tier(String id) {
        return config.tiers().get(id).orElseThrow();
    }

    @Test
    void reachingATierGivesItAndTheTiersBelow() {
        proficiencies.setTier(MAIN, proficiency("axes"), tier("adept"));
        assertEquals(List.of(new Update(ALICE,
                Set.of("skysongstats.proficiency.axes.proficient", "skysongstats.proficiency.axes.adept"),
                Set.of("skysongstats.proficiency.axes.master"))), updates);
    }

    @Test
    void aTierUpFromTrainingGivesTheNewNode() {
        proficiencies.changeFocus(MAIN, proficiency("axes"));
        proficiencies.train(MAIN, 4, "martial", tier("master"));
        assertEquals(Set.of("skysongstats.proficiency.axes.proficient"), updates.getLast().grant());
    }

    @Test
    void loweringATierTakesTheNodesAbove() {
        proficiencies.setTier(MAIN, proficiency("axes"), tier("master"));
        proficiencies.setTier(MAIN, proficiency("axes"), tier("untrained"));
        Update last = updates.getLast();
        assertTrue(last.grant().isEmpty());
        assertEquals(ProficiencyPermissions.all(proficiency("axes"), config.tiers()), last.revoke());
    }

    @Test
    void aCharacterThePlayerIsNotPlayingLeavesTheNodesAlone() {
        proficiencies.setTier(ALT, proficiency("axes"), tier("adept"));
        assertTrue(updates.isEmpty());
    }

    @Test
    void syncSetsEveryProficiencysNodes() {
        proficiencies.setTier(ALT, proficiency("bows"), tier("proficient"));
        permissions.sync(ALT, proficiencies);
        Update update = updates.getLast();
        assertEquals(Set.of("skysongstats.proficiency.bows.proficient"), update.grant());
        assertTrue(update.revoke().contains("skysongstats.proficiency.axes.proficient"));
        assertTrue(update.revoke().contains("skysongstats.proficiency.bows.adept"));
        assertEquals(5, update.revoke().size());
    }
}
