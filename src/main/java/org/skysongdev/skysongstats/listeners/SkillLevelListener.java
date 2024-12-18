package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.events.SkillUpdateEvent;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SkillLevelListener implements Listener {
    @EventHandler
    public void onSkillLevelChange(SkillUpdateEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        Utils.Skills skill = event.getSkill();
        String profile = event.getProfile();

        String level = getPlugin().getUtils().getSkillManager().getSkillLevel(getPlugin().getUtils().getSkillManager().findSkills(player.getUniqueId().toString(), profile).getSkill(skill));
        switch (level){
            case "Expert":
                player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".expert", true);
            case "Journeyman":
                player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".journeyman", true);
            case "Competent":
                player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".competent", true);
            case "Novice":
                player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".novice", true);
            default:
                break;
        }
        switch (level){
            case "Unskilled":
                player.removeAttachment(player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".novice", false));
            case "Novice":
                player.removeAttachment(player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".competent", false));
            case "Competent":
                player.removeAttachment(player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".journeyman", false));
            case "Journeyman":
                player.removeAttachment(player.addAttachment(getPlugin(), "skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".expert", false));
            default:
                break;
        }
    }
}
