package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.StatChange;

import java.util.Objects;

/**
 * Fired after a stat's total changed: a stat command, a modifier, {@code /setup}, a
 * {@code /ssreload} that brought a bar down to a lower max, or the API. For a bar the
 * total is its current value. A damage that a shield absorbs fires one event for the
 * shield and one for the bar. Item recounts don't fire it.
 *
 * <p>Fired on the server's main thread, once per change, and only when the total is
 * different afterwards. Not cancellable: the change has already happened.
 */
public final class StatChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final StatChange change;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public StatChangeEvent(@NotNull StatChange change, @NotNull ChangeCause cause) {
        this.change = Objects.requireNonNull(change, "change");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character, the stat, and its total before and after. */
    public @NotNull StatChange getChange() {
        return change;
    }

    /** What changed the stat. */
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
