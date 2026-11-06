package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A registered {@link ItemSource}, as {@link ItemSources#register} returned it. Keep it to
 * remove the source later.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface ItemSourceRegistration {

    /** The plugin that registered the source. */
    @NotNull Plugin owner();

    /** The source's id, unique within its plugin. */
    @NotNull String id();

    /** Whether the source still counts: not unregistered, and its plugin is still enabled. */
    boolean isRegistered();

    /**
     * Removes the source, and recounts online players without it. Doing it again does
     * nothing. Must be called on the server's main thread.
     *
     * @throws IllegalStateException when called from another thread
     */
    void unregister();
}
