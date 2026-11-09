package org.skysongdev.skysongstats.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

// A read-only menu that lists items over pages, like the /injuries and
// /conditions menus: 45 slots, a border row at the top and bottom, 27 items per
// page between them, and arrows in the bottom row to turn pages. Clicks are
// handled by ListMenuListener.
public final class ListMenu implements InventoryHolder {
    static final int SIZE = 45;
    static final int PER_PAGE = 27;
    static final int FIRST_SLOT = 9;
    static final int PREVIOUS_SLOT = 39;
    static final int NEXT_SLOT = 41;

    private final Inventory inventory;
    private final List<ItemStack> entries;
    private final ItemStack previous;
    private final ItemStack next;
    private int page;

    // previousName and nextName name the page arrows.
    public ListMenu(Component title, List<ItemStack> entries, Component previousName, Component nextName) {
        this.inventory = Bukkit.createInventory(this, SIZE, Objects.requireNonNull(title, "title"));
        this.entries = List.copyOf(entries);
        this.previous = arrow(previousName);
        this.next = arrow(nextName);
        draw();
    }

    private static ItemStack arrow(Component name) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> meta.displayName(Objects.requireNonNull(name, "name")));
        return arrow;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // The page shown, counting from 0.
    public int page() {
        return page;
    }

    public int pages() {
        return Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
    }

    // A click on one of the menu's slots: an arrow turns the page.
    void click(int slot) {
        if (slot == PREVIOUS_SLOT && page > 0) {
            page--;
            draw();
        } else if (slot == NEXT_SLOT && page < pages() - 1) {
            page++;
            draw();
        }
    }

    private void draw() {
        inventory.clear();
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < FIRST_SLOT; slot++) {
            inventory.setItem(slot, border);
            inventory.setItem(FIRST_SLOT + PER_PAGE + slot, border);
        }
        int start = page * PER_PAGE;
        for (int index = start; index < Math.min(entries.size(), start + PER_PAGE); index++) {
            inventory.setItem(FIRST_SLOT + index - start, entries.get(index));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, previous);
        }
        if (page < pages() - 1) {
            inventory.setItem(NEXT_SLOT, next);
        }
    }
}
