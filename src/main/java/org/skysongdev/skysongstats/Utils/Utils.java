package org.skysongdev.skysongstats.Utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.skysongdev.skysongstats.SkysongStats;

import java.sql.SQLException;

public class Utils {
    private final SkysongStats plugin;
    private static final MiniMessage mm = MiniMessage.miniMessage();
    public static final String PLUGIN_TAG = "<dark_gray>[<gold>SkysongStats<dark_gray>] ";
    public static NamespacedKey selectableKey = new NamespacedKey(SkysongStats.getPlugin(), "selectable");
    public static NamespacedKey statlineKey = new NamespacedKey(SkysongStats.getPlugin(), "id");
    public static NamespacedKey statButtonKey = new NamespacedKey(SkysongStats.getPlugin(), "stat");
    public static NamespacedKey optionButtonKey = new NamespacedKey(SkysongStats.getPlugin(), "option");
    public static NamespacedKey skillButtonKey = new NamespacedKey(SkysongStats.getPlugin(), "skill");


    public SkillManager getSkillManager() {
        return skillManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    private final SkillManager skillManager = new SkillManager();
    private final StatsManager statsManager = new StatsManager();
    private final ProfileManager profileManager = new ProfileManager();

    public Utils(SkysongStats plugin){
        this.plugin = plugin;
    }
    public static MiniMessage getMiniMessage(){ return mm; }

    public enum StaticStats{
        STRENGTH,
        DEXTERITY,
        CONSTITUTION,
        FOCUS,
        SPEED,
        MAXHP,
        FICTIS_MAX,
        ARCROT_MAX,
        POTION_MAX;

        public static String getStatName(StaticStats stat){
            return switch(stat){
                case STRENGTH -> "Strength";
                case DEXTERITY -> "Dexterity";
                case CONSTITUTION -> "Constitution";
                case FOCUS -> "Focus";
                case SPEED -> "Speed";
                case MAXHP -> "Max HP";
                case FICTIS_MAX -> "Fictis Max";
                case ARCROT_MAX -> "Arcrot Max";
                case POTION_MAX -> "Potion Max";
            };
        }
    }
    public enum Skills{
        ANIMAL_HANDLING(0),
        FARMING(1),
        FORESTRY(2),
        MINING(3),
        MIST_GATHERING(4),
        WOOD_PROCESSING(5),
        ALCHEMY(6),
        ARTIFICING(7),
        COOKING(8),
        CRAFTSMAN(9),
        METALWORKING(10),
        TAILORING(11),
        WITCHCRAFT(12),
        ECONOMICAL(13),
        SCHOLARLY(14),
        ENTERTAINMENT(15),
        MARTIAL(16),
        MEDICAL(17),
        TRANSPORTATION_AND_EXPLORATION(18);

        private final int value;
        private Skills(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }
        public static Skills fromInt(int id){
            return switch(id){
                case 0 -> ANIMAL_HANDLING;
                case 1 -> FARMING;
                case 2 -> FORESTRY;
                case 3 -> MINING;
                case 4 -> MIST_GATHERING;
                case 5 -> WOOD_PROCESSING;
                case 6 -> ALCHEMY;
                case 7 -> ARTIFICING;
                case 8 -> COOKING;
                case 9 -> CRAFTSMAN;
                case 10 -> METALWORKING;
                case 11 -> TAILORING;
                case 12 -> WITCHCRAFT;
                case 13 -> ECONOMICAL;
                case 14 -> SCHOLARLY;
                case 15 -> ENTERTAINMENT;
                case 16 -> MARTIAL;
                case 17 -> MEDICAL;
                case 18 -> TRANSPORTATION_AND_EXPLORATION;
                default -> null;
            };
        }
        public static String getSkillName(Skills skill){
            return switch(skill){
                case ANIMAL_HANDLING -> "Animal Handling";
                case FARMING -> "Farming";
                case FORESTRY -> "Forestry";
                case MINING -> "Mining";
                case MIST_GATHERING -> "Mist Gathering";
                case WOOD_PROCESSING -> "Wood Processing";
                case ALCHEMY -> "Alchemy";
                case ARTIFICING -> "Artificing";
                case COOKING -> "Cooking";
                case CRAFTSMAN -> "Craftsman";
                case METALWORKING -> "Metalworking";
                case TAILORING -> "Tailoring";
                case WITCHCRAFT -> "Witchcraft";
                case ECONOMICAL -> "Economical";
                case SCHOLARLY -> "Scholarly";
                case ENTERTAINMENT -> "Entertainment";
                case MARTIAL -> "Martial";
                case MEDICAL -> "Medical";
                case TRANSPORTATION_AND_EXPLORATION -> "T&E";
            };
        }
        public static String getSkillNameFormatted(Skills skill){
            return switch(skill){
                case ANIMAL_HANDLING -> "<#45c2ea>Animal Handling<reset>";
                case FARMING -> "<#f9f36d>Farming<reset>";
                case FORESTRY -> "<#54bf3e>Forestry<reset>";
                case MINING -> "<#a0836f>Mining<reset>";
                case MIST_GATHERING -> "<gold>Mist Gathering<reset>";
                case WOOD_PROCESSING -> "<#ba7f3d>Wood Processing<reset>";
                case ALCHEMY -> "<#aaeb38>Alchemy<reset>";
                case ARTIFICING -> "<#2cf0f1>Artificing<reset>";
                case COOKING -> "<#f77b3c>Cooking<reset>";
                case CRAFTSMAN -> "<#eba751>Craftsman<reset>";
                case METALWORKING -> "<#8d8d8d>Metalworking<reset>";
                case TAILORING -> "<#ea5be7>Tailoring<reset>";
                case WITCHCRAFT -> "<#8235b0>Witchcraft<reset>";
                case ECONOMICAL -> "<gold>Economical<reset>";
                case SCHOLARLY -> "<#5375e9>Scholarly<reset>";
                case ENTERTAINMENT -> "<#f22a93>Entertainment<reset>";
                case MARTIAL -> "<#e18048>Martial<reset>";
                case MEDICAL -> "<#f14f4f>Medical<reset>";
                case TRANSPORTATION_AND_EXPLORATION -> "<#7fe95b>T&E<reset>";
            };
        }
        public static String getSkillid(Skills skill){
            return switch(skill){
                case ANIMAL_HANDLING -> "animalhandling";
                case FARMING -> "farming";
                case FORESTRY -> "forestry";
                case MINING -> "mining";
                case MIST_GATHERING -> "mistgathering";
                case WOOD_PROCESSING -> "woodprocessing";
                case ALCHEMY -> "alchemy";
                case ARTIFICING -> "artificing";
                case COOKING -> "cooking";
                case CRAFTSMAN -> "craftsman";
                case METALWORKING -> "metalworking";
                case TAILORING -> "tailoring";
                case WITCHCRAFT -> "witchcraft";
                case ECONOMICAL -> "economical";
                case SCHOLARLY -> "scholarly";
                case ENTERTAINMENT -> "entertainment";
                case MARTIAL -> "martial";
                case MEDICAL -> "medical";
                case TRANSPORTATION_AND_EXPLORATION -> "tande";
            };
        }
    }

    public static int toInt(StaticStats stat){
        return switch (stat) {
            case StaticStats.STRENGTH -> 0;
            case StaticStats.DEXTERITY -> 1;
            case StaticStats.CONSTITUTION -> 2;
            case StaticStats.FOCUS -> 3;
            case StaticStats.SPEED -> 4;
            case StaticStats.MAXHP -> 5;
            case StaticStats.FICTIS_MAX -> 6;
            case StaticStats.ARCROT_MAX -> 7;
            case StaticStats.POTION_MAX -> 8;
        };
    }

    public static StaticStats toStatEnum(int id){
        return switch(id){
            case 0 -> StaticStats.STRENGTH;
            case 1 -> StaticStats.DEXTERITY;
            case 2 -> StaticStats.CONSTITUTION;
            case 3 -> StaticStats.FOCUS;
            case 4 -> StaticStats.SPEED;
            case 5 -> StaticStats.MAXHP;
            case 6 -> StaticStats.FICTIS_MAX;
            case 7 -> StaticStats.ARCROT_MAX;
            case 8 -> StaticStats.POTION_MAX;
            default -> null;
        };
    }

    public static boolean isAStat(String input){
        switch(input.toLowerCase()){
            case "strength":
            case "str":
            case "dexterity":
            case "dex":
            case "constitution":
            case "con":
            case "speed":
            case "spd":
            case "focus":
            case "foc":
            case "hp":
            case "temphp":
            case "maxhp":
            case "arcrot":
            case "fictis":
            case "potion":
            case "ac":
                return true;
            default:
                return false;
        }
    }

    public static boolean isASkill(String input){
        switch(input.toLowerCase()){
            case "animalhandling":
            case "ah":
            case "farming":
            case "farm":
            case "forestry":
            case "for":
            case "mining":
            case "min":
            case "mistgathering":
            case "mist":
            case "woodprocessing":
            case "wood":
            case "alchemy":
            case "alch":
            case "artificing":
            case "arti":
            case "cooking":
            case "cook":
            case "craftsman":
            case "craft":
            case "metalworking":
            case "metal":
            case "witchcraft":
            case "witch":
            case "economical":
            case "eco":
            case "scholarly":
            case "scholar":
            case "entertainment":
            case "ent":
            case "martial":
            case "medical":
            case "med":
            case "medic":
            case "transportationandexploration":
            case "tande":
            case "t&e":
                return true;
            default:
                return false;
        }
    }

    public boolean isPlayerInDatabase(String uuid) throws SQLException{
        return plugin.getDatabase().checkForUUID(uuid);
    }


}
