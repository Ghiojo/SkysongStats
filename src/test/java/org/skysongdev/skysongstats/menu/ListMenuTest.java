package org.skysongdev.skysongstats.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** The paged, read-only {@link ListMenu} and its {@link ListMenuListener}. */
class ListMenuTest {
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        player = MockBukkit.mock().addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static List<ItemStack> items(int count) {
        return IntStream.range(0, count).mapToObj(index -> new ItemStack(Material.STONE, index + 1)).toList();
    }

    private static ListMenu menu(int count) {
        return new ListMenu(Component.text("Title"), items(count), Component.text("Back"), Component.text("Next"));
    }

    private static ItemStack slot(ListMenu menu, int slot) {
        return menu.getInventory().getItem(slot);
    }

    @Test
    void onePageHasBordersAndNoArrows() {
        ListMenu menu = menu(3);
        assertEquals(1, menu.pages());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot(menu, 0).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot(menu, 44).getType());
        assertEquals(1, slot(menu, 9).getAmount());
        assertEquals(3, slot(menu, 11).getAmount());
        assertNull(slot(menu, 12));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot(menu, ListMenu.PREVIOUS_SLOT).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot(menu, ListMenu.NEXT_SLOT).getType());
    }

    @Test
    void anEmptyMenuHasOnePage() {
        ListMenu menu = menu(0);
        assertEquals(1, menu.pages());
        assertNull(slot(menu, 9));
    }

    @Test
    void arrowsTurnThePages() {
        ListMenu menu = menu(60);
        assertEquals(3, menu.pages());
        assertEquals(Material.ARROW, slot(menu, ListMenu.NEXT_SLOT).getType());

        menu.click(ListMenu.NEXT_SLOT);
        assertEquals(1, menu.page());
        assertEquals(28, slot(menu, 9).getAmount());
        assertEquals(Material.ARROW, slot(menu, ListMenu.PREVIOUS_SLOT).getType());

        menu.click(ListMenu.NEXT_SLOT);
        menu.click(ListMenu.NEXT_SLOT);
        assertEquals(2, menu.page());
        assertEquals(55, slot(menu, 9).getAmount());
        assertNull(slot(menu, 15));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot(menu, ListMenu.NEXT_SLOT).getType());

        menu.click(ListMenu.PREVIOUS_SLOT);
        assertEquals(1, menu.page());
        menu.click(20);
        assertEquals(1, menu.page());
    }

    @Test
    void theListenerCancelsClicksAndTurnsPages() {
        ListMenu menu = menu(30);
        player.openInventory(menu.getInventory());
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, ListMenu.NEXT_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        new ListMenuListener().onClick(click);

        assertTrue(click.isCancelled());
        assertEquals(1, menu.page());
    }
}
