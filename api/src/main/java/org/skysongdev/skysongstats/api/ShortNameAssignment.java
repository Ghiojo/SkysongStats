package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A short name a plugin gave a stat, as {@link Stats#assignShortName} returned it. Keep it to
 * take the short name off again.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.5.0
 */
@ApiStatus.NonExtendable
public interface ShortNameAssignment {

    /** The plugin that assigned it. */
    @NotNull Plugin owner();

    /** The id of the stat it is for. */
    @NotNull String statId();

    /** The short name, plain text, without spaces around it. */
    @NotNull String shortName();

    /** Whether it still exists: not unassigned, and its plugin is still enabled. */
    boolean isAssigned();

    /**
     * Whether the stat shows this short name now. An assignment that exists can still be out of
     * effect: a newer assignment for the same stat is, or after {@code /ssreload} the stat left
     * stats.yml or the name clashes with one of the file's names.
     */
    boolean isActive();

    /**
     * Takes the short name off. The stat goes back to the assignment before it, or to
     * stats.yml. Doing it again does nothing. Must be called on the server's main thread.
     *
     * @throws IllegalStateException when called from another thread
     */
    void unassign();
}
