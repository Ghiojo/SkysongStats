package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.api.ItemSource;
import org.skysongdev.skysongstats.api.ItemSourceRegistration;
import org.skysongdev.skysongstats.items.ItemSourceRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourcesImplTest {
    private static final ItemSource EMPTY = player -> List.of();

    private final List<Player> recounted = new ArrayList<>();
    private ServerMock server;
    private ItemSourceRegistry registry;
    private ItemSourcesImpl sources;
    private Plugin backpacks;
    private Player alice;
    private Player bob;
    private boolean onMainThread = true;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        backpacks = MockBukkit.createMockPlugin("Backpacks");
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        registry = new ItemSourceRegistry(line -> { });
        sources = new ItemSourcesImpl(registry, recounted::add, () -> List.of(alice, bob),
                new MainThread(() -> onMainThread));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aRegisteredSourceIsReadAndOnlinePlayersAreRecounted() {
        ItemSourceRegistration registration = sources.register(backpacks, "backpack", player -> List.of());

        assertSame(backpacks, registration.owner());
        assertEquals("backpack", registration.id());
        assertTrue(registration.isRegistered());
        assertEquals(List.of(alice, bob), recounted);
    }

    @Test
    void theRegistryAsksTheRegisteredSource() {
        List<Player> asked = new ArrayList<>();
        sources.register(backpacks, "backpack", player -> {
            asked.add(player);
            return List.of();
        });

        registry.items(alice);

        assertEquals(List.of(alice), asked);
    }

    @Test
    void unregisterRemovesTheSourceAndRecountsOnce() {
        ItemSourceRegistration registration = sources.register(backpacks, "backpack", EMPTY);
        recounted.clear();

        registration.unregister();
        registration.unregister();

        assertFalse(registration.isRegistered());
        assertEquals(List.of(alice, bob), recounted, "the second unregister does nothing");
    }

    @Test
    void aPluginsSourcesGoWhenItDisables() {
        ItemSourceRegistration registration = sources.register(backpacks, "backpack", EMPTY);
        recounted.clear();

        sources.onPluginDisable(new PluginDisableEvent(backpacks));

        assertFalse(registration.isRegistered());
        assertEquals(List.of(alice, bob), recounted);
    }

    @Test
    void anotherPluginDisablingChangesNothing() {
        sources.register(backpacks, "backpack", EMPTY);
        recounted.clear();

        sources.onPluginDisable(new PluginDisableEvent(MockBukkit.createMockPlugin("Other")));

        assertEquals(List.of(), recounted);
    }

    @Test
    void anIdCanOnlyBeRegisteredOncePerPlugin() {
        sources.register(backpacks, "backpack", EMPTY);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> sources.register(backpacks, "backpack", EMPTY));

        assertTrue(error.getMessage().contains("already has an item source 'backpack'"), error.getMessage());
    }

    @Test
    void anIdMustBeLowercaseLettersDigitsUnderscoresAndDashes() {
        sources.register(backpacks, "ender_pack-2", EMPTY);

        for (String bad : List.of("", "Backpack", "back pack", "back.pack")) {
            assertThrows(IllegalArgumentException.class, () -> sources.register(backpacks, bad, EMPTY), bad);
        }
    }

    @Test
    void aDisabledPluginCannotRegister() {
        server.getPluginManager().disablePlugin(backpacks);

        assertThrows(IllegalStateException.class, () -> sources.register(backpacks, "backpack", EMPTY));
    }

    @Test
    void recountRecountsThatPlayer() {
        sources.recount(bob);

        assertEquals(List.of(bob), recounted);
    }

    @Test
    void everyMethodRefusesOtherThreads() {
        ItemSourceRegistration registration = sources.register(backpacks, "backpack", EMPTY);
        onMainThread = false;

        assertThrows(IllegalStateException.class, () -> sources.register(backpacks, "pouch", EMPTY));
        assertThrows(IllegalStateException.class, () -> sources.recount(alice));
        assertThrows(IllegalStateException.class, registration::unregister);
        assertTrue(registration.isRegistered());
    }

    @Test
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> sources.register(null, "backpack", EMPTY));
        assertThrows(NullPointerException.class, () -> sources.register(backpacks, null, EMPTY));
        assertThrows(NullPointerException.class, () -> sources.register(backpacks, "backpack", null));
        assertThrows(NullPointerException.class, () -> sources.recount(null));
    }
}
