package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.character.AncestryPicks.PackagePick;
import org.skysongdev.skysongstats.character.AncestryPicks.StatsPick;
import org.skysongdev.skysongstats.character.AncestryRules;
import org.skysongdev.skysongstats.definition.AncestryChoice;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryPackage;
import org.skysongdev.skysongstats.definition.PackageChoice;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatPickChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// The page where a player makes their ancestry's choices: in /setup after picking
// an ancestry with choices:, and for /ancestry choose. One row per choice: the
// first slot explains it, the others hold the stats (from stats.yml) or packages
// it can pick. Clicking picks or un-picks; picks the no-stacking rule forbids are
// marked and can't be clicked. The top row holds Back (only when onBack is
// given), the header and Confirm, which passes the picks to onConfirm once every
// choice is complete. Closing the page drops the picks. Its text is the
// setup.ancestry-choices.* keys of messages.yml.
public final class AncestryChoicesMenu implements SetupScreen {
    public static final int SIZE = 54;
    public static final int BACK_SLOT = 0;
    static final int HEADER_SLOT = 4;
    public static final int CONFIRM_SLOT = 8;
    private static final int ROW = 9;
    private static final String CHOICES = "setup.ancestry-choices.";

    private final Inventory inventory;
    private final AncestryDefinition ancestry;
    private final MenuTexts texts;
    private final AncestryTexts ancestryTexts;
    private final Optional<Consumer<Player>> onBack;
    private final BiConsumer<Player, AncestryPicks> onConfirm;
    private AncestryPicks picks = AncestryPicks.none();

    public AncestryChoicesMenu(AncestryDefinition ancestry, StatDefinitions stats, MenuTexts texts,
                               Optional<Consumer<Player>> onBack, BiConsumer<Player, AncestryPicks> onConfirm) {
        this.ancestry = Objects.requireNonNull(ancestry, "ancestry");
        this.texts = Objects.requireNonNull(texts, "texts");
        this.ancestryTexts = new AncestryTexts(texts, stats);
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
        this.inventory = Bukkit.createInventory(this, SIZE,
                texts.line(CHOICES + "title", Map.of("ancestry", ancestry.name())));
        draw();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public int size() {
        return SIZE;
    }

    AncestryPicks picks() {
        return picks;
    }

    @Override
    public void click(Player player, int slot, ClickType click) {
        if (slot == CONFIRM_SLOT) {
            confirm(player);
            return;
        }
        if (slot == BACK_SLOT && onBack.isPresent()) {
            player.closeInventory();
            onBack.get().accept(player);
            return;
        }
        int choiceIndex = slot / ROW - 1;
        int itemIndex = slot % ROW - 1;
        if (choiceIndex < 0 || choiceIndex >= ancestry.choices().size() || itemIndex < 0) {
            return;
        }
        AncestryChoice choice = ancestry.choices().get(choiceIndex);
        boolean changed = choice instanceof StatPickChoice stats ? toggleStat(choiceIndex, stats, itemIndex)
                : togglePackage(choiceIndex, (PackageChoice) choice, itemIndex);
        if (changed) {
            draw();
        }
    }

    @Override
    public void closed() {
        picks = AncestryPicks.none();
        draw();
    }

    private boolean toggleStat(int choiceIndex, StatPickChoice choice, int itemIndex) {
        if (itemIndex >= choice.from().size()) {
            return false;
        }
        String stat = choice.from().get(itemIndex);
        List<String> picked = new ArrayList<>(pickedStats(choiceIndex));
        if (picked.remove(stat)) {
            picks = picked.isEmpty() ? picks.without(choiceIndex) : picks.withStats(choiceIndex, picked);
            return true;
        }
        if (AncestryRules.conflicts(ancestry, picks, choiceIndex, stat, choice.amount())) {
            return false;
        }
        if (picked.size() >= choice.picks()) {
            if (choice.picks() != 1) {
                return false;
            }
            picked.clear();
        }
        picked.add(stat);
        picks = picks.withStats(choiceIndex, picked);
        return true;
    }

    private boolean togglePackage(int choiceIndex, PackageChoice choice, int itemIndex) {
        if (itemIndex >= choice.options().size()) {
            return false;
        }
        if (pickedPackage(choiceIndex).filter(option -> option == itemIndex).isPresent()) {
            picks = picks.without(choiceIndex);
            return true;
        }
        if (blocked(choiceIndex, choice.options().get(itemIndex))) {
            return false;
        }
        picks = picks.withPackage(choiceIndex, itemIndex);
        return true;
    }

    private void confirm(Player player) {
        for (int index = 0; index < ancestry.choices().size(); index++) {
            if (!isComplete(index)) {
                player.sendMessage(texts.line(CHOICES + "incomplete",
                        Map.of("choice", ancestryTexts.choiceName(ancestry.choices().get(index), index))));
                return;
            }
        }
        AncestryPicks confirmed = picks;
        player.closeInventory();
        onConfirm.accept(player, confirmed);
    }

    private boolean isComplete(int index) {
        AncestryChoice choice = ancestry.choices().get(index);
        if (choice instanceof StatPickChoice stats) {
            return pickedStats(index).size() == stats.picks();
        }
        return pickedPackage(index).isPresent();
    }

    private List<String> pickedStats(int choiceIndex) {
        return picks.get(choiceIndex).filter(StatsPick.class::isInstance).map(pick -> ((StatsPick) pick).stats())
                .orElse(List.of());
    }

    private Optional<Integer> pickedPackage(int choiceIndex) {
        return picks.get(choiceIndex).filter(PackagePick.class::isInstance).map(pick -> ((PackagePick) pick).option());
    }

    private boolean blocked(int choiceIndex, AncestryPackage option) {
        return option.adjustments().stream().anyMatch(adjustment ->
                AncestryRules.conflicts(ancestry, picks, choiceIndex, adjustment.stat(), adjustment.amount()));
    }

    private void draw() {
        inventory.clear();
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < ROW; slot++) {
            inventory.setItem(slot, border);
        }
        onBack.ifPresent(back -> inventory.setItem(BACK_SLOT, button(Material.RED_STAINED_GLASS_PANE, "back")));
        inventory.setItem(CONFIRM_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "confirm"));
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        header.editMeta(meta -> {
            Map<String, String> values = Map.of("ancestry", ancestry.name());
            meta.displayName(texts.line(CHOICES + "header.name", values));
            meta.lore(texts.lines(CHOICES + "header.lore", values));
        });
        inventory.setItem(HEADER_SLOT, header);
        for (int index = 0; index < ancestry.choices().size(); index++) {
            drawChoice(index);
        }
    }

    private ItemStack button(Material material, String key) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(texts.line(CHOICES + key, Map.of())));
        return item;
    }

    private void drawChoice(int index) {
        int rowStart = (index + 1) * ROW;
        AncestryChoice choice = ancestry.choices().get(index);
        String name = ancestryTexts.choiceName(choice, index);
        ItemStack explanation = new ItemStack(isComplete(index) ? Material.LIME_DYE : Material.GRAY_DYE);
        if (choice instanceof StatPickChoice stats) {
            Map<String, String> values = Map.of("name", name, "amount", AncestryTexts.signed(stats.amount()),
                    "picks", Integer.toString(stats.picks()),
                    "left", Integer.toString(stats.picks() - pickedStats(index).size()));
            describe(explanation, "choice-stats", values);
            inventory.setItem(rowStart, explanation);
            for (int item = 0; item < stats.from().size(); item++) {
                inventory.setItem(rowStart + 1 + item, statItem(index, stats, stats.from().get(item)));
            }
        } else {
            PackageChoice packages = (PackageChoice) choice;
            describe(explanation, "choice-package", Map.of("name", name));
            inventory.setItem(rowStart, explanation);
            for (int item = 0; item < packages.options().size(); item++) {
                inventory.setItem(rowStart + 1 + item, packageItem(index, packages.options().get(item), item));
            }
        }
    }

    private void describe(ItemStack item, String key, Map<String, String> values) {
        item.editMeta(meta -> {
            meta.displayName(texts.line(CHOICES + key + ".name", values));
            meta.lore(texts.lines(CHOICES + key + ".lore", values));
        });
    }

    private ItemStack statItem(int choiceIndex, StatPickChoice choice, String stat) {
        boolean picked = pickedStats(choiceIndex).contains(stat);
        boolean blocked = !picked && AncestryRules.conflicts(ancestry, picks, choiceIndex, stat, choice.amount());
        ItemStack item = MenuItems.icon(ancestryTexts.stat(stat).setup().icon());
        Map<String, String> values = Map.of("stat", ancestryTexts.statName(stat),
                "amount", AncestryTexts.signed(choice.amount()));
        item.editMeta(meta -> {
            meta.displayName(texts.line(CHOICES + "stat.name", values));
            meta.lore(List.of(state(picked, blocked)));
            meta.setEnchantmentGlintOverride(picked ? Boolean.TRUE : null);
        });
        return item;
    }

    private ItemStack packageItem(int choiceIndex, AncestryPackage option, int optionIndex) {
        boolean picked = pickedPackage(choiceIndex).filter(chosen -> chosen == optionIndex).isPresent();
        boolean blocked = !picked && blocked(choiceIndex, option);
        ItemStack item = MenuItems.icon(option.icon());
        Map<String, String> values = Map.of("name", option.name(), "adjustments", ancestryTexts.packageText(option));
        item.editMeta(meta -> {
            meta.displayName(texts.line(CHOICES + "package.name", values));
            List<Component> lore = new ArrayList<>(texts.lines(CHOICES + "package.lore", values));
            lore.add(state(picked, blocked));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(picked ? Boolean.TRUE : null);
        });
        return item;
    }

    // Picked, open, or blocked by the no-stacking rule.
    private Component state(boolean picked, boolean blocked) {
        String key = picked ? "picked" : blocked ? "blocked" : "open";
        return texts.line(CHOICES + key, Map.of());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
