package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.api.ItemSourceRegistration;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkysongStatsApiImplTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getFailsWithAClearMessageBeforeSkysongStatsIsEnabled() {
        IllegalStateException error = assertThrows(IllegalStateException.class, SkysongStatsApi::get);

        assertTrue(error.getMessage().contains("depend: [SkysongStats]"), error.getMessage());
    }

    @Test
    void enablingThePluginRegistersTheApiAsAService() {
        SkysongStats plugin = OfflineSkysongStats.load();

        RegisteredServiceProvider<SkysongStatsApi> registration =
                server.getServicesManager().getRegistration(SkysongStatsApi.class);

        assertInstanceOf(SkysongStatsApiImpl.class, registration.getProvider());
        assertSame(plugin, registration.getPlugin());
        assertSame(registration.getProvider(), SkysongStatsApi.get());
    }

    @Test
    void apiVersionIsTheImplementedVersion() {
        OfflineSkysongStats.load();

        String version = SkysongStatsApi.get().apiVersion();

        assertEquals(SkysongStatsApiImpl.API_VERSION, version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), version);
    }

    @Test
    void theSubApisAreTheSameInstanceEveryTime() {
        OfflineSkysongStats.load();
        SkysongStatsApi api = SkysongStatsApi.get();

        assertSame(api.profiles(), api.profiles());
        assertSame(api.skills(), api.skills());
        assertSame(api.stats(), api.stats());
        assertSame(api.modifiers(), api.modifiers());
        assertSame(api.proficiencies(), api.proficiencies());
        assertSame(api.downtime(), api.downtime());
        assertSame(api.sheets(), api.sheets());
        assertSame(api.itemSources(), api.itemSources());
    }

    @Test
    void disablingThePluginUnregistersTheApi() {
        SkysongStats plugin = OfflineSkysongStats.load();

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(server.getServicesManager().isProvidedFor(SkysongStatsApi.class));
        assertThrows(IllegalStateException.class, SkysongStatsApi::get);
    }

    @Test
    void anItemSourceGoesWhenItsPluginDisables() {
        OfflineSkysongStats.load();
        Plugin backpacks = MockBukkit.createMockPlugin("Backpacks");
        ItemSourceRegistration registration = SkysongStatsApi.get().itemSources()
                .register(backpacks, "backpack", player -> List.of());

        server.getPluginManager().disablePlugin(backpacks);

        assertFalse(registration.isRegistered());
    }

    @Test
    void disablingSkysongStatsDropsEveryItemSource() {
        SkysongStats plugin = OfflineSkysongStats.load();
        ItemSourceRegistration registration = SkysongStatsApi.get().itemSources()
                .register(MockBukkit.createMockPlugin("Backpacks"), "backpack", player -> List.of());

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(registration.isRegistered());
    }
}
