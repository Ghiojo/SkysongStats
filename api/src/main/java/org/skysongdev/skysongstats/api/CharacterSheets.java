package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The character sheet fields: name, age, gender, ancestry, pronouns and description, the
 * ones {@code /character set} changes.
 *
 * <p>Must be called on the server's main thread, and throws {@link IllegalStateException}
 * otherwise. Writes don't check permissions: a plugin acting for a player checks that
 * player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface CharacterSheets {

    /**
     * The character's sheet.
     *
     * @return the sheet, empty when the character doesn't exist
     */
    @NotNull Optional<CharacterSheet> get(@NotNull CharacterId character);

    /**
     * Sets one field and saves it straight away. The value is saved as given; the sheets
     * show it with the MiniMessage formats players may use in chat.
     *
     * @param value 1 to {@link SheetField#maxLength()} characters, not blank
     * @return the sheet after the change, empty when the character doesn't exist
     * @throws IllegalArgumentException when the value is blank or longer than the field takes
     * @throws IllegalStateException    when the database refused the change; the sheet keeps its old value
     */
    @NotNull Optional<CharacterSheet> set(@NotNull CharacterId character, @NotNull SheetField field,
                                          @NotNull String value);
}
