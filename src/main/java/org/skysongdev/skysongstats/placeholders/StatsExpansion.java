package org.skysongdev.skysongstats.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;

public class StatsExpansion extends PlaceholderExpansion {

    private final SkysongStats plugin;

    public StatsExpansion(SkysongStats plugin) { this.plugin = plugin; }
    @Override
    public String getAuthor() {
        return "Ghiojo"; //
    }

    @Override
    public String getIdentifier() {
        return "skysongstats"; //
    }

    @Override
    public String getVersion() {
        return "1.0.0"; //
    }

    public String onPlaceholderRequest(Player player,String params){
        PlayerStats stats = plugin.getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString());
        PlayerSkills skills = plugin.getUtils().getSkillManager().findActiveSkills(player.getUniqueId().toString());
        String returnString;

        switch(params.toLowerCase()){
            case "strength":
                returnString = Integer.toString(stats.getStrength());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "dexterity":
                returnString = Integer.toString(stats.getDexterity());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "constitution":
                returnString = Integer.toString(stats.getConstitution());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "focus":
                returnString = Integer.toString(stats.getFocus());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "speed":
                returnString = Integer.toString(stats.getSpeed());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "potion":
                returnString = Integer.toString(stats.getPotion());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "potion_max":
                returnString = Integer.toString(stats.getPotion_max());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "arcrot":
                returnString = Integer.toString(stats.getArcrot());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "arcrot_max":
                returnString = Integer.toString(stats.getArcrot_max());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "fictis":
                returnString = Integer.toString(stats.getFictis());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "fictis_max":
                returnString = Integer.toString(stats.getFictis_max());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "hp":
                returnString = Integer.toString(stats.getHp());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "hp_max":
                returnString = Integer.toString(stats.getHp_max());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "temp_hp":
                returnString = Integer.toString(stats.getTemp_hp());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "ac":
                returnString = Integer.toString(stats.getAc());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            //Skills
            case "animalhandling":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.ANIMAL_HANDLING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "farming":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.FARMING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "forestry":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.FORESTRY));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "mining":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.MINING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "mistgathering":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.MIST_GATHERING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "woodprocessing":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.WOOD_PROCESSING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "alchemy":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.ALCHEMY));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "artificing":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.ARTIFICING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "cooking":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.COOKING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "craftsman":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.CRAFTSMAN));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "metalworking":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.METALWORKING));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "witchcraft":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.WITCHCRAFT));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "economical":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.ECONOMICAL));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "scholarly":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.SCHOLARLY));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "entertainment":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.ENTERTAINMENT));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "martial":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.MARTIAL));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "medical":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.MEDICAL));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "tande":
                returnString = Integer.toString(skills.getSkill(Utils.Skills.TRANSPORTATION_AND_EXPLORATION));
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            //Modifiers
            case "strength_modifiers":
                returnString = Integer.toString(stats.getStrengthModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "dexterity_modifiers":
                returnString = Integer.toString(stats.getDexterityModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "constitution_modifiers":
                returnString = Integer.toString(stats.getConstitutionModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "focus_modifiers":
                returnString = Integer.toString(stats.getFocusModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "speed_modifiers":
                returnString = Integer.toString(stats.getSpeedModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxhp_modifiers":
                returnString = Integer.toString(stats.getMaxHPModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxpotion_modifiers":
                returnString = Integer.toString(stats.getMaxPotionModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxarcrot_modifiers":
                returnString = Integer.toString(stats.getMaxArcrotModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxfictis_modifiers":
                returnString = Integer.toString(stats.getMaxFictisModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            //Full Modifiers
            case "str_full":
                returnString = Integer.toString(stats.getStrength() + stats.getStrengthModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "dex_full":
                returnString = Integer.toString(stats.getDexterity() + stats.getDexterityModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "con_full":
                returnString = Integer.toString(stats.getConstitution() + stats.getConstitutionModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "foc_full":
                returnString = Integer.toString(stats.getFocus() + stats.getFocusModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "spd_full":
                returnString = Integer.toString(stats.getSpeed() + stats.getSpeedModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxhp_full":
                returnString = Integer.toString(stats.getHp_max() + stats.getMaxHPModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxpotion_full":
                returnString = Integer.toString(stats.getPotion_max() + stats.getMaxPotionModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxarcrot_full":
                returnString = Integer.toString(stats.getArcrot_max() + stats.getMaxArcrotModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
            case "maxfictis_full":
                returnString = Integer.toString(stats.getFictis_max() + stats.getMaxFictisModifiers());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;


        }

        return null;
    }
}
