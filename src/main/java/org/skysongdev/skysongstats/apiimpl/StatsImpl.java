package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Effect;
import org.skysongdev.skysongstats.api.ShortNameAssignment;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.StatValues;
import org.skysongdev.skysongstats.api.Stats;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.StatShortNameChangeEvent;
import org.skysongdev.skysongstats.character.CharacterSheet;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatCalculator;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.ShortNameAssignments;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

// Stats answered by the stat service. Each call reads the current stats.yml, so a
// Stat kept across /ssreload is looked up again by its id. Assigned short names live
// in the stat service; a plugin's go when it disables (the API offers this as a
// listener), and each change of a stat's short name fires an event.
final class StatsImpl implements Stats, Listener {
    private final StatService stats;
    private final MainThread mainThread;
    private final ChangeCauses causes;
    private final Consumer<Event> events;

    StatsImpl(StatService stats, MainThread mainThread, ChangeCauses causes) {
        this(stats, mainThread, causes, event -> { });
    }

    StatsImpl(StatService stats, MainThread mainThread, ChangeCauses causes, Consumer<Event> events) {
        this.stats = stats;
        this.mainThread = mainThread;
        this.causes = causes;
        this.events = events;
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

    @Override
    public @NotNull Optional<ShortNameAssignment> assignShortName(@NotNull Plugin owner, @NotNull Stat stat,
                                                                  @NotNull String shortName) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(shortName, "shortName");
        mainThread.check();
        if (!owner.isEnabled()) {
            throw new IllegalStateException(owner.getName() + " is not enabled: assign short names in onEnable");
        }
        Map<String, String> before = shortNames();
        Optional<ShortNameAssignment> assigned = stats.shortNames()
                .assign(stats.configDefinitions(), owner, stat.id(), shortName)
                .map(Assignment::new);
        assigned.ifPresent(assignment -> changed(before));
        return assigned;
    }

    @Override
    public @NotNull List<ShortNameAssignment> shortNameAssignments() {
        mainThread.check();
        return stats.shortNames().all().stream().<ShortNameAssignment>map(Assignment::new).toList();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Map<String, String> before = shortNames();
        if (stats.shortNames().removeOwner(event.getPlugin())) {
            changed(before);
        }
    }

    // Every stat's short name now, by id.
    private Map<String, String> shortNames() {
        Map<String, String> names = new HashMap<>();
        stats.definitions().all().forEach(stat -> names.put(stat.id(), stat.display().shortName()));
        return names;
    }

    // Puts the assignments in effect and fires an event for each stat whose short
    // name changed from before.
    private void changed(Map<String, String> before) {
        stats.refreshShortNames();
        for (StatDefinition stat : stats.definitions().all()) {
            String old = before.get(stat.id());
            if (old != null && !old.equals(stat.display().shortName())) {
                events.accept(new StatShortNameChangeEvent(ApiTypes.stat(stat), old));
            }
        }
    }

    private final class Assignment implements ShortNameAssignment {
        private final ShortNameAssignments.Assignment<Object> assignment;

        private Assignment(ShortNameAssignments.Assignment<Object> assignment) {
            this.assignment = assignment;
        }

        @Override
        public @NotNull Plugin owner() {
            return (Plugin) assignment.owner();
        }

        @Override
        public @NotNull String statId() {
            return assignment.statId();
        }

        @Override
        public @NotNull String shortName() {
            return assignment.shortName();
        }

        @Override
        public boolean isAssigned() {
            return !assignment.isRemoved();
        }

        @Override
        public boolean isActive() {
            return isAssigned() && stats.shortNames().isActive(stats.configDefinitions(), assignment);
        }

        @Override
        public void unassign() {
            mainThread.check();
            Map<String, String> before = shortNames();
            if (stats.shortNames().remove(assignment)) {
                changed(before);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Assignment that && that.assignment == assignment;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(assignment);
        }

        @Override
        public String toString() {
            return "ShortNameAssignment[" + assignment.statId() + "=" + assignment.shortName() + "]";
        }
    }

    @Override
    public @NotNull List<Effect> effects(@NotNull CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return stats.sheet(ApiTypes.key(character))
                .map(sheet -> sheet.effects().stream().map(AfflictionApiTypes::effect).toList())
                .orElse(List.of());
    }
}
