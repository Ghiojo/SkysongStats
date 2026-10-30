package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.*;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.sql.SQLException;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class PlayerRegisterListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        boolean isOldPlayer = false;
        try{
            isOldPlayer = getPlugin().getUtils().isPlayerInDatabase(event.getPlayer().getUniqueId().toString());
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Some Error happened when looking for the player in the database!");
            e.printStackTrace();
        }
        if(!isOldPlayer){
            getPlugin().getUtils().getProfileManager().createProfile(event.getPlayer().getUniqueId().toString(), "Default");
            getPlugin().getUtils().getProfileManager().createSetupProfile(event.getPlayer().getUniqueId().toString());
            CharacterProfile character = new CharacterProfile(event.getPlayer().getUniqueId().toString());
            getPlugin().getUtils().getCharacterManager().addCharacter(character);
            DowntimeService downtime = getPlugin().getDowntimeService();
            downtime.getGlobal(event.getPlayer().getUniqueId());
            downtime.getCharacter(event.getPlayer().getUniqueId(), character.getProfile());
            downtime.setPreference(event.getPlayer().getUniqueId(), false);
            getPlugin().getStatService().createCharacter(new ProfileKey(event.getPlayer().getUniqueId(), character.getProfile()));
            getPlugin().getSkillService().createCharacter(new ProfileKey(event.getPlayer().getUniqueId(), character.getProfile()));
        }
        if(!getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getPlayer().getUniqueId().toString()).isSetUp()){
            getPlugin().getMessages().send(event.getPlayer(), "profile.setup-reminder");
        }
    }
}
