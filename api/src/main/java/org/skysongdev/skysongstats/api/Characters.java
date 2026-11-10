package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin characters: characters another plugin creates and owns instead of a player's
 * profile, such as an NPC plugin's NPCs. A plugin character is a {@link CharacterId} like
 * any other, so every other part of the API (stats, modifiers, skills, proficiencies, the
 * character sheet, injuries and conditions) reads and changes it the same way, and their
 * events fire for it too.
 *
 * <pre>{@code
 * CharacterId guard = new CharacterId(npc.getUniqueId(), "npc");
 * Characters characters = SkysongStatsApi.get().characters();
 * if (characters.owner(guard).isEmpty()) {
 *     characters.create(this, guard, Map.of(stats.find("str").orElseThrow(), 4));
 * }
 * SkysongStatsApi.get().stats().apply(guard, "hp", StatAction.DAMAGE, 5);
 * }</pre>
 *
 * <p>A plugin character belongs to no player, so it has none of what belongs to players:
 * no {@code /setup}, no active profile, no {@code /profile} listing, no downtime, no
 * LuckPerms nodes for its skill levels or proficiency tiers, and PlaceholderAPI never
 * shows it. {@link Profiles} doesn't list it, and SkysongStats' commands never target it:
 * the owning plugin offers its own.
 *
 * <p>Plugin characters are saved like profiles and stay when their plugin disables or is
 * removed, so reinstalling the plugin brings them back. Only the owner deletes them.
 *
 * <p>Must be called on the server's main thread, and throws {@link IllegalStateException}
 * otherwise.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.3.0
 */
@ApiStatus.NonExtendable
public interface Characters {

    /**
     * Creates a plugin character, saved straight away. Its stats start at their stats.yml
     * start values, except the ones in {@code startingValues}, which start at that base
     * value as if set with {@link StatAction#SET}; the others are worked out from them, so
     * a bar that starts full starts at the max the given stats make. Values are kept within
     * each stat's limits. It starts with no skill XP, proficiencies, modifiers, injuries or
     * conditions. No change events fire: the character is new.
     *
     * @param owner          the plugin that owns the character; it must be enabled
     * @param character      the new character's id, for example the NPC's UUID and
     *                       {@code "npc"}; the profile is 1 to 20 characters long
     * @param startingValues base values for some stats, the others left out
     * @return true when the character was created, false when the id is already a
     *         character (a player's profile or a plugin character), and nothing changed
     * @throws IllegalArgumentException when the profile name is empty or longer than 20
     *                                  characters, or a stat is not in stats.yml anymore
     * @throws IllegalStateException    when the plugin is not enabled, or when called
     *                                  from another thread
     */
    boolean create(@NotNull Plugin owner, @NotNull CharacterId character,
                   @NotNull Map<Stat, Integer> startingValues);

    /**
     * Creates a plugin character with every stat at its stats.yml start value. Same as
     * {@link #create(Plugin, CharacterId, Map)} with no starting values.
     *
     * @param owner     the plugin that owns the character; it must be enabled
     * @param character the new character's id
     * @return true when the character was created, false when the id is already a character
     * @throws IllegalArgumentException when the profile name is empty or longer than 20 characters
     * @throws IllegalStateException    when the plugin is not enabled, or when called
     *                                  from another thread
     */
    default boolean create(@NotNull Plugin owner, @NotNull CharacterId character) {
        return create(owner, character, Map.of());
    }

    /**
     * Deletes a plugin character and everything SkysongStats keeps for it. Only its owner
     * can delete it.
     *
     * @param owner     the plugin that owns the character
     * @param character the character
     * @return true when it was deleted, false when it isn't a plugin character of this
     *         plugin, and nothing changed
     * @throws IllegalStateException when called from another thread
     */
    boolean delete(@NotNull Plugin owner, @NotNull CharacterId character);

    /**
     * The plugin that owns a character.
     *
     * @param character the character
     * @return the owning plugin's name, as its plugin.yml has it; empty for a player's
     *         profile and for an id that is no character
     * @throws IllegalStateException when called from another thread
     */
    @NotNull Optional<String> owner(@NotNull CharacterId character);

    /**
     * Every character a plugin owns, oldest first.
     *
     * @param owner the plugin
     * @return its characters, an unmodifiable list
     * @throws IllegalStateException when called from another thread
     */
    @NotNull List<CharacterId> ownedBy(@NotNull Plugin owner);
}
