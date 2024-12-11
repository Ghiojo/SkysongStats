package org.skysongdev.skysongstats.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.SkysongStats;
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
            case "ac":
                returnString = Integer.toString(stats.getAc());
                returnString = PlaceholderAPI.setPlaceholders(player, returnString);
                return returnString;
        }

        return null;
    }
}
