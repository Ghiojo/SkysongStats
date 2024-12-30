package org.skysongdev.skysongstats.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.database.PlayerSkills;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SkillManager {
    public static ArrayList<PlayerSkills> playerSkills;
    public ArrayList<String> skillList = new ArrayList<String>();

    public SkillManager(){
        skillList.add("AnimalHandling");
        skillList.add("Farming");
        skillList.add("Forestry");
        skillList.add("Mining");
        skillList.add("MistGathering");
        skillList.add("WoodProcessing");
        skillList.add("Alchemy");
        skillList.add("Artificing");
        skillList.add("Cooking");
        skillList.add("Craftsman");
        skillList.add("Metalworking");
        skillList.add("Tailoring");
        skillList.add("Witchcraft");
        skillList.add("Economical");
        skillList.add("Scholarly");
        skillList.add("Entertainment");
        skillList.add("Martial");
        skillList.add("Medical");
        skillList.add("T&E");
    }

    public ArrayList<String> getSkillList(){
        return skillList;
    }

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
        try {
            getPlugin().getDatabase().createSkillsData(skill);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add a skill profile!");
            e.printStackTrace();
        }
    }

    public Utils.Skills getSkill(String skill){
        switch(skill.toLowerCase()){
            case "animalhandling":
            case "ah":
                return Utils.Skills.ANIMAL_HANDLING;
            case "farming":
            case "farm":
                return Utils.Skills.FARMING;
            case "forestry":
            case "forest":
                return Utils.Skills.FORESTRY;
            case "mining":
            case "min":
                return Utils.Skills.MINING;
            case "mistgathering":
            case "mist":
                return Utils.Skills.MIST_GATHERING;
            case "woodprocessing":
            case "wood":
                return Utils.Skills.WOOD_PROCESSING;
            case "alchemy":
            case "alch":
                return Utils.Skills.ALCHEMY;
            case "artificing":
            case "arti":
                return Utils.Skills.ARTIFICING;
            case "cooking":
            case "cook":
                return Utils.Skills.COOKING;
            case "craftsman":
            case "craft":
                return Utils.Skills.CRAFTSMAN;
            case "metalworking":
            case "metal":
                return Utils.Skills.METALWORKING;
            case "tailoring":
            case "tailor":
                return Utils.Skills.TAILORING;
            case "witchcraft":
            case "witch":
                return Utils.Skills.WITCHCRAFT;
            case "economical":
            case "eco":
                return Utils.Skills.ECONOMICAL;
            case "scholarly":
            case "scholar":
                return Utils.Skills.SCHOLARLY;
            case "entertainment":
            case "ent":
                return Utils.Skills.ENTERTAINMENT;
            case "martial":
                return Utils.Skills.MARTIAL;
            case "medical":
            case "med":
            case "medic":
                return Utils.Skills.MEDICAL;
            case "transportationandexploration":
            case "tande":
            case "t&e":
                return Utils.Skills.TRANSPORTATION_AND_EXPLORATION;
            default:
                return null;
        }
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
            case "tailoring":
            case "tailor":
                skills.addSkill(Utils.Skills.TAILORING, xp);
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
        try{
            getPlugin().getDatabase().updateSkillsData(skills);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Couldn't update skill data!");
            e.printStackTrace();
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
            case "tailoring":
            case "tailor":
                skills.setSkill(Utils.Skills.TAILORING, xp);
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
        try{
            getPlugin().getDatabase().updateSkillsData(skills);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Couldn't update skill data!");
            e.printStackTrace();
        }
    }

    public PlayerSkills findActiveSkills(String uuid){
        String profile = getPlugin().getUtils().getProfileManager().getActiveProfileName(uuid);
        return findSkills(uuid, profile);
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

    public String getSkillLevel(int xp){
        if(xp < 16){
            return "Unskilled";
        }else if(xp < 40){
            return "Novice";
        }else if(xp < 80){
            return "Competent";
        }else if(xp < 128){
            return "Journeyman";
        }else{
            return "Expert";
        }
    }
    public String getSkillLevelFormatted(int xp){
        if(xp < 16){
            return "<dark_gray>Unskilled";
        }else if(xp < 40){
            return "<#6cda33>Novice";
        }else if(xp < 80){
            return "<blue>Competent";
        }else if(xp < 128){
            return "<#cc6ce9>Journeyman";
        }else{
            return "<gold>Expert";
        }
    }
}
