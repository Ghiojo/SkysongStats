package org.skysongdev.skysongstats.importer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.definition.DefinitionFile;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /ssimport chattweaks against a SkysongChatTweaks folder in a temporary
 * directory and its table in an in-memory H2 database. Both threads run the
 * work at once, so the report is there when start() returns.
 */
class ChatTweaksImporterTest {
    private static final String CHATTWEAKS_CONFIG = """
            database:
              host: "localhost"
              port: "3306"
              user: "root"
              password: ""
              database_name: "skysong_titles"
            conditions:
              bleeding: {item: RED_WOOL, name: "Their Bleeding", description: "Not ours"}
              poisoned: {item: GREEN_WOOL, name: "<green>Poisoned", description: "Feels sick"}
              Bad_Id: {item: GREEN_WOOL, name: "Bad", description: "Bad id"}
            injuries:
                cut:
                  item: RED_WOOL
                  name: "Their Cut"
                  description: "Not ours"
                  severity: "minor"
                  permissions: ["skysongchattweaks.injury.cut"]
                burn:
                  item: ORANGE_WOOL
                  name: "<gold>Burn"
                  description: "Hot"
                  severity: "Moderate"
                  permissions: ["skysongchattweaks.injury.burn", "skysongchattweaks.injury.burns"]
                sprain:
                  item: NOT_A_THING
                  name: "Sprain"
                  description: "Twisted"
                  severity: "minor"
                  permissions: ["SkysongChatTweaks.Injury.Sprain"]
                nosev:
                  item: PAPER
                  name: "No Severity"
                  permissions: ["skysongchattweaks.injury.nosev"]
            """;

    @TempDir
    File chatTweaks;

    private SkysongStats plugin;
    private AfflictionService afflictions;
    private ConsoleCommandSenderMock console;
    private PlayerMock alice;
    private PlayerMock bob;
    private JdbcDataSource dataSource;
    private ChatTweaksImporter.Connector connector;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        ServerMock server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        write(new File(plugin.getDataFolder(), "conditions.yml"), AfflictionFixtures.CONDITIONS_FILE);
        write(new File(plugin.getDataFolder(), "injuries.yml"), "# Our own note\n" + AfflictionFixtures.INJURIES_FILE);
        assertEquals(List.of(), plugin.reloadSettings());
        afflictions = plugin.getAfflictionService();
        console = server.getConsoleSender();
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        plugin.getStatService().createCharacter(character(alice));
        plugin.getStatService().createCharacter(character(bob));
        write(new File(chatTweaks, "config.yml"), CHATTWEAKS_CONFIG);
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        connector = settings -> dataSource.getConnection();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE skysongchat_profiles(id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " uuid VARCHAR(36), profile VARCHAR(20), permissions TEXT)");
        }
        row(alice.getUniqueId().toString(), "Default",
                "skysongchattweaks.injury.burn;skysongchattweaks.injury.cut;;other.node");
        row(bob.getUniqueId().toString(), "Default", "skysongchattweaks.injury.sprain");
        row(bob.getUniqueId().toString(), "Gone", "skysongchattweaks.injury.burn");
        row("not-a-uuid", "Default", "skysongchattweaks.injury.burn");
        row(alice.getUniqueId().toString(), "Alt", "");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ProfileKey character(PlayerMock player) {
        return new ProfileKey(player.getUniqueId(), "Default");
    }

    private static void write(File file, String text) throws IOException {
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }

    private void row(String uuid, String profile, String permissions) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO skysongchat_profiles(uuid, profile, permissions) VALUES (?, ?, ?)")) {
            statement.setString(1, uuid);
            statement.setString(2, profile);
            statement.setString(3, permissions);
            statement.executeUpdate();
        }
    }

    private ChatTweaksImporter.Targets targets() {
        return new ChatTweaksImporter.Targets(
                new File(plugin.getDataFolder(), "conditions.yml"), new File(plugin.getDataFolder(), "injuries.yml"),
                plugin::reloadSettings, afflictions, character -> plugin.getStatService().sheet(character).isPresent());
    }

    private ChatTweaksImporter importer(ChatTweaksImporter.Connector database) {
        return new ChatTweaksImporter(() -> chatTweaks, targets(), database,
                new ChatTweaksImporter.Threads(Runnable::run, Runnable::run), plugin::getMessages, plugin.getLogger(),
                DefinitionFile::isItem);
    }

    private List<String> consoleLines() {
        List<String> lines = new ArrayList<>();
        Component message;
        while ((message = console.nextComponentMessage()) != null) {
            lines.add(PlainTextComponentSerializer.plainText().serialize(message));
        }
        return lines;
    }

    private YamlConfiguration file(String name) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
    }

    @Test
    void addsTheConditionsAndInjuriesSkysongStatsDoesNotHave() {
        importer(connector).start(console);

        YamlConfiguration conditions = file("conditions.yml");
        assertEquals("<green>Poisoned", conditions.getString("conditions.poisoned.name"));
        assertEquals("Feels sick", conditions.getString("conditions.poisoned.description"));
        assertEquals("GREEN_WOOL", conditions.getString("conditions.poisoned.icon"));
        assertFalse(conditions.contains("conditions.poisoned.effects"));
        assertEquals("Bleeding", conditions.getString("conditions.bleeding.name"), "ours is kept");
        assertFalse(conditions.contains("conditions.Bad_Id"));

        YamlConfiguration injuries = file("injuries.yml");
        assertEquals("<gold>Burn", injuries.getString("injuries.burn.name"));
        assertEquals("moderate", injuries.getString("injuries.burn.severity"));
        assertEquals("ORANGE_WOOL", injuries.getString("injuries.burn.icon"));
        assertEquals("", injuries.getString("injuries.burn.message"));
        assertFalse(injuries.contains("injuries.burn.effects"));
        assertFalse(injuries.contains("injuries.sprain.icon"), "not an item, so the default icon");
        assertEquals("Cut", injuries.getString("injuries.cut.name"), "ours is kept");
        assertFalse(injuries.contains("injuries.nosev"));
        assertEquals(List.of("minor", "severe", "moderate"), injuries.getStringList("severities"));

        assertTrue(afflictions.injuryDefinitions().get("burn").isPresent(), "the files were reloaded");
        assertTrue(afflictions.conditionDefinitions().get("poisoned").isPresent());
    }

    @Test
    void keepsTheCommentsOfTheFiles() throws IOException {
        importer(connector).start(console);
        String injuries = Files.readString(new File(plugin.getDataFolder(), "injuries.yml").toPath());
        assertTrue(injuries.contains("# Our own note"), injuries);
    }

    @Test
    void teachesEachCharacterTheInjuriesItsNodesStandFor() {
        importer(connector).start(console);
        assertEquals(Set.of("burn", "cut"), afflictions.known(character(alice)));
        assertEquals(Set.of("sprain"), afflictions.known(character(bob)), "nodes match in any case");
        assertEquals(List.of(), afflictions.injuries(character(alice)), "known, not given");
    }

    @Test
    void reportsWhatWasImportedAndSkipped() {
        importer(connector).start(console);
        assertEquals(List.of(
                "[SkysongStats] Importing from " + new File(chatTweaks, "config.yml").getPath() + "...",
                "[SkysongStats] Imported from SkysongChatTweaks:",
                "Conditions: 1 added, 2 skipped",
                "Injuries: 2 added, 2 skipped",
                "Known injuries: 3 imported for 2 characters, 0 already known",
                "Skipped: 2 profiles SkysongStats doesn't have, 1 unreadable rows, 1 nodes that match no injury",
                "Every added and skipped entry is listed in the console."), consoleLines());
    }

    @Test
    void runningItAgainChangesNothing() throws IOException {
        ChatTweaksImporter importer = importer(connector);
        importer.start(console);
        String conditions = Files.readString(new File(plugin.getDataFolder(), "conditions.yml").toPath());
        String injuries = Files.readString(new File(plugin.getDataFolder(), "injuries.yml").toPath());
        consoleLines();

        importer.start(console);
        assertEquals(conditions, Files.readString(new File(plugin.getDataFolder(), "conditions.yml").toPath()));
        assertEquals(injuries, Files.readString(new File(plugin.getDataFolder(), "injuries.yml").toPath()));
        List<String> report = consoleLines();
        assertEquals("Conditions: 0 added, 3 skipped", report.get(2));
        assertEquals("Injuries: 0 added, 4 skipped", report.get(3));
        assertEquals("Known injuries: 0 imported for 0 characters, 3 already known", report.get(4));
        assertEquals(Set.of("burn", "cut"), afflictions.known(character(alice)));
    }

    @Test
    void aMissingFolderIsReported() {
        new ChatTweaksImporter(() -> new File(chatTweaks, "nowhere"), targets(), connector,
                new ChatTweaksImporter.Threads(Runnable::run, Runnable::run), plugin::getMessages, plugin.getLogger(),
                DefinitionFile::isItem).start(console);
        String path = new File(new File(chatTweaks, "nowhere"), "config.yml").getPath();
        assertEquals(List.of("[SkysongStats] There is no SkysongChatTweaks config at " + path
                + ". Set import.chattweaks-folder in config.yml."), consoleLines());
    }

    @Test
    void aDatabaseThatFailsStillKeepsTheDefinitions() {
        ChatTweaksImporter importer = importer(settings -> {
            throw new SQLException("Connection refused");
        });
        importer.start(console);
        List<String> lines = consoleLines();
        assertEquals("Injuries: 2 added, 2 skipped", lines.get(3));
        assertEquals("[SkysongStats] SkysongChatTweaks' database could not be read, so no known injuries were"
                + " imported. See the console for details.", lines.get(4));
        assertTrue(afflictions.injuryDefinitions().get("burn").isPresent());
        assertEquals(Set.of(), afflictions.known(character(alice)));
        assertFalse(importer.isRunning(), "a failed run lets the next one start");
    }

    @Test
    void aConfigWithoutDatabaseImportsOnlyTheDefinitions() throws IOException {
        write(new File(chatTweaks, "config.yml"), CHATTWEAKS_CONFIG.substring(CHATTWEAKS_CONFIG.indexOf("conditions:")));
        importer(connector).start(console);
        List<String> lines = consoleLines();
        assertTrue(lines.getLast().contains("has no database: section"), lines.getLast());
        assertEquals(Set.of(), afflictions.known(character(alice)));
    }

    @Test
    void aFileWithMistakesStopsBeforeTheKnownInjuries() throws IOException {
        // An injury of ours with a severity the list doesn't have: injuries.yml can't reload.
        write(new File(plugin.getDataFolder(), "injuries.yml"), AfflictionFixtures.INJURIES_FILE
                + "  odd: {name: \"Odd\", severity: weird}\n");
        importer(connector).start(console);
        List<String> lines = consoleLines();
        assertTrue(lines.getLast().startsWith("[SkysongStats] injuries.yml has mistakes after the import"),
                lines.getLast());
        assertEquals(Set.of(), afflictions.known(character(alice)));
    }

    @Test
    void anotherImportCannotStartWhileOneWaitsForTheDatabase() {
        List<Runnable> waiting = new ArrayList<>();
        ChatTweaksImporter.Targets targets = targets();
        ChatTweaksImporter importer = new ChatTweaksImporter(() -> chatTweaks, targets, connector,
                new ChatTweaksImporter.Threads(waiting::add, Runnable::run), plugin::getMessages, plugin.getLogger(),
                DefinitionFile::isItem);
        importer.start(console);
        consoleLines();
        assertTrue(importer.isRunning());

        importer.start(console);
        assertEquals(List.of("[SkysongStats] An import is already running, wait for its report."), consoleLines());

        waiting.getFirst().run();
        assertFalse(importer.isRunning());
        assertEquals(Set.of("burn", "cut"), afflictions.known(character(alice)));
    }

    @Test
    void mapsEveryNodeToEachInjuryListingIt() {
        List<ChatTweaksConfig.Injury> injuries = List.of(
                new ChatTweaksConfig.Injury("a", null, null, null, "minor", List.of("Node.One", "node.two")),
                new ChatTweaksConfig.Injury("b", null, null, null, "minor", List.of("node.one")));
        assertEquals(List.of("a", "b"), ChatTweaksImporter.injuriesByNode(injuries).get("node.one"));
        assertEquals(List.of("a"), ChatTweaksImporter.injuriesByNode(injuries).get("node.two"));
        assertNull(ChatTweaksImporter.injuriesByNode(injuries).get("node.three"));
    }

    @Test
    void readsTheNodesAsSkysongChatTweaksJoinedThem() {
        assertEquals(List.of("a.b", "c"), ChatTweaksProfiles.nodes("a.b; ;c;"));
        assertEquals(List.of(), ChatTweaksProfiles.nodes(""));
        assertEquals(List.of(), ChatTweaksProfiles.nodes(null));
    }
}
