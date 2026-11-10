package org.skysongdev.skysongstats.commands.setup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.character.AncestryRules;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.setup.AncestryChoicesMenu;
import org.skysongdev.skysongstats.setup.AncestrySetupMenu;
import org.skysongdev.skysongstats.setup.MenuTexts;
import org.skysongdev.skysongstats.setup.ProficiencySetupMenu;
import org.skysongdev.skysongstats.setup.SkillSetupMenu;
import org.skysongdev.skysongstats.setup.StatSetupMenu;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        // The ancestry list comes from ancestries.yml (skipped when the player may
        // pick none), then the ancestry's choices page if it has choices; then the
        // stat point screen from stats.yml, the skill screen from skills.yml and
        // the proficiency screen from proficiencies.yml, and confirming that
        // finishes the setup.
        openAncestryScreen(target, setup);
        return true;
    }

    private void openAncestryScreen(Player player, SetupProfile setup) {
        List<AncestryDefinition> ancestries = getPlugin().getAncestryDefinitions().all().stream()
                .filter(ancestry -> ancestry.permission().map(player::hasPermission).orElse(true))
                .toList();
        if (ancestries.isEmpty()) {
            openStatScreen(player, setup, Optional.empty(), AncestryPicks.none());
            return;
        }
        StatDefinitions stats = getPlugin().getStatsConfig().stats();
        new AncestrySetupMenu(ancestries, stats, texts, (picker, ancestry) -> {
            if (!ancestry.hasChoices()) {
                openStatScreen(picker, setup, Optional.of(ancestry), AncestryPicks.none());
                return;
            }
            new AncestryChoicesMenu(ancestry, stats, texts, Optional.of(back -> openAncestryScreen(back, setup)),
                    (chooser, picks) -> openStatScreen(chooser, setup, Optional.of(ancestry), picks)).open(picker);
        }).open(player);
    }

    // The point screen starts each stat at its baseline plus what the ancestry
    // gives it.
    private void openStatScreen(Player player, SetupProfile setup, Optional<AncestryDefinition> ancestry,
                                AncestryPicks picks) {
        StatsConfig config = getPlugin().getStatsConfig();
        Map<String, Integer> amounts = ancestry.map(picked -> AncestryRules.totals(picked, picks)).orElse(Map.of());
        String name = ancestry.map(AncestryDefinition::name).orElse("");
        new StatSetupMenu(config.stats().setupStats(), config.setupRules(), name, amounts, texts, (confirmer, values) -> {
            setup.setChosenStats(values);
            ancestry.ifPresent(picked -> setup.setChosenAncestry(picked, picks));
            openSkillScreen(confirmer, setup);
        }).open(player);
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
        getPlugin().getChangeCauses().during(ChangeCause.SETUP, () -> setup.finishProfileSetup(
                getPlugin().getStatService(), getPlugin().getAncestryService(), getPlugin().getSkillService(), pickedSkills,
                getPlugin().getProficiencyService(), proficiencies));
        getPlugin().getMessages().send(player, "setup.completed");
    }
}
