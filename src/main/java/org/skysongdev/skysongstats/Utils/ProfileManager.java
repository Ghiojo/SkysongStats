package org.skysongdev.skysongstats.Utils;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.SkysongStats;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ProfileManager {
    public static ArrayList<ProfileIndex> activeProfiles;
    public static ArrayList<SetupProfile> setupProfiles;

    // The player's active profile as a stat character, or empty when they have
    // none (or the profiles could not be loaded).
    public Optional<ProfileKey> activeCharacter(Player player){
        if(ProfileManager.activeProfiles == null){
            return Optional.empty();
        }
        return Optional.ofNullable(getActiveProfileName(player.getUniqueId().toString()))
                .map(profile -> new ProfileKey(player.getUniqueId(), profile));
    }

    // Every profile of the player: each profile has its stats.
    public ArrayList<String> findAllProfiles(String uuid){
        ArrayList<String> profiles = new ArrayList<>();
        for(ProfileKey character : getPlugin().getStatService().characters()){
            if(Objects.equals(character.player().toString(), uuid)){
                profiles.add(character.profile());
            }
        }
        profiles.sort(String.CASE_INSENSITIVE_ORDER);
        return profiles;
    }

    // Whether the player has a profile with this name.
    public boolean hasProfile(UUID player, String profile){
        return getPlugin().getStatService().sheet(new ProfileKey(player, profile)).isPresent();
    }

    public void setProfile(String uuid, String profile){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                current.setProfile(profile);
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

    public void createProfile(String uuid, String profile){
        ProfileIndex newindex = new ProfileIndex(uuid, profile);
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
