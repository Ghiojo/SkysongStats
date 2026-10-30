package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;

// Fired after a character's proficiency focus changed: a new focus, points added,
// removed or set, or the focus cleared at the top tier. Other plugins can listen
// to it.
public class ProficiencyFocusChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ProficiencyFocusChange change;

    public ProficiencyFocusChangeEvent(ProficiencyFocusChange change) {
        this.change = change;
    }

    public ProficiencyFocusChange getChange() {
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
