package org.skysongdev.skysongstats.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.service.downtime.DowntimeChange;
import org.skysongdev.skysongstats.service.downtime.DowntimePool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DowntimeChangeEventTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");

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
    void exposesTheChange() {
        DowntimeChange change = new DowntimeChange(PLAYER, DowntimePool.CHARACTER, "Mage", 1, 3, DowntimeChange.Cause.RESET);
        DowntimeChangeEvent event = new DowntimeChangeEvent(change);

        assertEquals(change, event.getChange());
        assertEquals(PLAYER, event.getPlayerUUID());
        assertEquals(DowntimePool.CHARACTER, event.getPool());
        assertEquals("Mage", event.getProfile());
        assertEquals(1, event.getOldValue());
        assertEquals(3, event.getNewValue());
        assertEquals(DowntimeChange.Cause.RESET, event.getCause());
    }

    @Test
    void reachesListeners() {
        List<DowntimeChangeEvent> received = new ArrayList<>();
        Plugin plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDowntimeChange(DowntimeChangeEvent event) {
                received.add(event);
            }
        }, plugin);

        DowntimeChange change = new DowntimeChange(PLAYER, DowntimePool.GLOBAL, null, 3, 4, DowntimeChange.Cause.ADD);
        server.getPluginManager().callEvent(new DowntimeChangeEvent(change));

        assertEquals(1, received.size());
        assertNull(received.get(0).getProfile());
    }
}
