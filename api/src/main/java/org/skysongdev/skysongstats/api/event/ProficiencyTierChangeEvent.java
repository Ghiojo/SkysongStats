package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.TierChange;

import java.util.Objects;

/**
 * Fired after a character's tier in a proficiency changed, up or down: a training tier-up,
 * {@code /setup}, {@code /proficiency tier set}, or the API. One event for both
 * directions: {@link TierChange#tierUp()} tells which.
 *
 * <p>Fired on the server's main thread, once per change, and only when the tier is
 * different afterwards. The LuckPerms tier nodes already follow when it fires. Not
 * cancellable: the change has already happened.
 */
public final class ProficiencyTierChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final TierChange change;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public ProficiencyTierChangeEvent(@NotNull TierChange change, @NotNull ChangeCause cause) {
        this.change = Objects.requireNonNull(change, "change");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character, the proficiency, and its tier before and after. */
    public @NotNull TierChange getChange() {
        return change;
    }

    /** What changed the tier. */
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
