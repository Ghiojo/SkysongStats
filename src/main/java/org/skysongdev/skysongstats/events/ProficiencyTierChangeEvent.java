package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;

// Fired after a character's proficiency tier changed: a tier-up from training,
// /setup, or /proficiency tier set (which can also lower it, see
// getChange().tierUp()). Other plugins can listen to it.
public class ProficiencyTierChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ProficiencyTierChange change;

    public ProficiencyTierChangeEvent(ProficiencyTierChange change) {
        this.change = change;
    }

    public ProficiencyTierChange getChange() {
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
