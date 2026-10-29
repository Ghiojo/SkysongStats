package org.skysongdev.skysongstats.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterProfile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

// %skysongstats_<name>%. Stat placeholders come from stats.yml (see
// StatPlaceholders): <stat>, <stat>_base, <stat>_modifiers, max_<bar>... Then the
// skill levels (<skill id>), the character sheet fields (character_<field>) and
// downtime (global_downtime, character_downtime).
public class StatsExpansion extends PlaceholderExpansion {

    private final SkysongStats plugin;
    private final StatPlaceholders stats;
    private final Map<String, Function<Player, String>> others = new HashMap<>();

    public StatsExpansion(SkysongStats plugin) {
        this.plugin = plugin;
        this.stats = new StatPlaceholders(plugin.getStatService());
        for (Utils.Skills skill : Utils.Skills.values()) {
            others.put(Utils.Skills.getSkillid(skill), player -> Integer.toString(
                    plugin.getUtils().getSkillManager().findActiveSkills(player.getUniqueId().toString()).getSkill(skill)));
        }
        character("name", CharacterProfile::getName);
        character("age", CharacterProfile::getAge);
        character("gender", CharacterProfile::getGender);
        character("pronouns", CharacterProfile::getPronouns);
        character("ancestry", CharacterProfile::getAncestry);
        character("description", CharacterProfile::getDescription);
        others.put("global_downtime", player -> Integer.toString(plugin.getDowntimeService().getGlobal(player.getUniqueId())));
        others.put("character_downtime", this::characterDowntime);
    }

    private void character(String field, Function<CharacterProfile, String> value) {
        others.put("character_" + field, player -> value.apply(
                plugin.getUtils().getCharacterManager().findActiveCharacter(player.getUniqueId().toString())));
    }

    private String characterDowntime(Player player) {
        String profileName = plugin.getUtils().getProfileManager().getActiveProfileName(player.getUniqueId().toString());
        if (profileName == null) {
            return "0";
        }
        return Integer.toString(plugin.getDowntimeService().getCharacter(player.getUniqueId(), profileName));
    }

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

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        return plugin.getUtils().getProfileManager().activeCharacter(player)
                .flatMap(character -> stats.resolve(character, params))
                .orElseGet(() -> otherPlaceholder(player, params));
    }

    // null for names this plugin doesn't know.
    private String otherPlaceholder(Player player, String params) {
        Function<Player, String> other = others.get(params.toLowerCase(Locale.ROOT));
        if (other == null) {
            return null;
        }
        // Character fields are player-written text that may hold placeholders, as before.
        return PlaceholderAPI.setPlaceholders(player, other.apply(player));
    }
}
