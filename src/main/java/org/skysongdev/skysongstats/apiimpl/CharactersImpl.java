package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Characters;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.character.PluginCharacterService;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// Plugin characters answered by the plugin character service. Owners are told
// apart by their plugin.yml name.
final class CharactersImpl implements Characters {
    // The profile column is VARCHAR(20), as for players' profiles.
    private static final int MAX_PROFILE_LENGTH = 20;
    private static final String CHARACTER = "character";
    private static final String OWNER = "owner";

    private final PluginCharacterService characters;
    private final Supplier<StatDefinitions> stats;
    private final BiConsumer<ProfileKey, List<ItemStack>> countItems;
    private final MainThread mainThread;

    CharactersImpl(PluginCharacterService characters, Supplier<StatDefinitions> stats,
                   BiConsumer<ProfileKey, List<ItemStack>> countItems, MainThread mainThread) {
        this.characters = characters;
        this.stats = stats;
        this.countItems = countItems;
        this.mainThread = mainThread;
    }

    @Override
    public boolean create(@NotNull Plugin owner, @NotNull CharacterId character,
                          @NotNull Map<Stat, Integer> startingValues) {
        Objects.requireNonNull(owner, OWNER);
        Objects.requireNonNull(character, CHARACTER);
        Objects.requireNonNull(startingValues, "startingValues");
        mainThread.check();
        if (!owner.isEnabled()) {
            throw new IllegalStateException(owner.getName() + " is not enabled: create characters once it has enabled");
        }
        String profile = character.profile();
        if (profile.isEmpty() || profile.length() > MAX_PROFILE_LENGTH) {
            throw new IllegalArgumentException("A character's profile name must be 1 to " + MAX_PROFILE_LENGTH
                    + " characters long: '" + profile + "'");
        }
        return characters.create(owner.getName(), ApiTypes.key(character), statIds(startingValues));
    }

    @Override
    public boolean delete(@NotNull Plugin owner, @NotNull CharacterId character) {
        Objects.requireNonNull(owner, OWNER);
        Objects.requireNonNull(character, CHARACTER);
        mainThread.check();
        return characters.delete(owner.getName(), ApiTypes.key(character));
    }

    @Override
    public @NotNull Optional<String> owner(@NotNull CharacterId character) {
        Objects.requireNonNull(character, CHARACTER);
        mainThread.check();
        return characters.owner(ApiTypes.key(character));
    }

    @Override
    public @NotNull List<CharacterId> ownedBy(@NotNull Plugin owner) {
        Objects.requireNonNull(owner, OWNER);
        mainThread.check();
        return characters.ownedBy(owner.getName()).stream().map(ApiTypes::character).toList();
    }

    @Override
    public boolean setItems(@NotNull CharacterId character, @NotNull Collection<? extends ItemStack> items) {
        Objects.requireNonNull(character, CHARACTER);
        Objects.requireNonNull(items, "items");
        mainThread.check();
        ProfileKey key = ApiTypes.key(character);
        if (!characters.isPluginCharacter(key)) {
            return false;
        }
        // A copy: the plugin may change its collection afterwards. Nulls are empty slots.
        countItems.accept(key, new ArrayList<>(items));
        return true;
    }

    // The values by stat id, checked against stats.yml as it is now: a Stat kept
    // across /ssreload may have been removed from it.
    private Map<String, Integer> statIds(Map<Stat, Integer> startingValues) {
        StatDefinitions definitions = stats.get();
        Map<String, Integer> values = new LinkedHashMap<>();
        startingValues.forEach((stat, value) -> {
            Objects.requireNonNull(stat, "stat");
            Objects.requireNonNull(value, "value");
            if (definitions.get(stat.id()).isEmpty()) {
                throw new IllegalArgumentException("The stat " + stat.id() + " is not in stats.yml anymore");
            }
            values.put(stat.id(), value);
        });
        return values;
    }
}
