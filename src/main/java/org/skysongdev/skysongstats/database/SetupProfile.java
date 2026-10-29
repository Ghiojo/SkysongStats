package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.inventories.SkillChoiceGUI;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupProfile {
    private String uuid;
    private String profile;
    private boolean isSetUp;

    private SkillChoiceGUI skillChoiceGUI = new SkillChoiceGUI(this);

    // The stat values picked on the /setup point screen, by stat id.
    private Map<String, Integer> chosenStats = Map.of();

    public ArrayList<Integer> skillsSelected = new ArrayList<Integer>();
    public int skillPoints = 2;

    public Map<String, Integer> getChosenStats() {
        return chosenStats;
    }

    public void setChosenStats(Map<String, Integer> chosenStats) {
        this.chosenStats = Map.copyOf(chosenStats);
    }

    public ArrayList<Integer> getSkillsSelected() {
        return skillsSelected;
    }

    public void setSkillsSelected(ArrayList<Integer> skillsSelected) {
        this.skillsSelected = skillsSelected;
    }

    public String getUuid() {
        return uuid;
    }

    public String getProfile() {
        return profile;
    }

    public boolean isSetUp() {
        return isSetUp;
    }
    public void setSetUp(boolean setUp) {
        isSetUp = setUp;
    }
    public SetupProfile(String uuid){
        this.uuid = uuid;
        this.profile = "Default";
        this.isSetUp = false;
    }

    public SetupProfile(String uuid, String profile, boolean isSetUp) {
        this.uuid = uuid;
        this.profile = profile;
        this.isSetUp = isSetUp;
    }

    public void finishProfileSetup(){
        applyChosenStats(getPlugin().getStatService());

        PlayerSkills playerSkills = getPlugin().getUtils().getSkillManager().findSkills(uuid, profile);
        playerSkills.setSkill(Utils.Skills.fromInt(skillsSelected.get(0)), 16);
        playerSkills.setSkill(Utils.Skills.fromInt(skillsSelected.get(1)), 16);
        getPlugin().getUtils().getSkillManager().updateSkillProfile(playerSkills);

        this.isSetUp = true;
        try{
        getPlugin().getDatabase().updateSetupData(uuid, profile, true);
        } catch (Exception e){
            Bukkit.getLogger().warning("[Skysong Stats] Failed to update player setup data!");
        }
    }

    // Sets the character's stats to the values picked on the point screen. A stat
    // that left stats.yml since then is skipped.
    public void applyChosenStats(StatService stats){
        ProfileKey character = new ProfileKey(UUID.fromString(uuid), profile);
        chosenStats.forEach((id, value) -> stats.definitions().get(id)
                .ifPresent(stat -> stats.apply(character, stat, StatAction.SET, value)));
    }

    public SkillChoiceGUI getSkillChoiceGUI() {
        return skillChoiceGUI;
    }

    public void setupSkillChoiceGui(){
        skillChoiceGUI.setupInventory(Bukkit.getPlayer(UUID.fromString(uuid)));
    }
}
