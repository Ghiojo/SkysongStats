package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ProfileUpdateListener implements Listener {
    @EventHandler
    public void onProfileUpdate(ProfileUpdateEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        Utils.Skills skill = null;
        String profile = event.getProfile();
        String level = null;

        for(int i = 0; i < 19; i++) {
            skill = Utils.Skills.fromInt(i);
            level = getPlugin().getUtils().getSkillManager().getSkillLevel(getPlugin().getUtils().getSkillManager().findSkills(player.getUniqueId().toString(), profile).getSkill(skill));

            switch (level) {
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
            switch (level) {
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
}
