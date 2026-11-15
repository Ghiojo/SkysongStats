package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.Stat;

/**
 * Fired when the short name a stat shows changes because a plugin assigned or unassigned one,
 * or a plugin with assignments disabled. A short name changed in stats.yml comes with
 * {@link SkysongStatsReloadEvent} instead. Fired on the server's main thread, after the change.
 *
 * @since 1.5.0
 */
public final class StatShortNameChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Stat stat;
    private final String oldShortName;

    /**
     * @param stat         the stat, with its new short name
     * @param oldShortName the short name it showed before
     */
    public StatShortNameChangeEvent(@NotNull Stat stat, @NotNull String oldShortName) {
        this.stat = stat;
        this.oldShortName = oldShortName;
    }

    /** The stat, with its new {@link Stat#shortName()}. */
    public @NotNull Stat getStat() {
        return stat;
    }

    /** The short name it showed before. */
    public @NotNull String getOldShortName() {
        return oldShortName;
    }

    /** The short name it shows now, the same as {@code getStat().shortName()}. */
    public @NotNull String getNewShortName() {
        return stat.shortName();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The handlers of this event, as every Bukkit event needs. */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
