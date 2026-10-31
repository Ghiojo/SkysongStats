package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillSetup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

// The /setup proficiency screen, after the skill screen. It shows the trained
// proficiencies of proficiencies.yml's setup: section and every proficiency of a
// skill the player has picks for, in file order from the first free slot, with
// the header listing the picks left per skill. Left click picks a proficiency,
// right click un-picks it; picked and trained ones glow. Confirming needs every
// pick used, except picks of a skill with nothing left to pick, and passes the
// trained and picked proficiencies to onConfirm. Closing the screen without
// confirming clears the picks. Its text is the proficiency.choice.* keys of
// messages.yml.
public final class ProficiencySetupMenu implements SetupScreen {
    public static final int SIZE = 54;
    static final int HEADER_SLOT = 4;
    public static final int CONFIRM_SLOT = 49;
    private static final String CHOICE = "proficiency.choice.";

    private final Inventory inventory;
    private final List<ProficiencyDefinition> shown;
    private final ProficiencyPicks picks;
    private final List<SkillDefinition> skills;
    private final MenuTexts texts;
    private final BiConsumer<Player, List<ProficiencyDefinition>> onConfirm;

    // skills are the skills.yml skills in file order, for the header.
    private ProficiencySetupMenu(List<ProficiencyDefinition> shown, ProficiencyPicks picks,
                                 List<SkillDefinition> skills, MenuTexts texts,
                                 BiConsumer<Player, List<ProficiencyDefinition>> onConfirm) {
        this.shown = List.copyOf(shown);
        this.picks = picks;
        this.skills = List.copyOf(skills);
        this.texts = Objects.requireNonNull(texts, "texts");
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
        this.inventory = Bukkit.createInventory(this, SIZE);
        drawFrame();
        drawAll();
    }

    // The screen for a player who picked these skill ids on the skill screen, or
    // empty when it would show nothing: nothing trained and no picks.
    public static Optional<ProficiencySetupMenu> forPickedSkills(
            ProficienciesConfig config, List<SkillDefinition> skills, List<String> pickedSkills, MenuTexts texts,
            BiConsumer<Player, List<ProficiencyDefinition>> onConfirm) {
        ProficiencyPicks picks = new ProficiencyPicks(config.setup().picksFor(pickedSkills),
                new HashSet<>(config.setup().trained()));
        List<ProficiencyDefinition> shown = config.proficiencies().all().stream()
                .filter(proficiency -> picks.isTrained(proficiency) || picks.skills().contains(proficiency.skill()))
                .toList();
        return shown.isEmpty() ? Optional.empty()
                : Optional.of(new ProficiencySetupMenu(shown, picks, skills, texts, onConfirm));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public void click(Player player, int slot, ClickType click) {
        if (slot == CONFIRM_SLOT) {
            confirm(player);
            return;
        }
        int index = slot - SkillSetup.FIRST_SLOT;
        if (index < 0 || index >= shown.size()) {
            return;
        }
        ProficiencyDefinition proficiency = shown.get(index);
        boolean changed = click.isLeftClick() ? picks.pick(proficiency)
                : click.isRightClick() && picks.unpick(proficiency);
        if (changed) {
            drawProficiency(index);
            drawHeader();
        }
    }

    // The next /setup starts with no picks.
    @Override
    public void closed() {
        picks.reset();
        drawAll();
    }

    ProficiencyPicks picks() {
        return picks;
    }

    private void confirm(Player player) {
        picks.skillStillToPick(shown).ifPresentOrElse(skill -> askForMore(player, skill), () -> finish(player));
    }

    private void askForMore(Player player, String skill) {
        player.sendMessage(texts.line(CHOICE + "pick-more", Map.of("picks", Integer.toString(picks.left(skill)),
                "skill", skillName(skill))));
    }

    private void finish(Player player) {
        List<ProficiencyDefinition> raised = new ArrayList<>(shown.stream().filter(picks::isTrained).toList());
        raised.addAll(picks.picked());
        player.closeInventory();
        onConfirm.accept(player, raised);
    }

    private void drawFrame() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SkillSetup.FIRST_SLOT; slot++) {
            inventory.setItem(slot, border);
        }
        for (int slot = SkillSetup.LAST_SLOT + 1; slot < SIZE; slot++) {
            inventory.setItem(slot, border);
        }
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        confirm.editMeta(meta -> meta.displayName(texts.line(CHOICE + "confirm", Map.of())));
        inventory.setItem(CONFIRM_SLOT, confirm);
    }

    private void drawAll() {
        for (int index = 0; index < shown.size(); index++) {
            drawProficiency(index);
        }
        drawHeader();
    }

    // The header lists the skills with picks in skills.yml order.
    private void drawHeader() {
        List<Component> lore = new ArrayList<>(texts.lines(CHOICE + "lore", Map.of()));
        skills.stream().filter(skill -> picks.skills().contains(skill.id())).forEach(skill ->
                lore.add(MenuItems.colored(skill.color(), texts.line(CHOICE + "skill-picks",
                        Map.of("skill", skill.name(), "picks", Integer.toString(picks.left(skill.id())))))));
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        header.editMeta(meta -> {
            meta.displayName(texts.line(CHOICE + "title", Map.of()));
            meta.lore(lore);
        });
        inventory.setItem(HEADER_SLOT, header);
    }

    private void drawProficiency(int index) {
        ProficiencyDefinition proficiency = shown.get(index);
        ItemStack item = MenuItems.icon(proficiency.setup().icon());
        boolean trained = picks.isTrained(proficiency);
        item.editMeta(meta -> {
            meta.displayName(MenuItems.colored(proficiency.color(),
                    texts.line(CHOICE + "proficiency-name", Map.of("name", proficiency.name()))));
            List<Component> lore = new ArrayList<>();
            proficiency.setup().lore().forEach(line -> lore.add(MenuItems.MINI_MESSAGE.deserialize(line)));
            if (trained) {
                lore.addAll(texts.lines(CHOICE + "trained", Map.of()));
            }
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(trained || picks.isPicked(proficiency) ? Boolean.TRUE : null);
        });
        inventory.setItem(SkillSetup.FIRST_SLOT + index, item);
    }

    private String skillName(String id) {
        return skills.stream().filter(skill -> skill.id().equals(id)).map(SkillDefinition::name)
                .findFirst().orElse(id);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
