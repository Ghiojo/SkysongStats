package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

// The first /setup screen: the ancestries the player may pick, each one's icon in
// its slot from ancestries.yml, with lore saying what it gives, under a header and
// between a border along the top and bottom rows. Clicking an ancestry passes it
// to onPick. Its text is the setup.ancestry.* keys of messages.yml.
public final class AncestrySetupMenu implements SetupScreen {
    public static final int SIZE = 54;
    static final int HEADER_SLOT = 4;
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;
    private static final String ANCESTRY = "setup.ancestry.";

    private final Inventory inventory;
    private final Map<Integer, AncestryDefinition> ancestriesBySlot = new HashMap<>();
    private final MenuTexts texts;
    private final AncestryTexts ancestryTexts;
    private final BiConsumer<Player, AncestryDefinition> onPick;

    // ancestries: only those the player may pick (the caller checks permission:).
    public AncestrySetupMenu(List<AncestryDefinition> ancestries, StatDefinitions stats, MenuTexts texts,
                             BiConsumer<Player, AncestryDefinition> onPick) {
        this.texts = Objects.requireNonNull(texts, "texts");
        this.ancestryTexts = new AncestryTexts(texts, stats);
        this.onPick = Objects.requireNonNull(onPick, "onPick");
        ancestries.forEach(ancestry -> ancestriesBySlot.put(ancestry.slot(), ancestry));
        this.inventory = Bukkit.createInventory(this, SIZE, texts.line(ANCESTRY + "title", Map.of()));
        draw();
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
        AncestryDefinition ancestry = ancestriesBySlot.get(slot);
        if (ancestry != null) {
            player.closeInventory();
            onPick.accept(player, ancestry);
        }
    }

    @Override
    public void closed() {
        // Nothing is picked until a click, so there is nothing to undo.
    }

    private void draw() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < FIRST_SLOT || slot > LAST_SLOT) {
                inventory.setItem(slot, border);
            }
        }
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        header.editMeta(meta -> {
            meta.displayName(texts.line(ANCESTRY + "header.name", Map.of()));
            meta.lore(texts.lines(ANCESTRY + "header.lore", Map.of()));
        });
        inventory.setItem(HEADER_SLOT, header);
        ancestriesBySlot.values().forEach(this::drawAncestry);
    }

    private void drawAncestry(AncestryDefinition ancestry) {
        ItemStack item = MenuItems.icon(ancestry.icon());
        item.editMeta(meta -> {
            meta.displayName(texts.line(ANCESTRY + "item-name", Map.of("name", ancestry.name())));
            List<Component> lore = new ArrayList<>(ancestryTexts.summary(ancestry));
            ancestry.lore().forEach(line -> lore.add(MenuItems.MINI_MESSAGE.deserialize(line)));
            lore.add(texts.line(ANCESTRY + "click", Map.of()));
            meta.lore(lore);
        });
        inventory.setItem(ancestry.slot(), item);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
