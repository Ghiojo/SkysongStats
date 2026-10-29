package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * messages.yml as the stat commands and the /setup screen see it.
 */
class PluginTextsTest {
    private ServerMock server;
    private final AtomicReference<Messages> current = new AtomicReference<>();
    private final PluginTexts texts = new PluginTexts(current::get);

    private static Messages messages(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return new Messages(file, file, warning -> { });
    }

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        current.set(messages("hello: \"Hi {player}\"\nsheet:\n  - \"{str}\"\n  - \"{hp}/{max_hp}\"\n"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void sendsAndRendersMessages() {
        PlayerMock alice = server.addPlayer("Alice");
        texts.send(alice, "hello", Map.of("player", "Alice"));
        assertEquals("Hi Alice", plain(alice.nextComponentMessage()));
        assertEquals("Hi Bob", plain(texts.line("hello", Map.of("player", "Bob"))));
        assertEquals(2, texts.lines("sheet", Map.of()).size());
    }

    @Test
    void givesTheLinesAsWritten() {
        assertEquals(List.of("{str}", "{hp}/{max_hp}"), texts.lines("sheet"));
        assertTrue(texts.has("hello"));
        assertFalse(texts.has("missing"));
    }

    @Test
    void usesTheReloadedMessages() {
        current.set(messages("hello: \"Hola {player}\"\n"));
        assertEquals("Hola Ana", plain(texts.line("hello", Map.of("player", "Ana"))));
    }
}
