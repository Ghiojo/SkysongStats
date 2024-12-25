package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewSkills implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        Player target;

        if(strings.length > 0){
            target = Bukkit.getServer().getPlayer(strings[0]);
            if(target == null){
                player.sendMessage(getPlugin().getUtils().getMiniMessage().deserialize("<red>That player is not online!"));
                return true;
            }
        } else {
            target = player;
        }
        ItemStack gui = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) gui.getItemMeta();
        PlayerSkills skills = getPlugin().getUtils().getSkillManager().findActiveSkills(target.getUniqueId().toString());
        ArrayList<Component> pages = new ArrayList<Component>();

        int skillIndex = 0;
        int sectionIndex = 0;
        String[] sections = {"<green>Gathering Skills:\n\n", "<blue>Crafting Skills:\n\n", "<gold>Service Skills:\n\n"};
        String pageoutput = "";
        Utils.Skills currentSkill;

        for(skillIndex = 0; skillIndex < 6; skillIndex++) {
            currentSkill = Utils.Skills.fromInt(skillIndex);
            pageoutput += Utils.Skills.getSkillNameFormatted(currentSkill) + ": " + getPlugin().getUtils().getSkillManager().getSkillLevelFormatted(skills.getSkill(currentSkill)) + "(" + skills.getSkill(currentSkill) + ")\n";
        }
        pages.add(getPlugin().getUtils().getMiniMessage().deserialize(sections[sectionIndex] + pageoutput));
        pageoutput = "";
        for(sectionIndex++; skillIndex < 13; skillIndex++) {
            currentSkill = Utils.Skills.fromInt(skillIndex);
            pageoutput += Utils.Skills.getSkillNameFormatted(currentSkill) + ": " + getPlugin().getUtils().getSkillManager().getSkillLevelFormatted(skills.getSkill(currentSkill)) + "(" + skills.getSkill(currentSkill) + ")\n";
        }
        pages.add(getPlugin().getUtils().getMiniMessage().deserialize(sections[sectionIndex] + pageoutput));
        pageoutput = "";
        for(sectionIndex++; skillIndex < 19; skillIndex++) {
            currentSkill = Utils.Skills.fromInt(skillIndex);
            pageoutput += Utils.Skills.getSkillNameFormatted(currentSkill) + ": " + getPlugin().getUtils().getSkillManager().getSkillLevelFormatted(skills.getSkill(currentSkill)) + "(" + skills.getSkill(currentSkill) + ")\n";
        }
        pages.add(getPlugin().getUtils().getMiniMessage().deserialize(sections[sectionIndex] + pageoutput));
        pageoutput = "";

        for(Component page : pages) {
            meta.addPages(page);
        }

        meta.setTitle("Skills");
        meta.setAuthor("Skysong");
        gui.setItemMeta(meta);
        player.openBook(gui);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            List<String> players = new ArrayList<String>();
            for(Player player : Bukkit.getOnlinePlayers()){
                players.add(player.getName());
            }
            return players;
        }
        return null;
    }
}
