package org.skysongdev.skysongstats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SkillUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerUUID;
    private final Utils.Skills skill;
    private final String profile;
    private final Player player;

    public SkillUpdateEvent(Player player, String playerUUID, Utils.Skills skill) {
        this.playerUUID = playerUUID;
        this.skill = skill;
        this.profile = getPlugin().getUtils().getSkillManager().findActiveSkills(playerUUID).getProfile();
        this.player = player;
    }
    public SkillUpdateEvent(Player player, String playerUUID, Utils.Skills skill, String profile) {
        this.playerUUID = playerUUID;
        this.skill = skill;
        this.profile = profile;
        this.player = player;
    }

    public Utils.Skills getSkill() {
        return skill;
    }

    public String getProfile() {
        return profile;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
