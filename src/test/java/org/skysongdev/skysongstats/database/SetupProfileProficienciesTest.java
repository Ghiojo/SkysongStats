package org.skysongdev.skysongstats.database;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.ProficiencyChangeListener;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.definition.ProficienciesConfigLoader;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedProficiencyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What /setup gives a character from the proficiency screen.
 */
class SetupProfileProficienciesTest {
    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");

    private final List<ProficiencyTierChange> tierChanges = new ArrayList<>();
    private ProficiencyService service;

    @BeforeEach
    void setUp() throws InvalidConfigurationException {
        YamlConfiguration skills = new YamlConfiguration();
        skills.loadFromString("levels:\n  unskilled: {xp: 0}\nskills:\n  martial: {}\n");
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                cost: 4
                tiers:
                  untrained: {}
                  proficient: {}
                  adept: {extra: 2}
                proficiencies:
                  axes: {skill: martial}
                  bows: {skill: martial}
                  whips: {skill: martial}
                """);
        service = new ProficiencyService(ProficienciesConfigLoader.load(file,
                SkillsConfigLoader.load(skills, name -> true).skills(), name -> true),
                new UnsavedProficiencyRepository(line -> { }), new ProficiencyChangeListener() {
                    @Override
                    public void onTierChange(ProficiencyTierChange change) {
                        tierChanges.add(change);
                    }

                    @Override
                    public void onFocusChange(ProficiencyFocusChange change) {
                        // Setup gives no focus.
                    }
                }, line -> { });
        service.createCharacter(ALICE);
    }

    private ProficiencyDefinition proficiency(String id) {
        return service.config().proficiencies().get(id).orElseThrow();
    }

    private String tier(String id) {
        return service.tier(ALICE, proficiency(id)).id();
    }

    @Test
    void theStartingProficienciesBecomeProficientAndNothingElseChanges() {
        new SetupProfile(ALICE.player().toString(), ALICE.profile(), false)
                .applyStartingProficiencies(service, List.of(proficiency("axes"), proficiency("bows")));
        assertEquals("proficient", tier("axes"));
        assertEquals("proficient", tier("bows"));
        assertEquals("untrained", tier("whips"));
        // Every tier change fires the event and sets the LuckPerms node.
        assertEquals(List.of("axes", "bows"), tierChanges.stream().map(change -> change.proficiency().id()).toList());
    }
}
