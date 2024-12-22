package org.skysongdev.skysongstats.Utils;

import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.ProfileIndex;

import java.util.ArrayList;
import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class CharacterManager {
    public static ArrayList<CharacterProfile> characterProfiles;

    public CharacterProfile findCharacterProfile(String uuid, String profile){
        for(CharacterProfile current : CharacterManager.characterProfiles){
            if(current.getUuid().equals(uuid) && current.getProfile().equals(profile)){
                return current;
            }
        }
        return null;
    }

    public CharacterProfile findActiveCharacter(String uuid){
        for(ProfileIndex current : ProfileManager.activeProfiles){
            if(Objects.equals(current.getUuid(), uuid)){
                return findCharacterProfile(current.getUuid(), current.getProfile());
            }
        }
        return null;
    }

    public void addCharacter(CharacterProfile profile){
        try {
            getPlugin().getDatabase().createCharacterData(profile);
            CharacterManager.characterProfiles.add(profile);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateCharacter(CharacterProfile profile) {
        try {
            getPlugin().getDatabase().updateCharacterData(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteCharacterProfile(CharacterProfile profile) {
        try {
            getPlugin().getDatabase().deleteCharacterData(profile);
            CharacterManager.characterProfiles.remove(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
