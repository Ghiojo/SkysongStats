package org.skysongdev.skysongstats.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourceRegistryTest {
    private final List<String> warnings = new ArrayList<>();
    private ItemSourceRegistry registry;
    private Plugin backpacks;
    private Plugin quivers;
    private Player player;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        backpacks = MockBukkit.createMockPlugin("Backpacks");
        quivers = MockBukkit.createMockPlugin("Quivers");
        player = server.addPlayer();
        registry = new ItemSourceRegistry(warnings::add);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack item(Material material) {
        return new ItemStack(material);
    }

    @Test
    void everySourcesItemsInRegistrationOrder() {
        ItemStack stick = item(Material.STICK);
        ItemStack stone = item(Material.STONE);
        ItemStack arrow = item(Material.ARROW);
        registry.add(backpacks, "backpack", p -> List.of(stick, stone));
        registry.add(quivers, "quiver", p -> List.of(arrow));

        assertEquals(List.of(stick, stone, arrow), registry.items(player));
    }

    @Test
    void theSourceIsAskedForThatPlayer() {
        List<Player> asked = new ArrayList<>();
        registry.add(backpacks, "backpack", p -> {
            asked.add(p);
            return List.of();
        });

        registry.items(player);

        assertEquals(List.of(player), asked);
    }

    @Test
    void anIdIsTakenOncePerPlugin() {
        assertTrue(registry.add(backpacks, "backpack", p -> List.of()).isPresent());
        assertTrue(registry.add(backpacks, "backpack", p -> List.of()).isEmpty());
        assertTrue(registry.add(quivers, "backpack", p -> List.of()).isPresent(), "another plugin may use it");
    }

    @Test
    void aRemovedSourceStopsCountingAndItsIdIsFreeAgain() {
        ItemSourceRegistry.Entry entry = registry.add(backpacks, "backpack", p -> List.of(item(Material.STICK)))
                .orElseThrow();

        assertTrue(registry.remove(entry));
        assertFalse(registry.remove(entry), "removing twice does nothing");
        assertFalse(entry.isRegistered());
        assertEquals(List.of(), registry.items(player));
        assertTrue(registry.add(backpacks, "backpack", p -> List.of()).isPresent());
    }

    @Test
    void removeOwnerRemovesOnlyThatPluginsSources() {
        ItemSourceRegistry.Entry backpack = registry.add(backpacks, "backpack", p -> List.of()).orElseThrow();
        ItemSourceRegistry.Entry pouch = registry.add(backpacks, "pouch", p -> List.of()).orElseThrow();
        ItemSourceRegistry.Entry quiver = registry.add(quivers, "quiver", p -> List.of()).orElseThrow();

        assertTrue(registry.removeOwner(backpacks));
        assertFalse(registry.removeOwner(backpacks), "nothing left to remove");

        assertFalse(backpack.isRegistered());
        assertFalse(pouch.isRegistered());
        assertTrue(quiver.isRegistered());
    }

    @Test
    void clearRemovesEverything() {
        ItemSourceRegistry.Entry backpack = registry.add(backpacks, "backpack", p -> List.of()).orElseThrow();

        registry.clear();

        assertFalse(backpack.isRegistered());
        assertEquals(List.of(), registry.items(player));
    }

    @Test
    void aSourceThatThrowsCountsNothingAndIsLoggedOnce() {
        ItemStack arrow = item(Material.ARROW);
        registry.add(backpacks, "backpack", p -> {
            throw new IllegalStateException("broken");
        });
        registry.add(quivers, "quiver", p -> List.of(arrow));

        assertEquals(List.of(arrow), registry.items(player), "the other sources still count");
        registry.items(player);

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("'backpack' of Backpacks"), warnings.get(0));
        assertTrue(warnings.get(0).contains("broken"), warnings.get(0));
    }

    @Test
    void aSourceThatAnswersNullCountsNothingAndIsLoggedOnce() {
        registry.add(backpacks, "backpack", p -> null);

        assertEquals(List.of(), registry.items(player));
        registry.items(player);

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("returned null"), warnings.get(0));
    }
}
