package org.skysongdev.skysongstats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the MockBukkit test server starts, so tests that need
 * players, items or events can rely on it.
 *
 * This does not load SkysongStats itself: onEnable() connects to the
 * database, which isn't available in tests.
 */
class MockBukkitSmokeTest {

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
    void serverCanAddPlayers() {
        PlayerMock player = server.addPlayer("Tester");
        assertEquals(1, server.getOnlinePlayers().size());
        assertEquals("Tester", player.getName());
    }
}
