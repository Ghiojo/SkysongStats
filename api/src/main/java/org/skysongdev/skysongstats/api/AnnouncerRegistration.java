package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A plugin's claim to announce injury award messages, as
 * {@link Injuries#announceAwards(Plugin)} returned it.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.2.0
 */
@ApiStatus.NonExtendable
public interface AnnouncerRegistration {

    /** The plugin that announces. */
    @NotNull Plugin owner();

    /** Whether it still announces: not unregistered, and its plugin is still enabled. */
    boolean isRegistered();

    /**
     * Stops announcing. When no other plugin announces, SkysongStats sends award messages
     * to the injured player again. Doing it again does nothing.
     */
    void unregister();
}
