package org.skysongdev.skysongstats.Utils;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ProfileManager {
    public static ArrayList<ProfileIndex> activeProfiles;
    public static ArrayList<SetupProfile> setupProfiles;

    public PlayerStats findActiveStats(String uuid){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                return getPlugin().getUtils().getStatsManager().findStats(current.getUuid(), current.getProfile());
            }
        }
        return null;
    }

    public ArrayList<String> findAllProfiles(String uuid){
        ArrayList<String> profiles = new ArrayList<>();
        for(PlayerStats current : StatsManager.statsProfileList){
            if(Objects.equals(current.getUuid(), uuid)){
                profiles.add(current.getProfile());
            }
        }
        return profiles;
    }

    public void setProfile(PlayerStats stat){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), stat.getUuid())){
                current.setProfile(stat.getProfile());
                try {
                    SkysongStats.getPlugin().getDatabase().updateActiveProfileData(current);
                }catch(SQLException e){
                    Bukkit.getLogger().warning("[SkysongStats] Unable to set new active stat!");
                    e.printStackTrace();
                }
            }
        }
    }

    public String getActiveProfileName(String uuid){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                return current.getProfile();
            }
        }
        return null;
    }

    public ProfileIndex findActiveProfile(String uuid){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                return current;
            }
        }
        return null;
    }

    public void createProfile(PlayerStats stat){
        ProfileIndex newindex = new ProfileIndex(stat.getUuid(), stat.getProfile());
        try {
            getPlugin().getDatabase().createActiveProfileData(newindex);
            ProfileManager.activeProfiles.add(newindex);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add an active stat profile!");
            e.printStackTrace();
        }
    }

    public SetupProfile findActiveSetupProfile(String uuid){
        String profile = getActiveProfileName(uuid);
        for(SetupProfile current : ProfileManager.setupProfiles){
            if(Objects.equals(current.getUuid(), uuid) && Objects.equals(current.getProfile(), profile)){
                return current;
            }
        }
        return null;
    }

    public void addSetupProfile(SetupProfile setupProfile){
        ProfileManager.setupProfiles.add(setupProfile);
        try {
            getPlugin().getDatabase().createSetupData(setupProfile.getUuid(), setupProfile.getProfile());
        }catch(SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add a setup profile!");
            e.printStackTrace();
        }
    }

    public void createSetupProfile(String uuid){
        try {
            SetupProfile setupProfile = new SetupProfile(uuid);
            getPlugin().getDatabase().createSetupData(uuid, "Default");
            ProfileManager.setupProfiles.add(setupProfile);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add a setup profile!");
            e.printStackTrace();
        }
    }
    public SetupProfile findSetupProfile(String uuid, String profile){
        for(SetupProfile current : ProfileManager.setupProfiles){
            if(Objects.equals(current.getUuid(), uuid) && Objects.equals(current.getProfile(), profile)){
                return current;
            }
        }
        return null;
    }
    public void setSetupProfile(String uuid, String profile, boolean isSetUp){
        for(SetupProfile current : ProfileManager.setupProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                current.setSetUp(isSetUp);
                try {
                    getPlugin().getDatabase().updateSetupData(uuid, profile, isSetUp);
                }catch(SQLException e){
                    Bukkit.getLogger().warning("[SkysongStats] Unable to set new setup profile!");
                    e.printStackTrace();
                }
            }
        }
    }
    public void deleteSetupProfile(SetupProfile setupProfile){
        ProfileManager.setupProfiles.remove(setupProfile);
        try {
            getPlugin().getDatabase().deleteSetupData(setupProfile.getUuid(), setupProfile.getProfile());
        }catch(SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to delete setup profile!");
            e.printStackTrace();
        }
    }
}
