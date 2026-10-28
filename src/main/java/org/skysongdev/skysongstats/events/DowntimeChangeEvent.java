package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skysongdev.skysongstats.service.downtime.DowntimeChange;
import org.skysongdev.skysongstats.service.downtime.DowntimePool;

import java.util.UUID;

/**
 * Fired after a player's global or character downtime changes and the change
 * is saved: by add, remove or resetall. It cannot be cancelled.
 */
public class DowntimeChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final DowntimeChange change;

    public DowntimeChangeEvent(DowntimeChange change) {
        this.change = change;
    }

    public DowntimeChange getChange() {
        return change;
    }

    public UUID getPlayerUUID() {
        return change.player();
    }

    public DowntimePool getPool() {
        return change.pool();
    }

    /** The profile for character downtime, {@code null} for global downtime. */
    public String getProfile() {
        return change.profile();
    }

    public int getOldValue() {
        return change.oldValue();
    }

    public int getNewValue() {
        return change.newValue();
    }

    public DowntimeChange.Cause getCause() {
        return change.cause();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
