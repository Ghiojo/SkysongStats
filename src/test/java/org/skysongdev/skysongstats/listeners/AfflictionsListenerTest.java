package org.skysongdev.skysongstats.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.Utils.SkysongItemsKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfflictionsListenerTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack item(int amount, Object... keysAndValues) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            meta.getPersistentDataContainer().set((NamespacedKey) keysAndValues[i], PersistentDataType.INTEGER, (Integer) keysAndValues[i + 1]);
        }
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void emptySlotsAndAirCountAsZero() {
        ItemStack[] items = {null, new ItemStack(Material.AIR)};
        assertEquals(0, AfflictionsListener.CountArcrot(items));
        assertEquals(0, AfflictionsListener.CountFictis(items));
    }

    @Test
    void untaggedItemsCountAsZero() {
        ItemStack[] items = {new ItemStack(Material.STONE, 64)};
        assertEquals(0, AfflictionsListener.CountArcrot(items));
        assertEquals(0, AfflictionsListener.CountFictis(items));
    }

    @Test
    void arcrotIsMultipliedByStackSizeAndSummed() {
        ItemStack[] items = {
                item(3, SkysongItemsKeys.ARCROT, 2),
                item(1, SkysongItemsKeys.ARCROT, 5)
        };
        assertEquals(11, AfflictionsListener.CountArcrot(items));
    }

    @Test
    void chargedArcrotCountsWhileChargesRemain() {
        ItemStack[] items = {item(2, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 1)};
        assertEquals(8, AfflictionsListener.CountArcrot(items));
    }

    @Test
    void chargedArcrotIgnoredWithNoChargesLeft() {
        ItemStack[] items = {item(1, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 0)};
        assertEquals(0, AfflictionsListener.CountArcrot(items));
    }

    @Test
    void chargedArcrotIgnoredWithoutAChargesTag() {
        ItemStack[] items = {item(1, SkysongItemsKeys.CHARGED_ARCROT, 4)};
        assertEquals(0, AfflictionsListener.CountArcrot(items));
    }

    @Test
    void plainAndChargedArcrotOnOneItemAddUp() {
        ItemStack[] items = {item(1, SkysongItemsKeys.ARCROT, 3, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 2)};
        assertEquals(7, AfflictionsListener.CountArcrot(items));
    }

    @Test
    void fictisIsMultipliedByStackSizeAndSummed() {
        ItemStack[] items = {
                item(2, SkysongItemsKeys.FICTIS, 3),
                item(1, SkysongItemsKeys.FICTIS, 1),
                item(5, SkysongItemsKeys.ARCROT, 9)
        };
        assertEquals(7, AfflictionsListener.CountFictis(items));
    }

    @Test
    void keysFromAnotherNamespaceAreIgnored() {
        ItemStack[] items = {item(1, new NamespacedKey("other_plugin", "arcrot"), 9, new NamespacedKey("other_plugin", "fictis"), 9)};
        assertEquals(0, AfflictionsListener.CountArcrot(items));
        assertEquals(0, AfflictionsListener.CountFictis(items));
    }
}
