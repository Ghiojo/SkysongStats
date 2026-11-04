package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Focus;
import org.skysongdev.skysongstats.api.FocusResult;
import org.skysongdev.skysongstats.api.Proficiencies;
import org.skysongdev.skysongstats.api.Proficiency;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.Tier;
import org.skysongdev.skysongstats.api.TierChange;
import org.skysongdev.skysongstats.api.TrainingResult;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.FocusRequest;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

// Proficiencies answered by the proficiency service. Each call reads the current
// proficiencies.yml, so a Proficiency or Tier kept across /ssreload is looked up
// again by its id.
final class ProficienciesImpl implements Proficiencies {
    private final ProficiencyService proficiencies;
    private final SkillService skills;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    ProficienciesImpl(ProficiencyService proficiencies, SkillService skills, MainThread mainThread,
                      ChangeCauses causes) {
        this.proficiencies = proficiencies;
        this.skills = skills;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull List<Proficiency> all() {
        return proficiencies.config().proficiencies().all().stream().map(ApiTypes::proficiency).toList();
    }

    @Override
    public @NotNull Optional<Proficiency> find(@NotNull String idOrAlias) {
        Objects.requireNonNull(idOrAlias, "idOrAlias");
        return proficiencies.config().proficiencies().find(idOrAlias).map(ApiTypes::proficiency);
    }

    @Override
    public @NotNull List<Proficiency> ofSkill(@NotNull Skill skill) {
        Objects.requireNonNull(skill, "skill");
        return proficiencies.config().proficiencies().ofSkill(skill.id()).stream().map(ApiTypes::proficiency).toList();
    }

    @Override
    public @NotNull List<Tier> tiers() {
        return proficiencies.config().tiers().all().stream().map(ApiTypes::tier).toList();
    }

    @Override
    public @NotNull Optional<Tier> findTier(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return proficiencies.config().tiers().get(id.trim().toLowerCase(Locale.ROOT)).map(ApiTypes::tier);
    }

    @Override
    public @NotNull Optional<Tier> tier(@NotNull CharacterId character, @NotNull Proficiency proficiency) {
        Objects.requireNonNull(proficiency, "proficiency");
        return existing(character).flatMap(key -> definition(proficiency)
                .map(definition -> ApiTypes.tier(proficiencies.tier(key, definition))));
    }

    @Override
    public @NotNull Optional<Focus> focus(@NotNull CharacterId character) {
        return existing(character).flatMap(proficiencies::focus).map(ApiTypes::focus);
    }

    @Override
    public @NotNull OptionalInt pointsNeeded(@NotNull CharacterId character) {
        return existing(character)
                .flatMap(key -> proficiencies.focus(key).map(focus -> proficiencies.pointsNeeded(key, focus)))
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    @Override
    public @NotNull Optional<FocusResult> setFocus(@NotNull CharacterId character, @NotNull Proficiency proficiency,
                                                   boolean discardPoints) {
        Objects.requireNonNull(proficiency, "proficiency");
        return existing(character).flatMap(key -> definition(proficiency).map(definition -> asApi(() -> {
            FocusRequest request = proficiencies.requestFocus(key, definition);
            if (request == FocusRequest.NEEDS_CONFIRMATION && discardPoints) {
                proficiencies.changeFocus(key, definition);
                return FocusResult.FOCUSED;
            }
            return FocusResult.valueOf(request.name());
        })));
    }

    @Override
    public @NotNull Optional<TrainingResult> train(@NotNull CharacterId character, @NotNull Skill skill, int points,
                                                   @NotNull Tier maxTier) {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(maxTier, "maxTier");
        if (points < 1) {
            throw new IllegalArgumentException("Training needs at least 1 point: " + points);
        }
        Optional<ProfileKey> key = existing(character);
        Optional<ProficiencyTier> max = definition(maxTier);
        if (key.isEmpty() || max.isEmpty() || skills.config().skills().get(skill.id()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ApiTypes.training(
                asApi(() -> proficiencies.train(key.orElseThrow(), points, skill.id(), max.orElseThrow()))));
    }

    @Override
    public @NotNull Optional<TierChange> setTier(@NotNull CharacterId character, @NotNull Proficiency proficiency,
                                                 @NotNull Tier tier) {
        Objects.requireNonNull(proficiency, "proficiency");
        Objects.requireNonNull(tier, "tier");
        return existing(character).flatMap(key -> definition(proficiency).flatMap(definition -> definition(tier)
                .map(target -> ApiTypes.tierChange(asApi(() -> proficiencies.setTier(key, definition, target))))));
    }

    @Override
    public @NotNull Optional<Focus> setPoints(@NotNull CharacterId character, int points) {
        return existing(character).flatMap(key -> asApi(() -> proficiencies.setPoints(key, points)))
                .map(ApiTypes::focus);
    }

    @Override
    public @NotNull Optional<Focus> removePoints(@NotNull CharacterId character, int points) {
        if (points < 0) {
            throw new IllegalArgumentException("The points can't be negative: " + points);
        }
        return existing(character).flatMap(key -> asApi(() -> proficiencies.removePoints(key, points)))
                .map(ApiTypes::focus);
    }

    // Runs a change so its events carry the API as their cause.
    private <T> T asApi(Supplier<T> change) {
        return causes.during(ChangeCause.API, change);
    }

    // Checks the argument and the thread, and gives the character's key when it has
    // proficiencies.
    private Optional<ProfileKey> existing(CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        ProfileKey key = ApiTypes.key(character);
        return proficiencies.hasCharacter(key) ? Optional.of(key) : Optional.empty();
    }

    // Matched by id only: an alias can move to another proficiency on reload, an id can't.
    private Optional<ProficiencyDefinition> definition(Proficiency proficiency) {
        return proficiencies.config().proficiencies().get(proficiency.id());
    }

    private Optional<ProficiencyTier> definition(Tier tier) {
        return proficiencies.config().tiers().get(tier.id());
    }
}
