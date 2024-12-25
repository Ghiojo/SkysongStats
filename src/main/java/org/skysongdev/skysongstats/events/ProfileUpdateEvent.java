package org.skysongdev.skysongstats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ProfileUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerUUID;
    private final String profile;
    private final Player player;

    public ProfileUpdateEvent(String playerUUID) {
        this.playerUUID = playerUUID;
        this.profile = getPlugin().getUtils().getProfileManager().findActiveStats(playerUUID).getProfile();
        this.player = null;
    }

    public ProfileUpdateEvent(Player player, String playerUUID, String profile) {
        this.playerUUID = playerUUID;
        this.profile = profile;
        this.player = player;
    }

    public String getProfile() {
        return profile;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public Player getPlayer() {
        return player;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
