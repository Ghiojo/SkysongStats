package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.definition.Icon;
import org.skysongdev.skysongstats.definition.SetupRules;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatSetup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

// The /setup point allocation screen, built from the stats with a setup: section
// in stats.yml: each one's icon in its slot, the header with the points left, and
// the confirm button. Left click raises a stat, right click lowers it (see
// PointAllocation). Confirming with every point spent passes the chosen values,
// by stat id, to onConfirm. Closing the screen without confirming starts over.
// Its text is the setup.* keys of messages.yml.
public final class StatSetupMenu implements InventoryHolder {
    public static final int SIZE = 27;
    private static final String SETUP = "setup.";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Inventory inventory;
    private final PointAllocation allocation;
    private final Map<Integer, StatDefinition> statsBySlot = new HashMap<>();
    private final MenuTexts texts;
    private final BiConsumer<Player, Map<String, Integer>> onConfirm;

    public StatSetupMenu(List<StatDefinition> stats, SetupRules rules, MenuTexts texts,
                         BiConsumer<Player, Map<String, Integer>> onConfirm) {
        this.texts = Objects.requireNonNull(texts, "texts");
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
        this.allocation = new PointAllocation(stats, rules);
        stats.forEach(stat -> statsBySlot.put(stat.setup().slot(), stat));
        this.inventory = Bukkit.createInventory(this, SIZE, text("title"));
        drawFrame();
        drawAll();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    // A click on a slot of this screen.
    public void click(Player player, int slot, ClickType click) {
        if (slot == StatSetup.CONFIRM_SLOT) {
            confirm(player);
            return;
        }
        StatDefinition stat = statsBySlot.get(slot);
        if (stat == null) {
            return;
        }
        boolean changed = click.isLeftClick() ? allocation.raise(stat) : click.isRightClick() && allocation.lower(stat);
        if (changed) {
            drawStat(stat);
            drawHeader();
        }
    }

    // The player closed the screen: the next /setup starts from the baselines.
    public void closed() {
        allocation.reset();
        drawAll();
    }

    PointAllocation allocation() {
        return allocation;
    }

    private void confirm(Player player) {
        if (!allocation.canConfirm()) {
            player.sendMessage(text("points-left"));
            return;
        }
        Map<String, Integer> values = allocation.values();
        player.closeInventory();
        onConfirm.accept(player, values);
    }

    private void drawFrame() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            boolean edge = slot < 9 || slot >= 18;
            if (edge && !statsBySlot.containsKey(slot)) {
                inventory.setItem(slot, border);
            }
        }
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        confirm.editMeta(meta -> meta.displayName(text("confirm.name")));
        inventory.setItem(StatSetup.CONFIRM_SLOT, confirm);
    }

    private void drawAll() {
        statsBySlot.values().forEach(this::drawStat);
        drawHeader();
    }

    private void drawHeader() {
        Map<String, String> points = Map.of(
                "points", Integer.toString(allocation.points()),
                "negative-points", Integer.toString(allocation.negativePoints()));
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        header.editMeta(meta -> {
            meta.displayName(texts.line(SETUP + "header.name", points));
            meta.lore(texts.lines(SETUP + "header.lore", points));
        });
        inventory.setItem(StatSetup.HEADER_SLOT, header);
    }

    private void drawStat(StatDefinition stat) {
        Map<String, String> values = Map.of(
                "name", stat.display().name(),
                "value", Integer.toString(allocation.value(stat)),
                "min", Integer.toString(stat.baseline() + stat.setup().min()),
                "max", Integer.toString(stat.baseline() + stat.setup().max()));
        ItemStack item = icon(stat.setup().icon());
        item.editMeta(meta -> {
            meta.displayName(inStatColor(stat, texts.line(SETUP + "stat.name", values)));
            List<Component> lore = new ArrayList<>(texts.lines(SETUP + "stat.lore", values));
            stat.setup().lore().forEach(line -> lore.add(MINI_MESSAGE.deserialize(line)));
            meta.lore(lore);
        });
        inventory.setItem(stat.setup().slot(), item);
    }

    // The stat's colour from stats.yml, for the parts of the text that set none.
    private static Component inStatColor(StatDefinition stat, Component text) {
        return MINI_MESSAGE.deserialize(stat.display().color() + "<text>", Placeholder.component("text", text));
    }

    private static ItemStack icon(Icon icon) {
        if (!icon.isModel()) {
            return new ItemStack(Optional.ofNullable(Material.matchMaterial(icon.material())).orElse(Material.BARRIER));
        }
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(icon.model())));
        return item;
    }

    private Component text(String key) {
        return texts.line(SETUP + key, Map.of());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
