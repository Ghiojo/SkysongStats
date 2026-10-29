package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.StatChange;

// Fired after a stat of a character changed: a command, a modifier, a reload that
// brought a bar down to its max... Other plugins can listen to it.
public class StatChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final StatChange change;

    public StatChangeEvent(StatChange change) {
        this.change = change;
    }

    public StatChange getChange() {
        return change;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
