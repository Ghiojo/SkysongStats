package org.skysongdev.skysongstats.commands.setup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.setup.MenuTexts;
import org.skysongdev.skysongstats.setup.StatSetupMenu;

import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupCommand implements CommandExecutor {
    private final MenuTexts texts;

    public SetupCommand(MenuTexts texts) {
        this.texts = Objects.requireNonNull(texts, "texts");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player target)){
            getPlugin().getMessages().send(commandSender, "general.players-only");
            return true;
        }
        SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString());
        if(setup.isSetUp()){
            getPlugin().getMessages().send(target, "setup.already-set-up");
            return true;
        }
        getPlugin().getMessages().send(target, "setup.started");
        // The stat point screen comes from stats.yml; confirming it moves on to
        // the skill picker.
        StatsConfig config = getPlugin().getStatsConfig();
        new StatSetupMenu(config.stats().setupStats(), config.setupRules(), texts, (player, values) -> {
            setup.setChosenStats(values);
            setup.setupSkillChoiceGui();
            player.openInventory(setup.getSkillChoiceGUI().getInventory());
        }).open(target);
        return true;
    }
}
