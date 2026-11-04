package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.SkillChange;

import java.util.Objects;

/**
 * Fired after a character's XP in a skill changed: {@code /skills addxp} or {@code setxp},
 * {@code /setup}, or the API.
 *
 * <p>Fired on the server's main thread, once per change, and only when the XP is different
 * afterwards. Not cancellable: the change has already happened.
 */
public final class SkillChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final SkillChange change;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public SkillChangeEvent(@NotNull SkillChange change, @NotNull ChangeCause cause) {
        this.change = Objects.requireNonNull(change, "change");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character, the skill, and the XP before and after. */
    public @NotNull SkillChange getChange() {
        return change;
    }

    /** What changed the XP. */
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
