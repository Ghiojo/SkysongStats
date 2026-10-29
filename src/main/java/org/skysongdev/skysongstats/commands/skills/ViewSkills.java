package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.skysongdev.skysongstats.messages.Messages;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewSkills implements TabExecutor {
    // The book's pages: the messages.yml key of each page and the skills it lists
    // (Utils.Skills numbers, first included, last excluded).
    private static final String[] PAGE_KEYS = {"skills.view.gathering-page", "skills.view.crafting-page", "skills.view.service-page"};
    private static final int[] PAGE_STARTS = {0, 6, 13, 18};

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        Player target;

        if(strings.length > 0){
            target = Bukkit.getServer().getPlayer(strings[0]);
            if(target == null){
                getPlugin().getMessages().send(player, "skills.view.player-not-online");
                return true;
            }
        } else {
            target = player;
        }
        ItemStack gui = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) gui.getItemMeta();
        PlayerSkills skills = getPlugin().getUtils().getSkillManager().findActiveSkills(target.getUniqueId().toString());
        pages(skills).forEach(meta::addPages);

        meta.setTitle(plainText("skills.view.book-title"));
        meta.setAuthor(plainText("skills.view.book-author"));
        gui.setItemMeta(meta);
        player.openBook(gui);
        return true;
    }

    /** The book's pages for these skills. */
    static List<Component> pages(PlayerSkills skills) {
        List<Component> pages = new ArrayList<>();
        for(int page = 0; page < PAGE_KEYS.length; page++) {
            pages.add(page(skills, PAGE_KEYS[page], PAGE_STARTS[page], PAGE_STARTS[page + 1]));
        }
        return pages;
    }

    private static Component page(PlayerSkills skills, String key, int firstSkill, int endSkill) {
        Messages messages = getPlugin().getMessages();
        List<Component> lines = new ArrayList<>();
        for(int skillIndex = firstSkill; skillIndex < endSkill; skillIndex++) {
            Utils.Skills skill = Utils.Skills.fromInt(skillIndex);
            int xp = skills.getSkill(skill);
            lines.add(messages.get("skills.view.skill-line", Map.of("xp", String.valueOf(xp)), Map.of(
                    "skill", Utils.getMiniMessage().deserialize(Utils.Skills.getSkillNameFormatted(skill)),
                    "level", Utils.getMiniMessage().deserialize(getPlugin().getUtils().getSkillManager().getSkillLevelFormatted(xp)))));
        }
        Component skillLines = Component.join(JoinConfiguration.newlines(), lines);
        return messages.get(key, Map.of(), Map.of("skills", skillLines));
    }

    private static String plainText(String key) {
        return PlainTextComponentSerializer.plainText().serialize(getPlugin().getMessages().get(key));
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
