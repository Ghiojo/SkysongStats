package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.DowntimeChange;

import java.util.Objects;

/**
 * Fired after a global or character downtime pool changed and the change is saved:
 * {@code /downtime add}, {@code remove} or {@code resetall}, or the API. A reset fires one
 * event per pool that changed.
 *
 * <p>Fired on the server's main thread, only when the downtime is different afterwards.
 * Not cancellable: the change has already happened.
 */
public final class DowntimeChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final DowntimeChange change;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public DowntimeChangeEvent(@NotNull DowntimeChange change, @NotNull ChangeCause cause) {
        this.change = Objects.requireNonNull(change, "change");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The pool, the kind of change, and the downtime before and after. */
    public @NotNull DowntimeChange getChange() {
        return change;
    }

    /** What made the change. */
    public @NotNull ChangeCause getCause() {
        return cause;
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
