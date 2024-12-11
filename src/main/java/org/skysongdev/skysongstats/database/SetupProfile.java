package org.skysongdev.skysongstats.database;

import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.inventories.PointAllocGUI;
import org.skysongdev.skysongstats.inventories.SkillChoiceGUI;
import org.skysongdev.skysongstats.inventories.StatSetupGUI;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupProfile {
    private String uuid;
    private String profile;
    private boolean isSetUp;

    private StatSetupGUI statSetupGUI = new StatSetupGUI();
    private PointAllocGUI pointAllocGUI = new PointAllocGUI();
    private SkillChoiceGUI skillChoiceGUI = new SkillChoiceGUI();

    private int[] strength = new int[2];
    private int[] dexterity = new int[2];
    private int[] constitution = new int[2];
    private int[] focus = new int[2];
    private int[] speed = new int[2];

    private int[] skillsSelected = new int[2];

    public int[] getStrength() {
        return strength;
    }

    public void setStrength(int[] strength) {
        this.strength = strength;
    }

    public int[] getDexterity() {
        return dexterity;
    }

    public void setDexterity(int[] dexterity) {
        this.dexterity = dexterity;
    }

    public int[] getConstitution() {
        return constitution;
    }

    public void setConstitution(int[] constitution) {
        this.constitution = constitution;
    }

    public int[] getFocus() {
        return focus;
    }

    public void setFocus(int[] focus) {
        this.focus = focus;
    }

    public int[] getSpeed() {
        return speed;
    }

    public void setSpeed(int[] speed) {
        this.speed = speed;
    }

    public int[] getSkillsSelected() {
        return skillsSelected;
    }

    public void setSkillsSelected(int[] skillsSelected) {
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
        PlayerStats playerStats = getPlugin().getUtils().getStatsManager().findStats(uuid, profile);
        playerStats.setStrength(strength[0] + strength[1]);
        playerStats.setDexterity(dexterity[0] + dexterity[1]);
        playerStats.setConstitution(constitution[0] + constitution[1]);
        playerStats.setFocus(focus[0] + focus[1]);
        playerStats.setSpeed(speed[0] + speed[1]);
        getPlugin().getUtils().getStatsManager().updateStats(playerStats);

        PlayerSkills playerSkills = getPlugin().getUtils().getSkillManager().findSkills(uuid, profile);
        playerSkills.setSkill(Utils.Skills.fromInt(skillsSelected[0]), 16);
        playerSkills.setSkill(Utils.Skills.fromInt(skillsSelected[1]), 16);
        getPlugin().getUtils().getSkillManager().updateSkillProfile(playerSkills);
    }
}
