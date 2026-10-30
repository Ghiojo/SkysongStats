package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillSetup;
import org.skysongdev.skysongstats.definition.SkillSetupRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

// The /setup skill screen, built from the skills with a setup: section in
// skills.yml: each one's icon in its slot, the header with the picks left, the
// confirm button, and a border along the top and bottom rows. Left click picks a
// skill, right click un-picks it, and picked skills glow. Confirming with every
// pick used passes the picked skills to onConfirm; with picks left it says how
// many. Closing the screen without confirming clears the picks. Its text is the
// skills.choice.* keys of messages.yml.
public final class SkillSetupMenu implements SetupScreen {
    public static final int SIZE = 54;
    static final int HEADER_SLOT = 4;
    public static final int CONFIRM_SLOT = 49;
    private static final String CHOICE = "skills.choice.";

    private final Inventory inventory;
    private final SkillPicks picks;
    private final Map<Integer, SkillDefinition> skillsBySlot = new HashMap<>();
    private final MenuTexts texts;
    private final BiConsumer<Player, List<SkillDefinition>> onConfirm;

    public SkillSetupMenu(List<SkillDefinition> skills, SkillSetupRules rules, MenuTexts texts,
                          BiConsumer<Player, List<SkillDefinition>> onConfirm) {
        this.texts = Objects.requireNonNull(texts, "texts");
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
        this.picks = new SkillPicks(rules.picks());
        skills.forEach(skill -> skillsBySlot.put(skill.setup().slot(), skill));
        this.inventory = Bukkit.createInventory(this, SIZE);
        drawFrame();
        drawAll();
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
        SkillDefinition skill = skillsBySlot.get(slot);
        if (skill == null) {
            return;
        }
        boolean changed = click.isLeftClick() ? picks.pick(skill) : click.isRightClick() && picks.unpick(skill);
        if (changed) {
            drawSkill(skill);
            drawHeader();
        }
    }

    // The next /setup starts with no picks, and no skill keeps its glow.
    @Override
    public void closed() {
        picks.reset();
        drawAll();
    }

    SkillPicks picks() {
        return picks;
    }

    private void confirm(Player player) {
        if (!picks.canConfirm()) {
            player.sendMessage(texts.line(CHOICE + "pick-more", Map.of("picks", Integer.toString(picks.left()))));
            return;
        }
        List<SkillDefinition> picked = picks.picked();
        player.closeInventory();
        onConfirm.accept(player, picked);
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
        skillsBySlot.values().forEach(this::drawSkill);
        drawHeader();
    }

    private void drawHeader() {
        Map<String, String> points = Map.of("points", Integer.toString(picks.left()));
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        header.editMeta(meta -> {
            meta.displayName(texts.line(CHOICE + "title", points));
            meta.lore(texts.lines(CHOICE + "lore", points));
        });
        inventory.setItem(HEADER_SLOT, header);
    }

    private void drawSkill(SkillDefinition skill) {
        SkillSetup setup = skill.setup();
        String color = setup.color().isEmpty() ? skill.color() : setup.color();
        ItemStack item = MenuItems.icon(setup.icon());
        item.editMeta(meta -> {
            meta.displayName(MenuItems.colored(color, texts.line(CHOICE + "skill-name", Map.of("name", skill.name()))));
            List<Component> lore = new ArrayList<>();
            setup.lore().forEach(line -> lore.add(MenuItems.MINI_MESSAGE.deserialize(line)));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(picks.isPicked(skill) ? Boolean.TRUE : null);
        });
        inventory.setItem(setup.slot(), item);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
