package org.skysongdev.skysongstats.commands.proficiency;

import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Focus switches waiting for /proficiency focus confirm, on a clock the tests move.
 */
class PendingFocusChangesTest {
    private final AtomicLong now = new AtomicLong(1_000);
    private final PendingFocusChanges pending = new PendingFocusChanges(now::get);
    private ServerMock server;
    private PlayerMock alice;
    private ProfileKey main;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        alice = server.addPlayer("Alice");
        main = new ProfileKey(alice.getUniqueId(), "main");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aSwitchCanBeConfirmedOnce() {
        pending.put(main, "axes");
        assertEquals(Optional.of("axes"), pending.take(main));
        assertEquals(Optional.empty(), pending.take(main));
    }

    @Test
    void aNewQuestionReplacesTheOldOne() {
        pending.put(main, "axes");
        pending.put(main, "bows");
        assertEquals(Optional.of("bows"), pending.take(main));
    }

    @Test
    void aSwitchRunsOutAfterAMinute() {
        pending.put(main, "axes");
        now.addAndGet(PendingFocusChanges.TIMEOUT_MILLIS);
        assertEquals(Optional.of("axes"), pending.take(main));
        pending.put(main, "axes");
        now.addAndGet(PendingFocusChanges.TIMEOUT_MILLIS + 1);
        assertEquals(Optional.empty(), pending.take(main));
    }

    @Test
    void anotherProfileCantConfirmIt() {
        pending.put(main, "axes");
        assertEquals(Optional.empty(), pending.take(new ProfileKey(alice.getUniqueId(), "alt")));
        assertEquals(Optional.empty(), pending.take(main));
    }

    @Test
    void loggingOutForgetsIt() {
        pending.put(main, "axes");
        pending.onQuit(new PlayerQuitEvent(alice, Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertEquals(Optional.empty(), pending.take(main));
    }
}
