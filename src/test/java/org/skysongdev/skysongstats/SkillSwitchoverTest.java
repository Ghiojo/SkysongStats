package org.skysongdev.skysongstats;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.StatSetup;
import org.skysongdev.skysongstats.placeholders.StatsExpansion;
import org.skysongdev.skysongstats.setup.SkillSetupMenu;
import org.skysongdev.skysongstats.setup.StatSetupMenu;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.SqlSkillRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin runs its skills on SkillService and skills.yml: /setup, profiles,
 * placeholders, generated permissions and /ssreload. Alice is an op with an
 * active "Default" profile. Tests have no MySQL, so the plugin keeps skills in
 * memory, as it does when the database is down.
 */
class SkillSwitchoverTest {
    private ServerMock server;
    private SkysongStats plugin;
    private SkillService skills;
    private PlayerMock alice;
    private ProfileKey character;
    private SetupProfile setup;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        skills = plugin.getSkillService();
        alice = server.addPlayer("Alice");
        alice.setOp(true);
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        setup = new SetupProfile(uuid, "Default", false);
        ProfileManager.setupProfiles = new ArrayList<>(List.of(setup));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(character);
        skills.createCharacter(character);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private SkillDefinition skill(String id) {
        return skills.config().skills().get(id).orElseThrow();
    }

    private int xp(ProfileKey key, String id) {
        return skills.xp(key, skill(id)).orElseThrow();
    }

    @Test
    void setupGivesThePickedSkillsTheNoviceXp() {
        alice.performCommand("setup");
        StatSetupMenu stats = assertInstanceOf(StatSetupMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
        // Spend the points: 4 on the first stat, 3 on the last.
        for (int i = 0; i < 4; i++) {
            stats.click(alice, 9, ClickType.LEFT);
        }
        for (int i = 0; i < 3; i++) {
            stats.click(alice, 17, ClickType.LEFT);
        }
        stats.click(alice, StatSetup.CONFIRM_SLOT, ClickType.LEFT);
        SkillSetupMenu menu = assertInstanceOf(SkillSetupMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
        // Animal Handling and Farming, in their skills.yml slots.
        menu.click(alice, 10, ClickType.LEFT);
        menu.click(alice, 11, ClickType.LEFT);
        menu.click(alice, SkillSetupMenu.CONFIRM_SLOT, ClickType.LEFT);

        assertEquals(16, xp(character, "animalhandling"));
        assertEquals(16, xp(character, "farming"));
        assertEquals(0, xp(character, "mining"));
        assertTrue(setup.isSetUp());
    }

    @Test
    void setupRunsOncePerProfile() {
        setup.setSetUp(true);
        ProfileManager.setupProfiles.add(new SetupProfile(alice.getUniqueId().toString(), "Second", false));

        alice.performCommand("setup");
        assertEquals("[SkysongStats] You have already set up your profile!",
                PlainTextComponentSerializer.plainText().serialize(alice.nextComponentMessage()));

        ProfileManager.activeProfiles.get(0).setProfile("Second");
        alice.performCommand("setup");
        assertInstanceOf(StatSetupMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void thePickedSkillsAreSavedToTheDatabase() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        SqlSkillRepository repository = new SqlSkillRepository(dataSource);
        repository.createTables();
        SkillService saved = new SkillService(skills.config(), repository, change -> { });
        saved.createCharacter(character);

        setup.applyPickedSkills(saved, List.of(skill("mining"), skill("cooking")));

        Map<String, Integer> stored = new SqlSkillRepository(dataSource).loadAll().get(character);
        assertEquals(16, stored.get("mining"));
        assertEquals(16, stored.get("cooking"));
        assertEquals(0, stored.get("farming"));
    }

    @Test
    void skillPlaceholdersGiveTheActiveCharactersXp() {
        skills.setXp(character, skill("tande"), 42);
        StatsExpansion expansion = new StatsExpansion(plugin);
        assertEquals("42", expansion.onPlaceholderRequest(alice, "tande"));
        assertEquals("0", expansion.onPlaceholderRequest(alice, "MINING"));
    }

    @Test
    void theLevelNodesComeFromSkillsYml() {
        assertNotNull(server.getPluginManager().getPermission("skysongstats.skill.mining.novice"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.skill.tande.expert"));
        assertNull(server.getPluginManager().getPermission("skysongstats.skill.mining.unskilled"));
    }

    @Test
    void reloadingSkillsYmlAppliesItsSkillsAndNodes() throws IOException {
        File file = new File(plugin.getDataFolder(), "skills.yml");
        Files.writeString(file.toPath(), Files.readString(file.toPath()).replace("aliases: [min]", "aliases: [dig]"));
        Files.writeString(file.toPath(), Files.readString(file.toPath()).replace("expert:     {name: \"Expert\"",
                "master:     {name: \"Master\""));

        new ReloadCommand().onCommand(server.getConsoleSender(), plugin.getCommand("ssreload"), "ssreload", new String[0]);

        server.dispatchCommand(server.getConsoleSender(), "skills setxp dig 30 Alice");
        assertEquals(30, xp(character, "mining"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.skill.mining.master"));
        assertNull(server.getPluginManager().getPermission("skysongstats.skill.mining.expert"));
    }

    @Test
    void aBrokenSkillsYmlIsReportedAndTheOldSkillsKept() throws IOException {
        File file = new File(plugin.getDataFolder(), "skills.yml");
        Files.writeString(file.toPath(), "levels: []\n");

        new ReloadCommand().onCommand(server.getConsoleSender(), plugin.getCommand("ssreload"), "ssreload", new String[0]);

        // The first line says the rest of the config was reloaded.
        server.getConsoleSender().nextComponentMessage();
        String report = PlainTextComponentSerializer.plainText().serialize(server.getConsoleSender().nextComponentMessage());
        assertTrue(report.contains("skills.yml"), report);
        assertTrue(skills.config().skills().get("mining").isPresent());
    }
}
