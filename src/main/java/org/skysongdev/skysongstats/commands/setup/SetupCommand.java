package org.skysongdev.skysongstats.commands.setup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.setup.MenuTexts;
import org.skysongdev.skysongstats.setup.ProficiencySetupMenu;
import org.skysongdev.skysongstats.setup.SkillSetupMenu;
import org.skysongdev.skysongstats.setup.StatSetupMenu;

import java.util.List;
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
        // the skill screen from skills.yml, then the proficiency screen from
        // proficiencies.yml, and confirming that finishes the setup.
        StatsConfig config = getPlugin().getStatsConfig();
        new StatSetupMenu(config.stats().setupStats(), config.setupRules(), texts, (player, values) -> {
            setup.setChosenStats(values);
            openSkillScreen(player, setup);
        }).open(target);
        return true;
    }

    private void openSkillScreen(Player player, SetupProfile setup) {
        SkillsConfig skills = getPlugin().getSkillService().config();
        new SkillSetupMenu(skills.skills().setupSkills(), skills.setup(), texts,
                (confirmer, picked) -> openProficiencyScreen(confirmer, setup, picked)).open(player);
    }

    // Skipped when nothing is trained and the player has no picks.
    private void openProficiencyScreen(Player player, SetupProfile setup, List<SkillDefinition> pickedSkills) {
        List<String> pickedIds = pickedSkills.stream().map(SkillDefinition::id).toList();
        ProficiencySetupMenu.forPickedSkills(getPlugin().getProficiencyService().config(),
                        getPlugin().getSkillService().config().skills().all(), pickedIds, texts,
                        (confirmer, proficiencies) -> finish(confirmer, setup, pickedSkills, proficiencies))
                .ifPresentOrElse(menu -> menu.open(player), () -> finish(player, setup, pickedSkills, List.of()));
    }

    private void finish(Player player, SetupProfile setup, List<SkillDefinition> pickedSkills,
                        List<ProficiencyDefinition> proficiencies) {
        setup.finishProfileSetup(getPlugin().getStatService(), getPlugin().getSkillService(), pickedSkills,
                getPlugin().getProficiencyService(), proficiencies);
        getPlugin().getMessages().send(player, "setup.completed");
    }
}
