package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

// All the rules for changing stats live here: what set/add/reduce/damage do, the
// min and max limits, shields, and modifiers. Every change is saved through the
// repository and reported to the listener. Characters are kept in memory, loaded
// once at startup. Call it from the server thread only.
//
// set/add/reduce change a stat's base value. The limits apply to its total (base
// + modifiers + items): a bar stops at its max unless it can exceed it, and no
// stat goes below its min.
public class StatService {
    private final StatRepository repository;
    private final StatChangeListener listener;
    private final Map<ProfileKey, CharacterSheet> sheets = new HashMap<>();
    // volatile: the API reads it from any thread, and /ssreload swaps it on the main thread.
    private volatile StatDefinitions definitions;

    public StatService(StatDefinitions definitions, StatRepository repository, StatChangeListener listener) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public void load() {
        sheets.clear();
        repository.loadAll().forEach((key, stored) -> sheets.put(key, new CharacterSheet(key, stored)));
        sheets.values().forEach(this::saveMissingValues);
    }

    public StatDefinitions definitions() {
        return definitions;
    }

    // Swaps in a reloaded stats.yml. Stats new to the file are saved at their
    // starting value, and bars that are now above their max are brought down to it.
    public void setDefinitions(StatDefinitions definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        sheets.values().forEach(this::saveMissingValues);
        sheets.values().forEach(this::clampBars);
    }

    // Every character with stats: one per profile.
    public Set<ProfileKey> characters() {
        return Set.copyOf(sheets.keySet());
    }

    public Optional<CharacterSheet> sheet(ProfileKey character) {
        return Optional.ofNullable(sheets.get(character));
    }

    public StatCalculator calculator(CharacterSheet sheet) {
        return new StatCalculator(definitions, sheet);
    }

    // A stat total or bar max by name (a stat id or max_<bar>), for placeholders.
    public OptionalInt value(ProfileKey character, String name) {
        CharacterSheet sheet = sheets.get(character);
        return sheet == null ? OptionalInt.empty() : calculator(sheet).value(name);
    }

    // Creates a new character's sheet and saves every stat's starting value, so the
    // database holds the whole character from the start (and a full bar doesn't
    // keep following its max as stats change later).
    public CharacterSheet createCharacter(ProfileKey character) {
        CharacterSheet sheet = new CharacterSheet(character, new StoredStats(Map.of(), List.of()));
        saveMissingValues(sheet);
        sheets.put(character, sheet);
        return sheet;
    }

    // Creates a new character like createCharacter, with some stats starting at the
    // given base values (stat id -> value, like /stats set) instead of their
    // stats.yml start. The other stats start as they would for a new profile,
    // worked out from the given ones (a bar that starts full starts at the max the
    // given stats make). Values are kept within each stat's min and max. No change
    // events fire: the character is new. Throws for an id that isn't a stat.
    public CharacterSheet createCharacter(ProfileKey character, Map<String, Integer> startingValues) {
        Map<StatDefinition, Integer> given = new LinkedHashMap<>();
        startingValues.forEach((id, value) -> given.put(definitions.get(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown stat: " + id)), value));
        CharacterSheet sheet = new CharacterSheet(character, new StoredStats(Map.of(), List.of()));
        given.forEach((stat, value) -> sheet.setValue(stat.id(), value));
        saveMissingValues(sheet);
        given.forEach((stat, value) -> change(sheet, stat, value));
        fitBars(sheet);
        sheets.put(character, sheet);
        return sheet;
    }

    // The character's sheet, created first if it has none yet: for profiles that
    // were made before the stat tables existed.
    public CharacterSheet ensureCharacter(ProfileKey character) {
        CharacterSheet sheet = sheets.get(character);
        return sheet != null ? sheet : createCharacter(character);
    }

    // Removes a character's values and modifiers, for deleted profiles.
    public void deleteCharacter(ProfileKey character) {
        sheets.remove(character);
        repository.deleteCharacter(character);
    }

    // Runs a stat action, e.g. what /heal 5 or /stats set str 2 do. amount can't be
    // negative except for set.
    public StatChange apply(ProfileKey character, StatDefinition stat, StatAction action, int amount) {
        if (action != StatAction.SET && amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative");
        }
        CharacterSheet sheet = require(character);
        StatCalculator calculator = calculator(sheet);
        int before = calculator.total(stat);
        switch (action) {
            case SET -> change(sheet, stat, amount);
            case ADD -> change(sheet, stat, calculator.base(stat) + amount);
            case REDUCE -> change(sheet, stat, calculator.base(stat) - amount);
            case DAMAGE -> damage(sheet, stat, amount);
            default -> throw new IllegalStateException("Unknown action " + action);
        }
        clampBars(sheet);
        StatChange change = report(sheet, stat, before);
        listener.onTotalsChanged(character);
        return change;
    }

    // The shield (temp HP for hp) takes the damage first, down to 0; the rest
    // comes off the bar.
    private void damage(CharacterSheet sheet, StatDefinition stat, int amount) {
        String shieldId = stat.isBar() ? stat.bar().shield() : null;
        StatDefinition shield = shieldId == null ? null : definitions.get(shieldId).orElse(null);
        if (shield == null) {
            throw new IllegalArgumentException(stat.id() + " has no shield to take damage");
        }
        StatCalculator calculator = calculator(sheet);
        int absorbed = Math.min(Math.max(calculator.total(shield), 0), amount);
        if (absorbed > 0) {
            int shieldBefore = calculator.total(shield);
            change(sheet, shield, calculator.base(shield) - absorbed);
            report(sheet, shield, shieldBefore);
        }
        change(sheet, stat, calculator(sheet).base(stat) - (amount - absorbed));
    }

    // Fills every bar that starts full (start: max) up to its max, for when /setup
    // finishes and the chosen stats may have raised those maxes. Other bars keep
    // their value.
    public void fillBarsThatStartFull(ProfileKey character) {
        CharacterSheet sheet = require(character);
        for (StatDefinition stat : definitions.all()) {
            if (stat.isBar() && stat.bar().startAtMax()) {
                StatCalculator calculator = calculator(sheet);
                int before = calculator.total(stat);
                change(sheet, stat, calculator.max(stat) - calculator.extra(stat));
                report(sheet, stat, before);
            }
        }
        listener.onTotalsChanged(character);
    }

    public StoredModifier addModifier(ProfileKey character, String target, int amount) {
        if (!isModifierTarget(target)) {
            throw new IllegalArgumentException("'" + target + "' is not a stat or a bar max");
        }
        CharacterSheet sheet = require(character);
        StoredModifier modifier = new StoredModifier(UUID.randomUUID().toString(), target, amount);
        sheet.addModifier(modifier);
        repository.saveModifier(character, modifier);
        clampBars(sheet);
        listener.onTotalsChanged(character);
        return modifier;
    }

    public boolean removeModifier(ProfileKey character, String modifierId) {
        CharacterSheet sheet = require(character);
        if (!sheet.removeModifier(modifierId)) {
            return false;
        }
        repository.deleteModifier(modifierId);
        clampBars(sheet);
        listener.onTotalsChanged(character);
        return true;
    }

    public boolean isModifierTarget(String target) {
        return definitions.get(target).isPresent() || definitions.findBarByMaxId(target).isPresent();
    }

    // Replaces the effects of the character's injuries and conditions, after one was
    // given or removed. Every stat whose total changes is reported, and capped bars
    // above their new max come down to it (taking an effect away doesn't refill
    // them). Does nothing for a character without stats.
    public void setEffects(ProfileKey character, List<StatEffect> effects) {
        CharacterSheet sheet = sheets.get(character);
        if (sheet == null || sheet.effects().equals(effects)) {
            return;
        }
        Map<StatDefinition, Integer> before = totals(sheet);
        sheet.setEffects(effects);
        fitBars(sheet);
        reportChanges(sheet, before);
        listener.onTotalsChanged(character);
    }

    // Replaces the Skysong item counts seen in the player's inventory. They are
    // recounted, never saved, and don't fire change events.
    public void setItemCounts(ProfileKey character, Map<ItemTagSource, Integer> counts) {
        require(character).setItemCounts(counts);
        listener.onTotalsChanged(character);
    }

    // Sets a stat's base value, keeping its total within its min and (for capped
    // bars) its max. A height's base is kept within its scale's min and max (the
    // commands refuse heights outside them before getting here).
    private void change(CharacterSheet sheet, StatDefinition stat, int newBase) {
        int base = stat.isHeight() ? Math.clamp(newBase, stat.scale().min(), stat.scale().max()) : newBase;
        StatCalculator calculator = calculator(sheet);
        int extra = calculator.extra(stat);
        int total = base + extra;
        if (stat.isBar() && !stat.bar().canExceedMax()) {
            total = Math.min(total, calculator.max(stat));
        }
        if (stat.min() != null) {
            total = Math.max(total, stat.min());
        }
        save(sheet, stat, total - extra);
    }

    // Brings capped bars that are above their max (after a modifier or a stat
    // they depend on went down) back to it.
    private void clampBars(CharacterSheet sheet) {
        Map<StatDefinition, Integer> before = totals(sheet);
        fitBars(sheet);
        reportChanges(sheet, before);
    }

    private void fitBars(CharacterSheet sheet) {
        for (StatDefinition stat : definitions.all()) {
            if (stat.isBar() && !stat.bar().canExceedMax()) {
                StatCalculator calculator = calculator(sheet);
                if (calculator.total(stat) > calculator.max(stat)) {
                    change(sheet, stat, calculator.base(stat));
                }
            }
        }
    }

    private Map<StatDefinition, Integer> totals(CharacterSheet sheet) {
        StatCalculator calculator = calculator(sheet);
        Map<StatDefinition, Integer> totals = new LinkedHashMap<>();
        definitions.all().forEach(stat -> totals.put(stat, calculator.total(stat)));
        return totals;
    }

    // Reports every stat whose total is no longer what it was.
    private void reportChanges(CharacterSheet sheet, Map<StatDefinition, Integer> before) {
        before.forEach((stat, total) -> report(sheet, stat, total));
    }

    // Saves the starting value of every stat the character has no saved value for:
    // all of them for a new character, or the stats added to stats.yml since.
    private void saveMissingValues(CharacterSheet sheet) {
        StatCalculator calculator = calculator(sheet);
        for (StatDefinition stat : definitions.all()) {
            if (sheet.savedValue(stat.id()).isEmpty()) {
                save(sheet, stat, calculator.base(stat));
            }
        }
    }

    private void save(CharacterSheet sheet, StatDefinition stat, int base) {
        sheet.setValue(stat.id(), base);
        repository.saveValue(sheet.key(), stat.id(), base);
    }

    private StatChange report(CharacterSheet sheet, StatDefinition stat, int before) {
        StatChange change = new StatChange(sheet.key(), stat, before, calculator(sheet).total(stat));
        if (change.changed()) {
            listener.onChange(change);
        }
        return change;
    }

    private CharacterSheet require(ProfileKey character) {
        CharacterSheet sheet = sheets.get(character);
        if (sheet == null) {
            throw new IllegalArgumentException("No stats loaded for " + character);
        }
        return sheet;
    }
}
