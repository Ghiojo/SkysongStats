package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The injuries of injuries.yml, what each character has now, and which injuries each
 * character knows (what {@code /injuries} shows).
 *
 * <p>An injury is found by its id, ignoring case. The methods about injuries.yml itself
 * ({@link #types()}, {@link #type(String)}, {@link #severities()}, {@link #tag(String)}) are
 * safe from any thread. The others must be called on the server's main thread, and throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like {@code /injury}: saved straight away, the stats follow the injury's
 * effects, and the injury and condition events fire with {@code ChangeCause.API}. They
 * don't check permissions.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.2.0
 */
@ApiStatus.NonExtendable
public interface Injuries {

    /** Every injury, in injuries.yml order. An unmodifiable list. */
    @NotNull List<InjuryType> types();

    /**
     * The injury with this id, ignoring case.
     *
     * @return the injury, empty when injuries.yml has none with that id
     */
    @NotNull Optional<InjuryType> type(@NotNull String id);

    /** Every severity of injuries.yml, in its order. Not every severity has injuries. */
    @NotNull List<String> severities();

    /** The character's current injuries, oldest first; empty for an unknown character. */
    @NotNull List<Injury> current(@NotNull CharacterId character);

    /** Whether the character has the injury now. */
    boolean has(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * Gives the character an injury and teaches it to them, like {@code /injury award}.
     * Giving one it already has keeps one, with the new time. The award message goes out
     * as described at {@link #announceAwards(Plugin)}.
     *
     * @return the injury, empty when the character or the injury doesn't exist
     */
    @NotNull Optional<Injury> award(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * Takes an injury away, and with it its conditions and effects. The character still
     * knows it.
     *
     * @return whether the character had it
     */
    boolean remove(@NotNull CharacterId character, @NotNull String injuryId);

    /** The ids of the injuries the character knows, in the order it learnt them. Unmodifiable. */
    @NotNull Set<String> known(@NotNull CharacterId character);

    /** Whether the character knows the injury. */
    boolean knows(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * Teaches the character an injury without giving it, for example when it hears about
     * one in chat.
     *
     * @return true when it is newly learnt; false when it was known already, or the
     *         character or the injury doesn't exist
     */
    boolean discover(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * Undoes a discover, like {@code /injury forget}. Refused while the character has the
     * injury.
     *
     * @return whether the character no longer knows it because of this call
     */
    boolean forget(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * How the injury shows in chat ({@code [injury_x]}): its name with the description on
     * hover, in the look of {@code injury.tag} in messages.yml.
     *
     * @return the tag as MiniMessage, empty when injuries.yml has no such injury
     */
    @NotNull Optional<String> tag(@NotNull String injuryId);

    /**
     * The injury's award message for this character, with {@code {character}} (the
     * character's name) and {@code {injury}} (its {@link #tag(String) tag}) filled in.
     *
     * @return the message as MiniMessage, empty when the injury has no message or doesn't exist
     */
    @NotNull Optional<String> message(@NotNull CharacterId character, @NotNull String injuryId);

    /**
     * Takes over saying injury award messages. When an injury with a message is awarded,
     * exactly one of two things happens:
     * <ul>
     *     <li>at least one plugin is registered: SkysongStats sends nothing, and the plugin
     *     says {@link org.skysongdev.skysongstats.api.event.InjuryAwardEvent#getMessage()}
     *     wherever it likes (a chat channel, for example);</li>
     *     <li>none is: SkysongStats sends the message to the injured player, if online.</li>
     * </ul>
     * The registration ends with {@link AnnouncerRegistration#unregister()}, or when the
     * plugin disables, so a plugin calls this in its {@code onEnable}.
     *
     * @param plugin the plugin that will announce; it must be enabled
     * @return the registration
     * @throws IllegalStateException when the plugin is not enabled, or when called from
     *                               another thread
     */
    @NotNull AnnouncerRegistration announceAwards(@NotNull Plugin plugin);
}
