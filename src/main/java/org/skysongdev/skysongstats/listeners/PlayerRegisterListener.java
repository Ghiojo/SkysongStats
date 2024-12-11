package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.sql.SQLException;

public class PlayerRegisterListener implements Listener {
    SkysongStats plugin;
    public PlayerRegisterListener(SkysongStats plugin){ this.plugin = plugin; }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        boolean isOldPlayer = false;
        try{
            isOldPlayer = plugin.getUtils().isPlayerInDatabase(event.getPlayer().getUniqueId().toString());
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Some Error happened when looking for the player in the database!");
            e.printStackTrace();
        }
        if(!isOldPlayer){
            PlayerStats stats = new PlayerStats(event.getPlayer().getUniqueId().toString());
            plugin.getUtils().getStatsManager().addStatProfile(stats);
            plugin.getUtils().getProfileManager().createProfile(stats);
            PlayerSkills skills = new PlayerSkills(event.getPlayer().getUniqueId().toString());
            plugin.getUtils().getSkillManager().addSkillProfile(skills);
            plugin.getUtils().getProfileManager().createSetupProfile(event.getPlayer().getUniqueId().toString());
        }
        if(!SkysongStats.getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getPlayer().getUniqueId().toString()).isSetUp()){
            event.getPlayer().sendMessage(SkysongStats.getPlugin().getUtils().getMiniMessage().deserialize(SkysongStats.getPlugin().getUtils().PLUGIN_TAG + "<green>You have yet to set up your profile! Use /setup to get started!"));
        }
    }
}
