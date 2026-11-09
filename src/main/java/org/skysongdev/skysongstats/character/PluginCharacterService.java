package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.storage.PluginCharacterRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Characters another plugin owns instead of a player's profile, such as an NPC
// plugin's NPCs. They have stats, modifiers, skills, proficiencies, a character
// sheet and injuries like any profile, but no /setup, no active profile, no
// downtime and no LuckPerms nodes. This service only knows which characters are
// plugin characters and who owns them; what creating and deleting a character
// does to the rest of the plugin is the CharacterLifecycle's job.
public class PluginCharacterService {

    // What creating and deleting a character means for every part of the plugin.
    public interface CharacterLifecycle {
        // Whether the id is already a character: a player's profile or a plugin character.
        boolean exists(ProfileKey character);

        // Creates the character with some stats at the given base values. Throws
        // IllegalArgumentException, before creating anything, for an id that isn't a stat.
        void create(ProfileKey character, Map<String, Integer> startingValues);

        // Removes the character and all its data.
        void delete(ProfileKey character);
    }

    private final PluginCharacterRepository repository;
    private final CharacterLifecycle lifecycle;
    private final Map<ProfileKey, String> owners = new LinkedHashMap<>();

    public PluginCharacterService(PluginCharacterRepository repository, CharacterLifecycle lifecycle) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public void load() {
        owners.clear();
        owners.putAll(repository.loadAll());
    }

    public boolean isPluginCharacter(ProfileKey character) {
        return owners.containsKey(character);
    }

    // The name of the plugin that owns the character; empty for a player's profile.
    public Optional<String> owner(ProfileKey character) {
        return Optional.ofNullable(owners.get(character));
    }

    // The plugin's characters, oldest first.
    public List<ProfileKey> ownedBy(String owner) {
        return owners.entrySet().stream()
                .filter(entry -> entry.getValue().equals(owner))
                .map(Map.Entry::getKey)
                .toList();
    }

    // The characters of these that are players' profiles, not plugin characters.
    public Set<ProfileKey> playerCharacters(Set<ProfileKey> characters) {
        return characters.stream().filter(character -> !owners.containsKey(character))
                .collect(Collectors.toUnmodifiableSet());
    }

    // Creates a plugin character owned by owner. False, and nothing changes, when
    // the id is already a character. Throws IllegalArgumentException for a
    // starting value of an id that isn't a stat.
    public boolean create(String owner, ProfileKey character, Map<String, Integer> startingValues) {
        Objects.requireNonNull(owner, "owner");
        if (owners.containsKey(character) || lifecycle.exists(character)) {
            return false;
        }
        lifecycle.create(character, startingValues);
        owners.put(character, owner);
        repository.save(character, owner);
        return true;
    }

    // Deletes a plugin character and all its data. False, and nothing changes,
    // when the character isn't a plugin character of this owner.
    public boolean delete(String owner, ProfileKey character) {
        if (!Objects.equals(owners.get(character), owner)) {
            return false;
        }
        lifecycle.delete(character);
        owners.remove(character);
        repository.delete(character);
        return true;
    }
}
