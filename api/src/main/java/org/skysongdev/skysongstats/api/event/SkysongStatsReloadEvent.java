package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after {@code /ssreload}, once the reloaded config files are in use. Listen to it to
 * check your own configuration again, for example skill names your files refer to: a skill,
 * stat or proficiency may have been added, renamed or removed.
 *
 * <p>A file with mistakes is not reloaded and keeps what it had, so this also fires when
 * only some files changed. Fired on the server's main thread.
 */
public final class SkysongStatsReloadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The handlers of this event, as every Bukkit event needs. */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
