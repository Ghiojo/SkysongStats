package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.ActiveCondition;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.ConditionType;
import org.skysongdev.skysongstats.api.Conditions;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.messages.AfflictionTexts;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

// Conditions answered by the affliction service. A character "exists" when it has
// stats, like everywhere in the API.
final class ConditionsImpl implements Conditions {
    private final Supplier<AfflictionService> afflictions;
    private final StatService stats;
    private final AfflictionTexts texts;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    ConditionsImpl(Supplier<AfflictionService> afflictions, StatService stats, AfflictionTexts texts,
                   MainThread mainThread, ChangeCauses causes) {
        this.afflictions = afflictions;
        this.stats = stats;
        this.texts = texts;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull List<ConditionType> types() {
        return afflictions.get().conditionDefinitions().all().stream().map(AfflictionApiTypes::conditionType).toList();
    }

    @Override
    public @NotNull Optional<ConditionType> type(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return afflictions.get().conditionDefinitions().get(id).map(AfflictionApiTypes::conditionType);
    }

    @Override
    public @NotNull List<ActiveCondition> current(@NotNull CharacterId character) {
        return afflictions.get().conditions(checked(character)).stream().map(AfflictionApiTypes::condition).toList();
    }

    @Override
    public @NotNull Optional<ActiveCondition> award(@NotNull CharacterId character, @NotNull String conditionId,
                                                    int level) {
        Objects.requireNonNull(conditionId, "conditionId");
        if (level < 1) {
            throw new IllegalArgumentException("A condition's level is at least 1, got " + level);
        }
        ProfileKey key = checked(character);
        if (stats.sheet(key).isEmpty()) {
            return Optional.empty();
        }
        return causes.during(ChangeCause.API, () -> afflictions.get().awardCondition(key, conditionId, level))
                .map(AfflictionApiTypes::condition);
    }

    @Override
    public boolean remove(@NotNull CharacterId character, @NotNull String conditionId) {
        Objects.requireNonNull(conditionId, "conditionId");
        ProfileKey key = checked(character);
        AfflictionService.ConditionRemoval removal =
                causes.during(ChangeCause.API, () -> afflictions.get().removeCondition(key, conditionId));
        return removal == AfflictionService.ConditionRemoval.REMOVED
                || removal == AfflictionService.ConditionRemoval.REMOVED_STILL_GIVEN;
    }

    @Override
    public @NotNull Optional<String> tag(@NotNull String conditionId, int level) {
        Objects.requireNonNull(conditionId, "conditionId");
        if (level < 1) {
            throw new IllegalArgumentException("A condition's level is at least 1, got " + level);
        }
        return afflictions.get().conditionDefinitions().get(conditionId)
                .map(condition -> AfflictionTexts.miniMessage(texts.conditionTag(condition, level)));
    }

    private ProfileKey checked(CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return ApiTypes.key(character);
    }
}
