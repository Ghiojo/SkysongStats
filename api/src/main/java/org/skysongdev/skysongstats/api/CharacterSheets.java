package org.skysongdev.skysongstats.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The character sheet fields: name, age, gender, ancestry, pronouns and description, the
 * ones {@code /character set} changes, and the sheets of messages.yml rendered for any
 * character, a player's profile or another plugin's character such as an NPC.
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
     * @throws IllegalStateException    when the database refused the change; the sheet keeps its old value.
     *                                  Since 1.4.0 also for {@link SheetField#ANCESTRY} on a character with
     *                                  an ancestry ({@link Ancestries}): its field then follows the ancestry
     */
    @NotNull Optional<CharacterSheet> set(@NotNull CharacterId character, @NotNull SheetField field,
                                          @NotNull String value);

    /**
     * One of SkysongStats' sheets for a character, as the server's messages.yml lays it
     * out, ready to send: the same lines its command shows, filled in for this character.
     * {@code {player}} is the name given, for example an NPC's display name. The
     * {@code {injuries...}} and {@code {conditions...}} sheet tags show this character's.
     *
     * <p>Other plugins' PlaceholderAPI placeholders are not filled in: they show a
     * player's active profile, not this character.
     *
     * @param character the character, a player's profile or a plugin character
     * @param layout    which sheet
     * @param name      what {@code {player}} shows
     * @return one component per line, empty when the character doesn't exist
     * @throws IllegalStateException when called from another thread
     * @since 1.3.0
     */
    @NotNull Optional<List<Component>> render(@NotNull CharacterId character, @NotNull SheetLayout layout,
                                              @NotNull String name);

    /**
     * Lines a plugin lays out itself, filled in for a character like SkysongStats' own
     * sheets: MiniMessage text with the same {@code {placeholders}} the sheets of
     * messages.yml use. Every stat's {@code {<id>}}, {@code {<id>_base}},
     * {@code {<id>_modifiers}} and, for bars, {@code {max_<id>}}, {@code {max_<id>_base}},
     * {@code {max_<id>_modifiers}}; {@code {height}} and {@code {height_feet}};
     * {@code {name}}, {@code {age}}, {@code {gender}}, {@code {pronouns}},
     * {@code {ancestry}} and {@code {description}} from the character sheet;
     * {@code {focus}} and {@code {proficiencies}} as the proficiency sheet shows them;
     * the {@code {injuries...}} and {@code {conditions...}} sheet tags; {@code {prefix}};
     * and {@code {player}}, the name given. PlaceholderAPI placeholders are not filled in.
     *
     * @param character the character, a player's profile or a plugin character
     * @param lines     the lines, in MiniMessage
     * @param name      what {@code {player}} shows
     * @return one component per line, empty when the character doesn't exist
     * @throws IllegalStateException when called from another thread
     * @since 1.3.0
     */
    @NotNull Optional<List<Component>> render(@NotNull CharacterId character, @NotNull List<String> lines,
                                              @NotNull String name);
}
