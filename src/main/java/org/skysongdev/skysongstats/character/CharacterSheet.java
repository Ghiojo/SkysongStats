package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

// One character's stat data: the saved base values by stat id, the modifiers, and
// the Skysong item counts last seen in the player's inventory (never saved).
// Computed values (totals, bar maximums) are never stored here; StatCalculator
// derives them. Only StatService changes a sheet, so every change is saved.
public final class CharacterSheet {
    private final ProfileKey key;
    private final Map<String, Integer> values;
    private final List<StoredModifier> modifiers;
    private final Map<ItemTagSource, Integer> itemCounts = new HashMap<>();

    CharacterSheet(ProfileKey key, StoredStats stored) {
        this.key = Objects.requireNonNull(key, "key");
        this.values = new HashMap<>(stored.values());
        this.modifiers = new ArrayList<>(stored.modifiers());
    }

    public ProfileKey key() {
        return key;
    }

    // The saved base value of a stat, or empty when none is saved yet (the stat is
    // then at its baseline or, for a bar, its start value).
    public OptionalInt savedValue(String statId) {
        Integer value = values.get(statId);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public List<StoredModifier> modifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public Optional<StoredModifier> modifier(String id) {
        return modifiers.stream().filter(modifier -> modifier.id().equals(id)).findFirst();
    }

    public int itemCount(ItemTagSource source) {
        return itemCounts.getOrDefault(source, 0);
    }

    void setValue(String statId, int value) {
        values.put(statId, value);
    }

    void addModifier(StoredModifier modifier) {
        modifiers.add(modifier);
    }

    boolean removeModifier(String id) {
        return modifiers.removeIf(modifier -> modifier.id().equals(id));
    }

    void setItemCounts(Map<ItemTagSource, Integer> counts) {
        itemCounts.clear();
        itemCounts.putAll(counts);
    }
}
