package org.skysongdev.skysongstats.Utils;

import org.skysongdev.skysongstats.database.CharacterDowntime;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.PlayerDowntime;

import java.util.ArrayList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DowntimeManager {
    public static ArrayList<CharacterDowntime> characterDowntimes;
    public static ArrayList<PlayerDowntime> playerDowntimes;

    public CharacterDowntime findActiveCharacterDowntime(String uuid){
        for(CharacterDowntime current : DowntimeManager.characterDowntimes){
            if(current.getUuid().equals(uuid) && current.getProfile().equals(getPlugin().getUtils().getProfileManager().findActiveProfile(uuid))){
                return current;
            }
        }
        return null;
    }

    public PlayerDowntime findPlayerDowntime(String uuid){
        for(PlayerDowntime current : DowntimeManager.playerDowntimes){
            if(current.getUuid().equals(uuid)){
                return current;
            }
        }
        return null;
    }

    public CharacterDowntime findCharacterDowntime(String uuid, String profile){
        for(CharacterDowntime current : DowntimeManager.characterDowntimes){
            if(current.getUuid().equals(uuid) && current.getProfile().equals(profile)){
                return current;
            }
        }
        return null;
    }

    public void addCharacterDowntime(CharacterDowntime downtime) {
        try {
            getPlugin().getDatabase().createCharacterDowntimeData(downtime);
            DowntimeManager.characterDowntimes.add(downtime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateCharacterDowntime(CharacterDowntime downtime) {
        try {
            getPlugin().getDatabase().updateCharacterDowntimeData(downtime);
            for(CharacterDowntime current : DowntimeManager.characterDowntimes){
                if(current.getUuid().equals(downtime.getUuid()) && current.getProfile().equals(downtime.getProfile())){
                    current.setCurrentDowntime(downtime.getCurrentDowntime());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteCharacterDowntime(CharacterDowntime downtime) {
        try {
            getPlugin().getDatabase().deleteCharacterDowntimeData(downtime);
            DowntimeManager.characterDowntimes.remove(downtime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPlayerDowntime(PlayerDowntime downtime) {
        try {
            getPlugin().getDatabase().createPlayerDowntimeData(downtime);
            DowntimeManager.playerDowntimes.add(downtime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerDowntime(PlayerDowntime downtime) {
        try {
            getPlugin().getDatabase().updatePlayerDowntimeData(downtime);
            for(PlayerDowntime current : DowntimeManager.playerDowntimes){
                if(current.getUuid().equals(downtime.getUuid())){
                    current.setCurrentDowntime(downtime.getCurrentDowntime());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
