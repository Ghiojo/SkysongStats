package org.skysongdev.skysongstats.placeholders;

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
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedProficiencyRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The proficiency placeholders on a small proficiencies.yml (costs 4, 6, 8), with
 * a proficiency whose id ends in _tier.
 */
class ProficiencyPlaceholdersTest {
    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");
    private static final String SKILLS = """
            levels:
              unskilled: {xp: 0}
            skills:
              martial: {}
            """;
    private static final String PROFICIENCIES = """
            cost: 4
            tiers:
              untrained: {name: Untrained, color: "<dark_gray>"}
              proficient: {name: Proficient, color: "<green>"}
              adept: {name: Adept, color: "<#123456>", extra: 2}
              master: {name: Master, extra: 2}
            proficiencies:
              axes: {name: Axes, skill: martial}
              bows: {name: Bows, skill: martial}
              axes_tier: {name: Axe Tiers, skill: martial}
            """;

    private ProficiencyService service;
    private ProficiencyPlaceholders placeholders;

    @BeforeEach
    void setUp() throws InvalidConfigurationException {
        YamlConfiguration skills = new YamlConfiguration();
        skills.loadFromString(SKILLS);
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(PROFICIENCIES);
        service = new ProficiencyService(
                ProficienciesConfigLoader.load(file, SkillsConfigLoader.load(skills, name -> true).skills()),
                new UnsavedProficiencyRepository(line -> { }), new ProficiencyChangeListener() {
                    @Override
                    public void onTierChange(ProficiencyTierChange change) {
                        // Not needed here.
                    }

                    @Override
                    public void onFocusChange(ProficiencyFocusChange change) {
                        // Not needed here.
                    }
                }, line -> { });
        service.createCharacter(ALICE);
        placeholders = new ProficiencyPlaceholders(service);
    }

    private ProficiencyDefinition proficiency(String id) {
        return service.config().proficiencies().get(id).orElseThrow();
    }

    private ProficiencyTier tier(String id) {
        return service.config().tiers().get(id).orElseThrow();
    }

    private Optional<String> resolve(String placeholder) {
        return placeholders.resolve(ALICE, placeholder);
    }

    @Test
    void aProficiencyShowsItsTierNameInTheTiersColour() {
        assertEquals(Optional.of("§8Untrained"), resolve("proficiency_axes"));
        service.setTier(ALICE, proficiency("axes"), tier("adept"));
        assertEquals(Optional.of("§x§1§2§3§4§5§6Adept"), resolve("PROFICIENCY_Axes"));
    }

    @Test
    void theTierIdIsForConditions() {
        service.setTier(ALICE, proficiency("bows"), tier("proficient"));
        assertEquals(Optional.of("proficient"), resolve("proficiency_bows_tier"));
    }

    @Test
    void anIdEndingInTierIsMatchedAsAnIdFirst() {
        service.setTier(ALICE, proficiency("axes_tier"), tier("proficient"));
        assertEquals(Optional.of("§aProficient"), resolve("proficiency_axes_tier"));
        assertEquals(Optional.of("proficient"), resolve("proficiency_axes_tier_tier"));
    }

    @Test
    void theFocusPlaceholdersAreEmptyOrZeroWithoutAFocus() {
        assertEquals(Optional.of(""), resolve("proficiency_focus"));
        assertEquals(Optional.of("0"), resolve("proficiency_focus_points"));
        assertEquals(Optional.of("0"), resolve("proficiency_focus_cost"));
    }

    @Test
    void theFocusPlaceholdersShowTheFocusPointsAndCost() {
        service.setTier(ALICE, proficiency("bows"), tier("proficient"));
        service.changeFocus(ALICE, proficiency("bows"));
        service.setPoints(ALICE, 5);
        assertEquals(Optional.of("Bows"), resolve("proficiency_focus"));
        assertEquals(Optional.of("5"), resolve("proficiency_focus_points"));
        assertEquals(Optional.of("6"), resolve("proficiency_focus_cost"));
    }

    @Test
    void otherNamesAreNotProficiencyPlaceholders() {
        assertEquals(Optional.empty(), resolve("axes"));
        assertEquals(Optional.empty(), resolve("proficiency_juggling"));
        assertEquals(Optional.empty(), resolve("proficiency_juggling_tier"));
        assertEquals(Optional.empty(), resolve("proficiency_"));
    }

    @Test
    void aCharacterWithoutProficienciesHasNoValues() {
        assertEquals(Optional.empty(), placeholders.resolve(new ProfileKey(UUID.randomUUID(), "main"),
                "proficiency_axes"));
    }
}
