package org.skysongdev.skysongstats.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.database.PlayerSkills;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SkillManager {
    public static ArrayList<PlayerSkills> playerSkills;

    public PlayerSkills findSkills(String uuid, String profile){
        for(PlayerSkills current : SkillManager.playerSkills){
            if(current.getUuid().equals(uuid) && current.getProfile().equals(profile)){
                return current;
            }
        }
        return null;
    }
    public void addSkillProfile(PlayerSkills skill){
        SkillManager.playerSkills.add(skill);
    }
    public void addSkillXP(PlayerSkills skills, String skill, int xp){
        switch(skill.toLowerCase()){
            case "animalhandling":
            case "ah":
                skills.addSkill(Utils.Skills.ANIMAL_HANDLING, xp);
                break;
            case "farming":
            case "farm":
                skills.addSkill(Utils.Skills.FARMING, xp);
                break;
            case "forestry":
            case "forest":
                skills.addSkill(Utils.Skills.FORESTRY, xp);
                break;
            case "mining":
            case "min":
                skills.addSkill(Utils.Skills.MINING, xp);
                break;
            case "mistgathering":
            case "mist":
                skills.addSkill(Utils.Skills.MIST_GATHERING, xp);
                break;
            case "woodprocessing":
            case "wood":
                skills.addSkill(Utils.Skills.WOOD_PROCESSING, xp);
                break;
            case "alchemy":
            case "alch":
                skills.addSkill(Utils.Skills.ALCHEMY, xp);
                break;
            case "artificing":
            case "arti":
                skills.addSkill(Utils.Skills.ARTIFICING, xp);
                break;
            case "cooking":
            case "cook":
                skills.addSkill(Utils.Skills.COOKING, xp);
                break;
            case "craftsman":
            case "craft":
                skills.addSkill(Utils.Skills.CRAFTSMAN, xp);
                break;
            case "metalworking":
            case "metal":
                skills.addSkill(Utils.Skills.METALWORKING, xp);
                break;
            case "witchcraft":
            case "witch":
                skills.addSkill(Utils.Skills.WITCHCRAFT, xp);
                break;
            case "economical":
            case "eco":
                skills.addSkill(Utils.Skills.ECONOMICAL, xp);
                break;
            case "scholarly":
            case "scholar":
                skills.addSkill(Utils.Skills.SCHOLARLY, xp);
                break;
            case "entertainment":
            case "ent":
                skills.addSkill(Utils.Skills.ENTERTAINMENT, xp);
                break;
            case "martial":
                skills.addSkill(Utils.Skills.MARTIAL, xp);
                break;
            case "medical":
            case "med":
            case "medic":
                skills.addSkill(Utils.Skills.MEDICAL, xp);
                break;
            case "transportationandexploration":
            case "tande":
            case "t&e":
                skills.addSkill(Utils.Skills.TRANSPORTATION_AND_EXPLORATION, xp);
                break;
            default:
                break;
        }
    }

    public void setSkillXP(PlayerSkills skills, String skill, int xp) {
        switch (skill.toLowerCase()) {
            case "animalhandling":
            case "ah":
                skills.setSkill(Utils.Skills.ANIMAL_HANDLING, xp);
                break;
            case "farming":
            case "farm":
                skills.setSkill(Utils.Skills.FARMING, xp);
                break;
            case "forestry":
            case "forest":
                skills.setSkill(Utils.Skills.FORESTRY, xp);
                break;
            case "mining":
            case "min":
                skills.setSkill(Utils.Skills.MINING, xp);
                break;
            case "mistgathering":
            case "mist":
                skills.setSkill(Utils.Skills.MIST_GATHERING, xp);
                break;
            case "woodprocessing":
            case "wood":
                skills.setSkill(Utils.Skills.WOOD_PROCESSING, xp);
                break;
            case "alchemy":
            case "alch":
                skills.setSkill(Utils.Skills.ALCHEMY, xp);
                break;
            case "artificing":
            case "arti":
                skills.setSkill(Utils.Skills.ARTIFICING, xp);
                break;
            case "cooking":
            case "cook":
                skills.setSkill(Utils.Skills.COOKING, xp);
                break;
            case "craftsman":
            case "craft":
                skills.setSkill(Utils.Skills.CRAFTSMAN, xp);
                break;
            case "metalworking":
            case "metal":
                skills.setSkill(Utils.Skills.METALWORKING, xp);
                break;
            case "witchcraft":
            case "witch":
                skills.setSkill(Utils.Skills.WITCHCRAFT, xp);
                break;
            case "economical":
            case "eco":
                skills.setSkill(Utils.Skills.ECONOMICAL, xp);
                break;
            case "scholarly":
            case "scholar":
                skills.setSkill(Utils.Skills.SCHOLARLY, xp);
                break;
            case "entertainment":
            case "ent":
                skills.setSkill(Utils.Skills.ENTERTAINMENT, xp);
                break;
            case "martial":
                skills.setSkill(Utils.Skills.MARTIAL, xp);
                break;
            case "medical":
            case "med":
            case "medic":
                skills.setSkill(Utils.Skills.MEDICAL, xp);
            case "transportationandexploration":
            case "tande":
            case "t&e":
                skills.setSkill(Utils.Skills.TRANSPORTATION_AND_EXPLORATION, xp);
                break;
            default:
                break;
        }
    }

    public void updateSkillProfile(PlayerSkills skillProfile){
        int index = playerSkills.indexOf(skillProfile);
        if(index != -1)
            playerSkills.set(index, skillProfile);
    }

    public void deleteSkillProfile(PlayerSkills skillProfile){
        try {
            getPlugin().getDatabase().deleteSkillData(skillProfile);
            SkillManager.playerSkills.remove(skillProfile);
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Couldn't delete stat data!");
            e.printStackTrace();
        }
    }
}
