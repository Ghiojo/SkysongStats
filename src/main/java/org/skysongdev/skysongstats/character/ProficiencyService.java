package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProficiencyRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredFocus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

// Every character's proficiency tiers and focus. Changes are saved through the
// repository and reported to the listener. Characters are kept in memory, loaded
// once at startup, like SkillService. Call it from the server thread only.
//
// A character trains one focus at a time. Training points go to the focus only
// when it belongs to the training's skill and is below the trainer's maximum
// tier. When the points reach the next tier's cost the focus goes up one tier and
// its points go back to 0; leftover points are lost. At the top tier the focus is
// cleared and the player picks a new one.
public class ProficiencyService {
    private final ProficiencyRepository repository;
    private final ProficiencyChangeListener listener;
    private final Consumer<String> warnings;
    private final Map<ProfileKey, Map<String, String>> tiers = new HashMap<>();
    private final Map<ProfileKey, StoredFocus> focuses = new HashMap<>();
    private ProficienciesConfig config;

    // warnings gets saved tiers that proficiencies.yml no longer has.
    public ProficiencyService(ProficienciesConfig config, ProficiencyRepository repository,
                              ProficiencyChangeListener listener, Consumer<String> warnings) {
        this.config = Objects.requireNonNull(config, "config");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    public void load() {
        tiers.clear();
        focuses.clear();
        repository.loadAll().forEach((character, saved) -> {
            tiers.put(character, new HashMap<>(saved.tiers()));
            saved.focusOptions().ifPresent(focus -> focuses.put(character, focus));
        });
        tiers.keySet().forEach(this::saveMissingProficiencies);
        warnAboutUnknownTiers();
    }

    public ProficienciesConfig config() {
        return config;
    }

    // Swaps in a reloaded proficiencies.yml. Proficiencies new to the file are
    // saved at the first tier. Rows of proficiencies that left the file stay saved
    // but unused, and a focus on one shows as no focus.
    public void setConfig(ProficienciesConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        tiers.keySet().forEach(this::saveMissingProficiencies);
        warnAboutUnknownTiers();
    }

    // Every character with proficiencies: one per profile.
    public Set<ProfileKey> characters() {
        return Set.copyOf(tiers.keySet());
    }

    public boolean hasCharacter(ProfileKey character) {
        return tiers.containsKey(character);
    }

    // Creates a new character and saves every proficiency at the first tier, so
    // the database holds the whole character from the start.
    public void createCharacter(ProfileKey character) {
        tiers.put(character, new HashMap<>());
        saveMissingProficiencies(character);
    }

    // Creates the character first if it has no proficiencies yet: for profiles
    // made before the proficiency tables existed.
    public void ensureCharacter(ProfileKey character) {
        if (!tiers.containsKey(character)) {
            createCharacter(character);
        }
    }

    // Removes a character's tiers and focus, for deleted profiles.
    public void deleteCharacter(ProfileKey character) {
        tiers.remove(character);
        focuses.remove(character);
        repository.deleteCharacter(character);
    }

    // The proficiency's tier. A saved tier that proficiencies.yml no longer has
    // counts as the first tier.
    public ProficiencyTier tier(ProfileKey character, ProficiencyDefinition proficiency) {
        String id = require(character).getOrDefault(proficiency.id(), "");
        return config.tiers().get(id).orElse(config.tiers().first());
    }

    // The character's focus, or empty when it has none or its proficiency left
    // proficiencies.yml.
    public Optional<ProficiencyFocus> focus(ProfileKey character) {
        require(character);
        StoredFocus saved = focuses.get(character);
        if (saved == null) {
            return Optional.empty();
        }
        return config.proficiencies().get(saved.proficiencyId())
                .map(proficiency -> new ProficiencyFocus(proficiency, saved.points()));
    }

    // The points the focus needs for its next tier.
    public int pointsNeeded(ProfileKey character, ProficiencyFocus focus) {
        return config.tiers().next(tier(character, focus.proficiency()))
                .map(next -> config.cost(focus.proficiency(), next))
                .orElse(0);
    }

    // Focuses a proficiency with 0 points, unless it is mastered, already the
    // focus, or the current focus has points: then the player must confirm first
    // and changeFocus does it.
    public FocusRequest requestFocus(ProfileKey character, ProficiencyDefinition proficiency) {
        Optional<ProficiencyFocus> current = focus(character);
        if (config.tiers().isTop(tier(character, proficiency))) {
            return FocusRequest.MASTERED;
        }
        if (current.filter(focus -> focus.proficiency().id().equals(proficiency.id())).isPresent()) {
            return FocusRequest.ALREADY_FOCUSED;
        }
        if (current.filter(focus -> focus.points() > 0).isPresent()) {
            return FocusRequest.NEEDS_CONFIRMATION;
        }
        changeFocus(character, proficiency);
        return FocusRequest.FOCUSED;
    }

    // Focuses a proficiency with 0 points, losing the current focus's points.
    public void changeFocus(ProfileKey character, ProficiencyDefinition proficiency) {
        if (config.tiers().isTop(tier(character, proficiency))) {
            throw new IllegalArgumentException(proficiency.id() + " is already at the top tier");
        }
        saveFocus(character, new ProficiencyFocus(proficiency, 0));
    }

    // Adds training points to the focus when it belongs to the skill and is below
    // maxTier. Reaching the next tier's cost raises the tier and resets the points.
    public Training train(ProfileKey character, int amount, String skillId, ProficiencyTier maxTier) {
        if (amount <= 0) {
            throw new IllegalArgumentException("The amount must be at least 1");
        }
        return focus(character)
                .map(focus -> trainFocus(character, focus, amount, skillId, maxTier))
                .orElseGet(() -> new Training(Training.Outcome.NO_FOCUS, null, null, 0));
    }

    // Removes points from the focus, stopping at 0. Never lowers a tier. Empty
    // when the character has no focus.
    public Optional<ProficiencyFocus> removePoints(ProfileKey character, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative");
        }
        return focus(character).map(focus -> {
            ProficiencyFocus after = new ProficiencyFocus(focus.proficiency(), Math.max(0, focus.points() - amount));
            saveFocus(character, after);
            return after;
        });
    }

    // Sets the focus's points; they must stay below the next tier's cost (tier
    // changes go through setTier). Empty when the character has no focus.
    public Optional<ProficiencyFocus> setPoints(ProfileKey character, int points) {
        return focus(character).map(focus -> {
            int needed = pointsNeeded(character, focus);
            if (points < 0 || points >= needed) {
                throw new IllegalArgumentException("Points must be between 0 and " + (needed - 1));
            }
            ProficiencyFocus after = new ProficiencyFocus(focus.proficiency(), points);
            saveFocus(character, after);
            return after;
        });
    }

    // Sets a tier directly. The focus keeps its points, unless the focused
    // proficiency reaches the top tier: then the focus is cleared.
    public ProficiencyTierChange setTier(ProfileKey character, ProficiencyDefinition proficiency, ProficiencyTier tier) {
        ProficiencyTierChange change = saveTier(character, proficiency, tier);
        boolean focused = focus(character).filter(focus -> focus.proficiency().id().equals(proficiency.id())).isPresent();
        if (focused && config.tiers().isTop(tier)) {
            clearFocus(character);
        }
        return change;
    }

    private Training trainFocus(ProfileKey character, ProficiencyFocus focus, int amount, String skillId,
                                ProficiencyTier maxTier) {
        ProficiencyTier tier = tier(character, focus.proficiency());
        if (!focus.proficiency().skill().equals(skillId)) {
            return new Training(Training.Outcome.OTHER_SKILL, focus.proficiency(), tier, focus.points());
        }
        if (config.tiers().atLeast(tier, maxTier)) {
            return new Training(Training.Outcome.AT_MAX_TIER, focus.proficiency(), tier, focus.points());
        }
        return addPoints(character, focus, tier, amount);
    }

    private Training addPoints(ProfileKey character, ProficiencyFocus focus, ProficiencyTier tier, int amount) {
        ProficiencyTier next = config.tiers().next(tier).orElseThrow();
        long points = (long) focus.points() + amount;
        if (points < config.cost(focus.proficiency(), next)) {
            saveFocus(character, new ProficiencyFocus(focus.proficiency(), (int) points));
            return new Training(Training.Outcome.ADDED, focus.proficiency(), tier, (int) points);
        }
        saveTier(character, focus.proficiency(), next);
        if (config.tiers().isTop(next)) {
            clearFocus(character);
        } else {
            saveFocus(character, new ProficiencyFocus(focus.proficiency(), 0));
        }
        return new Training(Training.Outcome.TIER_UP, focus.proficiency(), next, 0);
    }

    private ProficiencyTierChange saveTier(ProfileKey character, ProficiencyDefinition proficiency, ProficiencyTier tier) {
        ProficiencyTier before = tier(character, proficiency);
        require(character).put(proficiency.id(), tier.id());
        repository.saveTier(character, proficiency.id(), tier.id());
        ProficiencyTierChange change = new ProficiencyTierChange(character, proficiency, before, tier,
                !config.tiers().atLeast(before, tier));
        if (!before.id().equals(tier.id())) {
            listener.onTierChange(change);
        }
        return change;
    }

    private void saveFocus(ProfileKey character, ProficiencyFocus after) {
        ProficiencyFocus before = focus(character).orElse(null);
        focuses.put(character, new StoredFocus(after.proficiency().id(), after.points()));
        repository.saveFocus(character, new StoredFocus(after.proficiency().id(), after.points()));
        if (!after.equals(before)) {
            listener.onFocusChange(new ProficiencyFocusChange(character, before, after));
        }
    }

    private void clearFocus(ProfileKey character) {
        ProficiencyFocus before = focus(character).orElse(null);
        focuses.remove(character);
        repository.clearFocus(character);
        listener.onFocusChange(new ProficiencyFocusChange(character, before, null));
    }

    private void saveMissingProficiencies(ProfileKey character) {
        Map<String, String> saved = tiers.get(character);
        String first = config.tiers().first().id();
        for (ProficiencyDefinition proficiency : config.proficiencies().all()) {
            if (!saved.containsKey(proficiency.id())) {
                saved.put(proficiency.id(), first);
                repository.saveTier(character, proficiency.id(), first);
            }
        }
    }

    private void warnAboutUnknownTiers() {
        Set<String> unknown = new TreeSet<>();
        tiers.values().forEach(saved -> saved.values().stream()
                .filter(id -> config.tiers().get(id).isEmpty())
                .forEach(unknown::add));
        if (!unknown.isEmpty()) {
            warnings.accept("Saved proficiency tiers " + unknown + " are not in proficiencies.yml, they count as "
                    + config.tiers().first().id());
        }
    }

    private Map<String, String> require(ProfileKey character) {
        Map<String, String> saved = tiers.get(character);
        if (saved == null) {
            throw new IllegalArgumentException("No proficiencies loaded for " + character);
        }
        return saved;
    }
}
