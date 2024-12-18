package org.skysongdev.skysongstats.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ProfileUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerUUID;
    private final String profile;

    public ProfileUpdateEvent(String playerUUID) {
        this.playerUUID = playerUUID;
        this.profile = getPlugin().getUtils().getProfileManager().findActiveStats(playerUUID).getProfile();
    }

    public ProfileUpdateEvent(String playerUUID, String profile) {
        this.playerUUID = playerUUID;
        this.profile = profile;
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

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
