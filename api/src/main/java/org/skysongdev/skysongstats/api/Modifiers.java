package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The modifiers saved on each character, the same ones {@code /modifier} lists, adds and
 * removes. A modifier added here is an ordinary saved modifier: players and staff see it
 * in {@code /modifier view} and can remove it.
 *
 * <p>Must be called on the server's main thread, and throws {@link IllegalStateException}
 * otherwise. Writes don't check permissions: a plugin acting for a player checks that
 * player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Modifiers {

    /**
     * A character's modifiers, oldest first.
     *
     * @return an unmodifiable list, empty when the character has none or doesn't exist
     */
    @NotNull List<Modifier> list(@NotNull CharacterId character);

    /**
     * Adds a modifier to a character, like {@code /modifier add}. It is saved straight away,
     * and a capped bar above its new max comes down to it.
     *
     * @param target a stat's id or alias, or {@code max_<bar>} for a bar's max
     * @param amount what it adds; negative takes off
     * @return the new modifier, empty when the character doesn't exist or the target is
     *         neither a stat nor a bar's max
     */
    @NotNull Optional<Modifier> add(@NotNull CharacterId character, @NotNull String target, int amount);

    /**
     * Removes one of a character's modifiers, like {@code /modifier del}.
     *
     * @param modifierId the modifier's {@link Modifier#id()}
     * @return whether the character had that modifier
     */
    boolean remove(@NotNull CharacterId character, @NotNull String modifierId);
}
