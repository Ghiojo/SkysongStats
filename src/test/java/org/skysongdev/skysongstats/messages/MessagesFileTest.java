package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesFileTest {
    private static final String BUNDLED = """
            # Header comment
            prefix: "[SS] "
            general:
              # Shown after /ssreload
              reloaded: "{prefix}Reloaded!"
              no-permission: "{prefix}No permission"
            """;

    private final List<String> log = new ArrayList<>();
    private File file;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        file = folder.resolve("SkysongStats").resolve(MessagesFile.FILE_NAME).toFile();
    }

    private void writeServerFile(String text) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        Files.writeString(file.toPath(), text);
    }

    private static String plain(Messages messages, String key) {
        return PlainTextComponentSerializer.plainText().serialize(messages.get(key));
    }

    @Test
    void firstStartWritesTheBundledFileAsItIs() throws IOException {
        Messages messages = MessagesFile.load(file, BUNDLED, log::add);
        assertEquals(BUNDLED, Files.readString(file.toPath()));
        assertEquals("[SS] Reloaded!", plain(messages, "general.reloaded"));
        assertTrue(log.isEmpty());
    }

    @Test
    void addsNewMessagesAndKeepsEditedOnes() throws IOException {
        writeServerFile("prefix: \"[Stats] \"\ngeneral:\n  reloaded: \"{prefix}Recargado\"\n");
        Messages messages = MessagesFile.load(file, BUNDLED, log::add);

        assertEquals("[Stats] Recargado", plain(messages, "general.reloaded"));
        assertEquals("[Stats] No permission", plain(messages, "general.no-permission"));
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file);
        assertEquals("{prefix}Recargado", saved.getString("general.reloaded"));
        assertEquals("{prefix}No permission", saved.getString("general.no-permission"));
        assertEquals(List.of("messages.yml: added the new message 'general.no-permission'"), log);
    }

    @Test
    void copiesTheCommentsOfAddedMessages() throws IOException {
        writeServerFile("prefix: \"[SS] \"\n");
        MessagesFile.load(file, BUNDLED, log::add);
        assertTrue(Files.readString(file.toPath()).contains("# Shown after /ssreload"));
    }

    @Test
    void leavesACompleteFileUntouched() throws IOException {
        String edited = "# my translation\nprefix: \"[SS] \"\ngeneral:\n  reloaded: \"Listo\"\n  no-permission: \"No\"\n";
        writeServerFile(edited);
        MessagesFile.load(file, BUNDLED, log::add);
        assertEquals(edited, Files.readString(file.toPath()));
        assertTrue(log.isEmpty());
    }
}
