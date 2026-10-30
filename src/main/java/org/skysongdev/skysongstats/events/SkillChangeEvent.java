package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.SkillChange;

// Fired after a character's skill XP changed: /skills addxp or setxp, or the
// skills picked in /setup. Other plugins can listen to it.
public class SkillChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SkillChange change;

    public SkillChangeEvent(SkillChange change) {
        this.change = change;
    }

    public SkillChange getChange() {
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
