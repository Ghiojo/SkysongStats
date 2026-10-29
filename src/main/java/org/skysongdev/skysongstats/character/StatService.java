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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
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
    private StatDefinitions definitions;

    public StatService(StatDefinitions definitions, StatRepository repository, StatChangeListener listener) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public void load() {
        sheets.clear();
        repository.loadAll().forEach((key, stored) -> sheets.put(key, new CharacterSheet(key, stored)));
    }

    public StatDefinitions definitions() {
        return definitions;
    }

    // Swaps in a reloaded stats.yml. Bars that may no longer be above their max are
    // brought down to it.
    public void setDefinitions(StatDefinitions definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        sheets.values().forEach(this::clampBars);
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

    // Creates a new character's sheet. Bars are saved at their start value so that
    // a full bar doesn't keep following its max as stats change later.
    public CharacterSheet createCharacter(ProfileKey character) {
        CharacterSheet sheet = new CharacterSheet(character, new StoredStats(Map.of(), List.of()));
        StatCalculator calculator = calculator(sheet);
        for (StatDefinition stat : definitions.all()) {
            if (stat.isBar()) {
                save(sheet, stat, calculator.base(stat));
            }
        }
        sheets.put(character, sheet);
        return sheet;
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
        }
        clampBars(sheet);
        return report(sheet, stat, before);
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

    public StoredModifier addModifier(ProfileKey character, String target, int amount) {
        if (!isModifierTarget(target)) {
            throw new IllegalArgumentException("'" + target + "' is not a stat or a bar max");
        }
        CharacterSheet sheet = require(character);
        StoredModifier modifier = new StoredModifier(UUID.randomUUID().toString(), target, amount);
        sheet.addModifier(modifier);
        repository.saveModifier(character, modifier);
        clampBars(sheet);
        return modifier;
    }

    public boolean removeModifier(ProfileKey character, String modifierId) {
        CharacterSheet sheet = require(character);
        if (!sheet.removeModifier(modifierId)) {
            return false;
        }
        repository.deleteModifier(modifierId);
        clampBars(sheet);
        return true;
    }

    public boolean isModifierTarget(String target) {
        return definitions.get(target).isPresent() || definitions.findBarByMaxId(target).isPresent();
    }

    // Replaces the Skysong item counts seen in the player's inventory. They are
    // recounted, never saved, and don't fire change events.
    public void setItemCounts(ProfileKey character, Map<ItemTagSource, Integer> counts) {
        require(character).setItemCounts(counts);
    }

    // Sets a stat's base value, keeping its total within its min and (for capped
    // bars) its max.
    private void change(CharacterSheet sheet, StatDefinition stat, int newBase) {
        StatCalculator calculator = calculator(sheet);
        int extra = calculator.extra(stat);
        int total = newBase + extra;
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
        for (StatDefinition stat : definitions.all()) {
            if (stat.isBar() && !stat.bar().canExceedMax()) {
                StatCalculator calculator = calculator(sheet);
                int before = calculator.total(stat);
                if (before > calculator.max(stat)) {
                    change(sheet, stat, calculator.base(stat));
                    report(sheet, stat, before);
                }
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
