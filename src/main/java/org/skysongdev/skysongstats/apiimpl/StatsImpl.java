package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.StatValues;
import org.skysongdev.skysongstats.api.Stats;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.CharacterSheet;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatCalculator;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Stats answered by the stat service. Each call reads the current stats.yml, so a
// Stat kept across /ssreload is looked up again by its id.
final class StatsImpl implements Stats {
    private final StatService stats;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    StatsImpl(StatService stats, MainThread mainThread, ChangeCauses causes) {
        this.stats = stats;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull List<Stat> all() {
        return stats.definitions().all().stream().map(ApiTypes::stat).toList();
    }

    @Override
    public @NotNull Optional<Stat> find(@NotNull String idOrAlias) {
        Objects.requireNonNull(idOrAlias, "idOrAlias");
        return stats.definitions().find(idOrAlias).map(ApiTypes::stat);
    }

    @Override
    public @NotNull Optional<StatValues> get(@NotNull CharacterId character, @NotNull Stat stat) {
        ProfileKey key = checked(character, stat);
        return stats.sheet(key).flatMap(sheet -> definition(stat).map(definition -> values(sheet, definition)));
    }

    @Override
    public @NotNull Optional<StatChange> apply(@NotNull CharacterId character, @NotNull Stat stat,
                                               @NotNull StatAction action, int amount) {
        Objects.requireNonNull(action, "action");
        if (action != StatAction.SET && amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative for " + action + ": " + amount);
        }
        ProfileKey key = checked(character, stat);
        if (stats.sheet(key).isEmpty()) {
            return Optional.empty();
        }
        return definition(stat).map(definition -> ApiTypes.statChange(causes.during(ChangeCause.API,
                () -> stats.apply(key, definition, ApiTypes.action(action), amount))));
    }

    private StatValues values(CharacterSheet sheet, StatDefinition stat) {
        StatCalculator calculator = stats.calculator(sheet);
        Integer max = stat.isBar() ? calculator.max(stat) : null;
        Integer maxBase = stat.isBar() ? calculator.maxBase(stat) : null;
        return new StatValues(calculator.base(stat), calculator.modifiers(stat.id()), calculator.items(stat), max,
                maxBase);
    }

    // Checks the arguments and the thread, and gives the internal key.
    private ProfileKey checked(CharacterId character, Stat stat) {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(stat, "stat");
        mainThread.check();
        return ApiTypes.key(character);
    }

    // The stat as stats.yml has it now, matched by id only: an alias can move to
    // another stat on reload, an id can't.
    private Optional<StatDefinition> definition(Stat stat) {
        return stats.definitions().get(stat.id());
    }
}
