package org.skysongdev.skysongstats.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.events.SkillUpdateEvent;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SkillLevelListener implements Listener {
    private final LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
    @EventHandler
    public void onSkillLevelChange(SkillUpdateEvent event) {
        Player player = event.getPlayer();
        Utils.Skills skill = event.getSkill();
        String profile = event.getProfile();

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if(user == null){
            return;
        }

        String level = getPlugin().getUtils().getSkillManager().getSkillLevel(getPlugin().getUtils().getSkillManager().findSkills(player.getUniqueId().toString(), profile).getSkill(skill));
        switch (level){
            case "Expert":
                user.data().add(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".expert").build());
            case "Journeyman":
                user.data().add(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".journeyman").build());
            case "Competent":
                user.data().add(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".competent").build());
            case "Novice":
                user.data().add(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".novice").build());
            default:
                break;
        }
        switch (level){
            case "Unskilled":
                user.data().remove(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".novice").build());
            case "Novice":
                user.data().remove(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".competent").build());
            case "Competent":
                user.data().remove(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".journeyman").build());
            case "Journeyman":
                user.data().remove(Node.builder("skysongstats.skill." + Utils.Skills.getSkillid(skill) + ".expert").build());
            default:
                break;
        }

        luckPerms.getUserManager().saveUser(user);
    }
}
