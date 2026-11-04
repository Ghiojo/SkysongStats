package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Focus;
import org.skysongdev.skysongstats.api.FocusResult;
import org.skysongdev.skysongstats.api.Proficiency;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.Tier;
import org.skysongdev.skysongstats.api.TierChange;
import org.skysongdev.skysongstats.api.TrainingResult;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.ProficiencyChangeListener;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficienciesConfigLoader;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedProficiencyRepository;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProficienciesImplTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Default");
    private static final CharacterId NOBODY = new CharacterId(UUID.randomUUID(), "Default");
    private static final String SKILLS = """
            levels:
              unskilled: {xp: 0}
            skills:
              martial: {}
              medical: {}
            """;
    private static final String PROFICIENCIES = """
            cost: 4
            tiers:
              untrained: {name: "Untrained"}
              proficient: {name: "Proficient", color: "<green>"}
              adept: {name: "Adept", extra: 2}
              master: {name: "Master", extra: 2}
            proficiencies:
              heavy_armor: {name: "Heavy Armor", skill: martial, aliases: [heavy]}
              bows: {name: "Bows", skill: martial}
              medicine: {name: "Medicine", skill: medical}
            """;

    private final List<ChangeCause> causesSeen = new ArrayList<>();
    private final ChangeCauses causes = new ChangeCauses();
    private ProficiencyService service;
    private ProficienciesImpl proficiencies;
    private SkillService skillService;

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return file;
    }

    @BeforeEach
    void setUp() {
        SkillsConfig skills = SkillsConfigLoader.load(yaml(SKILLS), name -> true);
        ProficienciesConfig config = ProficienciesConfigLoader.load(yaml(PROFICIENCIES), skills.skills(), name -> true);
        service = new ProficiencyService(config, new UnsavedProficiencyRepository(line -> { }),
                new ProficiencyChangeListener() {
                    @Override
                    public void onTierChange(ProficiencyTierChange change) {
                        causesSeen.add(causes.current());
                    }

                    @Override
                    public void onFocusChange(ProficiencyFocusChange change) {
                        causesSeen.add(causes.current());
                    }
                }, line -> { });
        service.createCharacter(new ProfileKey(ALICE.player(), ALICE.profile()));
        skillService = new SkillService(skills, new UnsavedSkillRepository(line -> { }), change -> { });
        proficiencies = new ProficienciesImpl(service, skillService, new MainThread(() -> true), causes);
    }

    private Proficiency proficiency(String name) {
        return proficiencies.find(name).orElseThrow();
    }

    private Tier tier(String id) {
        return proficiencies.findTier(id).orElseThrow();
    }

    private Skill skill(String id) {
        return ApiTypes.skill(skillService.config().skills().get(id).orElseThrow());
    }

    @Test
    void configLookups() {
        assertEquals(List.of("heavy_armor", "bows", "medicine"),
                proficiencies.all().stream().map(Proficiency::id).toList());
        assertEquals(new Proficiency("heavy_armor", "Heavy Armor", "", "martial", List.of("heavy")),
                proficiency("HEAVY"));
        assertEquals(List.of("heavy_armor", "bows"),
                proficiencies.ofSkill(skill("martial")).stream().map(Proficiency::id).toList());
        assertEquals(List.of("untrained", "proficient", "adept", "master"),
                proficiencies.tiers().stream().map(Tier::id).toList());
        assertEquals(new Tier("proficient", "Proficient", "<green>"), tier("Proficient"));
        assertEquals(Optional.empty(), proficiencies.findTier("legend"));
        assertEquals(Optional.empty(), proficiencies.find("Heavy Armor"));
    }

    @Test
    void aNewCharacterIsUntrainedWithNoFocus() {
        assertEquals(Optional.of(tier("untrained")), proficiencies.tier(ALICE, proficiency("bows")));
        assertEquals(Optional.empty(), proficiencies.focus(ALICE));
        assertEquals(OptionalInt.empty(), proficiencies.pointsNeeded(ALICE));
    }

    @Test
    void setFocusFocusesWithZeroPoints() {
        assertEquals(Optional.of(FocusResult.FOCUSED), proficiencies.setFocus(ALICE, proficiency("bows"), false));
        assertEquals(Optional.of(new Focus(proficiency("bows"), 0)), proficiencies.focus(ALICE));
        assertEquals(OptionalInt.of(4), proficiencies.pointsNeeded(ALICE));
        assertEquals(Optional.of(FocusResult.ALREADY_FOCUSED),
                proficiencies.setFocus(ALICE, proficiency("bows"), false));
        assertEquals(List.of(ChangeCause.API), causesSeen);
    }

    @Test
    void switchingAFocusWithPointsNeedsConfirmationUnlessPointsMayBeDiscarded() {
        proficiencies.setFocus(ALICE, proficiency("bows"), false);
        proficiencies.train(ALICE, skill("martial"), 2, tier("master"));

        assertEquals(Optional.of(FocusResult.NEEDS_CONFIRMATION),
                proficiencies.setFocus(ALICE, proficiency("heavy"), false));
        assertEquals("bows", proficiencies.focus(ALICE).orElseThrow().proficiency().id());

        assertEquals(Optional.of(FocusResult.FOCUSED), proficiencies.setFocus(ALICE, proficiency("heavy"), true));
        assertEquals(Optional.of(new Focus(proficiency("heavy"), 0)), proficiencies.focus(ALICE));
    }

    @Test
    void aMasteredProficiencyCannotBeFocused() {
        proficiencies.setTier(ALICE, proficiency("bows"), tier("master"));

        assertEquals(Optional.of(FocusResult.MASTERED), proficiencies.setFocus(ALICE, proficiency("bows"), true));
    }

    @Test
    void trainingAddsPointsAndRaisesTheTier() {
        proficiencies.setFocus(ALICE, proficiency("bows"), false);

        TrainingResult added = proficiencies.train(ALICE, skill("martial"), 3, tier("master")).orElseThrow();
        assertEquals(TrainingResult.Outcome.ADDED, added.outcome());
        assertEquals(3, added.points());

        TrainingResult up = proficiencies.train(ALICE, skill("martial"), 1, tier("master")).orElseThrow();
        assertEquals(new TrainingResult(TrainingResult.Outcome.TIER_UP, proficiency("bows"), tier("proficient"), 0),
                up);
        assertTrue(up.added());
        assertEquals(Optional.of(tier("proficient")), proficiencies.tier(ALICE, proficiency("bows")));
        assertEquals(OptionalInt.of(6), proficiencies.pointsNeeded(ALICE));
    }

    @Test
    void trainingOnlyCountsForTheFocusSkillBelowTheTrainersTier() {
        assertEquals(TrainingResult.Outcome.NO_FOCUS,
                proficiencies.train(ALICE, skill("martial"), 1, tier("master")).orElseThrow().outcome());
        proficiencies.setFocus(ALICE, proficiency("bows"), false);
        TrainingResult other = proficiencies.train(ALICE, skill("medical"), 1, tier("master")).orElseThrow();
        assertEquals(TrainingResult.Outcome.OTHER_SKILL, other.outcome());
        assertFalse(other.added());
        assertEquals(TrainingResult.Outcome.AT_MAX_TIER,
                proficiencies.train(ALICE, skill("martial"), 1, tier("untrained")).orElseThrow().outcome());
    }

    @Test
    void trainingNeedsAtLeastOnePoint() {
        Skill martial = skill("martial");
        Tier master = tier("master");

        assertThrows(IllegalArgumentException.class, () -> proficiencies.train(ALICE, martial, 0, master));
    }

    @Test
    void setTierGoesUpOrDownAndSaysWhich() {
        TierChange up = proficiencies.setTier(ALICE, proficiency("bows"), tier("adept")).orElseThrow();
        assertTrue(up.tierUp());
        assertTrue(up.changed());

        TierChange down = proficiencies.setTier(ALICE, proficiency("bows"), tier("proficient")).orElseThrow();
        assertFalse(down.tierUp());
        assertEquals(tier("adept"), down.before());
    }

    @Test
    void pointsCanBeSetAndRemoved() {
        proficiencies.setFocus(ALICE, proficiency("bows"), false);

        assertEquals(3, proficiencies.setPoints(ALICE, 3).orElseThrow().points());
        assertEquals(1, proficiencies.removePoints(ALICE, 2).orElseThrow().points());
        assertEquals(0, proficiencies.removePoints(ALICE, 5).orElseThrow().points());
        assertThrows(IllegalArgumentException.class, () -> proficiencies.setPoints(ALICE, 4));
        assertThrows(IllegalArgumentException.class, () -> proficiencies.removePoints(ALICE, -1));
    }

    @Test
    void pointsWithoutAFocusAreEmpty() {
        assertEquals(Optional.empty(), proficiencies.setPoints(ALICE, 1));
        assertEquals(Optional.empty(), proficiencies.removePoints(ALICE, 1));
    }

    @Test
    void everythingIsEmptyForAMissingCharacterProficiencyTierOrSkill() {
        Proficiency gone = new Proficiency("lances", "Lances", "", "martial", List.of());
        Tier legend = new Tier("legend", "Legend", "");
        Skill cooking = new Skill("cooking", "Cooking", "Cooking", "", List.of());

        assertEquals(Optional.empty(), proficiencies.tier(NOBODY, proficiency("bows")));
        assertEquals(Optional.empty(), proficiencies.focus(NOBODY));
        assertEquals(Optional.empty(), proficiencies.tier(ALICE, gone));
        assertEquals(Optional.empty(), proficiencies.setFocus(ALICE, gone, true));
        assertEquals(Optional.empty(), proficiencies.setTier(ALICE, proficiency("bows"), legend));
        assertEquals(Optional.empty(), proficiencies.train(ALICE, cooking, 1, tier("master")));
        assertEquals(Optional.empty(), proficiencies.train(ALICE, skill("martial"), 1, legend));
        assertEquals(Optional.empty(), proficiencies.train(NOBODY, skill("martial"), 1, tier("master")));
        assertTrue(causesSeen.isEmpty());
    }

    @Test
    void characterMethodsRefuseCallsOffTheMainThread() {
        ProficienciesImpl offThread = new ProficienciesImpl(service, skillService, new MainThread(() -> false), causes);
        Proficiency bows = proficiency("bows");

        assertThrows(IllegalStateException.class, () -> offThread.tier(ALICE, bows));
        assertThrows(IllegalStateException.class, () -> offThread.focus(ALICE));
        assertThrows(IllegalStateException.class, () -> offThread.setFocus(ALICE, bows, false));
        assertThrows(IllegalStateException.class, () -> offThread.setPoints(ALICE, 1));
        assertEquals(3, offThread.all().size());
        assertEquals(4, offThread.tiers().size());
    }

    @Test
    void nullArgumentsAreRefused() {
        Proficiency bows = proficiency("bows");

        assertThrows(NullPointerException.class, () -> proficiencies.find(null));
        assertThrows(NullPointerException.class, () -> proficiencies.findTier(null));
        assertThrows(NullPointerException.class, () -> proficiencies.tier(null, bows));
        assertThrows(NullPointerException.class, () -> proficiencies.setTier(ALICE, bows, null));
    }
}
