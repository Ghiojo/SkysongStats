package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {
    private static final String DEFAULTS = """
            prefix: "[SS] "
            general:
              reloaded: "{prefix}<gray>Reloaded!"
              hello: "<green>Hello {player}"
              sheet:
                - "<gold>{player}"
                - "HP {hp}"
            """;

    private final List<String> warnings = new ArrayList<>();

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return yaml;
    }

    private Messages messages(String serverFile) {
        return new Messages(yaml(serverFile), yaml(DEFAULTS), warnings::add);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void usesTheServersText() {
        Messages messages = messages("general:\n  hello: \"Hola {player}\"");
        assertEquals("Hola Steve", plain(messages.get("general.hello", Map.of("player", "Steve"))));
    }

    @Test
    void appliesMiniMessageFormatting() {
        Component hello = messages(DEFAULTS).get("general.hello", Map.of("player", "Steve"));
        assertEquals(MiniMessage.miniMessage().deserialize("<green>Hello Steve"), hello);
    }

    @Test
    void replacesThePrefixFromTheFile() {
        Messages messages = messages("prefix: \"<red>[Stats] \"\ngeneral:\n  reloaded: \"{prefix}Done\"");
        assertEquals("[Stats] Done", plain(messages.get("general.reloaded")));
    }

    @Test
    void escapesPlaceholderValuesSoPlayersCannotAddFormatting() {
        Component hello = messages(DEFAULTS).get("general.hello", Map.of("player", "<red>Evil"));
        assertEquals("Hello <red>Evil", plain(hello));
    }

    @Test
    void leavesUnknownPlaceholdersAsWritten() {
        assertEquals("Hello {player}", plain(messages(DEFAULTS).get("general.hello")));
    }

    @Test
    void givesOneComponentPerLineOfAList() {
        List<Component> lines = messages(DEFAULTS).getLines("general.sheet", Map.of("player", "Steve", "hp", "60"));
        assertEquals(List.of("Steve", "HP 60"), lines.stream().map(MessagesTest::plain).toList());
    }

    @Test
    void joinsAListWithLineBreaksInGet() {
        Component sheet = messages(DEFAULTS).get("general.sheet", Map.of("player", "Steve", "hp", "60"));
        assertEquals("Steve\nHP 60", plain(sheet));
    }

    @Test
    void givesASingleLineMessageAsAListOfOne() {
        assertEquals(List.of("Hello {player}"), messages(DEFAULTS).getLines("general.hello").stream().map(MessagesTest::plain).toList());
    }

    @Test
    void fallsBackToTheDefaultTextAndWarnsOnce() {
        Messages messages = messages("general:\n  hello:\n    oops: 1");
        assertEquals("Hello Steve", plain(messages.get("general.hello", Map.of("player", "Steve"))));
        messages.get("general.hello");
        assertEquals(1, warnings.size());
    }

    @Test
    void hasTellsWhichKeysHaveText() {
        Messages messages = messages("general:\n  extra: \"Only here\"");
        assertTrue(messages.has("general.extra"));
        assertTrue(messages.has("general.sheet"));
        assertFalse(messages.has("general.nope"));
        assertFalse(messages.has("general"));
    }

    @Test
    void showsTheKeyForAMessageThatDoesNotExist() {
        assertEquals("general.nope", plain(messages(DEFAULTS).get("general.nope")));
        assertEquals(1, warnings.size());
    }

    @Test
    void fillsComponentPlaceholdersKeepingTheirStyle() {
        Component styled = Messages.playerStyledText("<red>Mage");
        Component hello = messages(DEFAULTS).get("general.hello", Map.of(), Map.of("player", styled));
        assertEquals(MiniMessage.miniMessage().deserialize("<green>Hello <red>Mage"), hello.compact());
    }

    @Test
    void playerStyledTextKeepsEveryChatFormat() {
        String text = "<bold><red>Hi</red></bold> <click:run_command:/char view>x</click><hover:show_text:y>z</hover>";
        assertEquals(MiniMessage.miniMessage().deserialize(text), Messages.playerStyledText(text));
        assertEquals("Hi xz", plain(Messages.playerStyledText(text)));
    }
}
