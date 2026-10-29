package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillLevel;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.SkillRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

// Every character's skill XP. Changes are saved through the repository and
// reported to the listener. Characters are kept in memory, loaded once at
// startup, like StatService. Call it from the server thread only.
//
// XP is never below 0. A skill's level comes from its XP and the shared levels
// in skills.yml.
public class SkillService {
    private final SkillRepository repository;
    private final SkillChangeListener listener;
    private final Map<ProfileKey, Map<String, Integer>> characters = new HashMap<>();
    private SkillsConfig config;

    public SkillService(SkillsConfig config, SkillRepository repository, SkillChangeListener listener) {
        this.config = Objects.requireNonNull(config, "config");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public void load() {
        characters.clear();
        repository.loadAll().forEach((key, xp) -> characters.put(key, new HashMap<>(xp)));
        characters.keySet().forEach(this::saveMissingSkills);
    }

    public SkillsConfig config() {
        return config;
    }

    // Swaps in a reloaded skills.yml. Skills new to the file are saved at their
    // baseline. XP of skills that left the file stays saved but unused.
    public void setConfig(SkillsConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        characters.keySet().forEach(this::saveMissingSkills);
    }

    // Every character with skills: one per profile.
    public Set<ProfileKey> characters() {
        return Set.copyOf(characters.keySet());
    }

    public boolean hasCharacter(ProfileKey character) {
        return characters.containsKey(character);
    }

    // The skill's XP, or empty when the character has no skills loaded.
    public OptionalInt xp(ProfileKey character, SkillDefinition skill) {
        Map<String, Integer> xp = characters.get(character);
        return xp == null ? OptionalInt.empty() : OptionalInt.of(xp.getOrDefault(skill.id(), skill.baseline()));
    }

    public SkillLevel level(ProfileKey character, SkillDefinition skill) {
        return config.levels().levelFor(require(character, skill));
    }

    // Creates a new character and saves every skill's baseline, so the database
    // holds the whole character from the start.
    public void createCharacter(ProfileKey character) {
        characters.put(character, new HashMap<>());
        saveMissingSkills(character);
    }

    // Creates the character first if it has no skills yet: for profiles made
    // before the skill table existed.
    public void ensureCharacter(ProfileKey character) {
        if (!characters.containsKey(character)) {
            createCharacter(character);
        }
    }

    // Removes a character's XP, for deleted profiles.
    public void deleteCharacter(ProfileKey character) {
        characters.remove(character);
        repository.deleteCharacter(character);
    }

    public SkillChange setXp(ProfileKey character, SkillDefinition skill, int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP can't be negative");
        }
        int before = require(character, skill);
        save(character, skill, xp);
        SkillChange change = new SkillChange(character, skill, before, xp);
        if (change.changed()) {
            listener.onChange(change);
        }
        return change;
    }

    // Adds XP, stopping at the largest whole number the database can hold.
    public SkillChange addXp(ProfileKey character, SkillDefinition skill, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative");
        }
        long total = (long) require(character, skill) + amount;
        return setXp(character, skill, (int) Math.min(total, Integer.MAX_VALUE));
    }

    private void saveMissingSkills(ProfileKey character) {
        Map<String, Integer> xp = characters.get(character);
        for (SkillDefinition skill : config.skills().all()) {
            if (!xp.containsKey(skill.id())) {
                save(character, skill, skill.baseline());
            }
        }
    }

    private void save(ProfileKey character, SkillDefinition skill, int xp) {
        characters.get(character).put(skill.id(), xp);
        repository.saveXp(character, skill.id(), xp);
    }

    private int require(ProfileKey character, SkillDefinition skill) {
        return xp(character, skill).orElseThrow(
                () -> new IllegalArgumentException("No skills loaded for " + character));
    }
}
