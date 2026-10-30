package org.skysongdev.skysongstats;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.character.SkillChange;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
import org.skysongdev.skysongstats.commands.downtime.DowntimeCommand;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.commands.profile.ProfileCommand;
import org.skysongdev.skysongstats.commands.setup.SetupCommand;
import org.skysongdev.skysongstats.commands.skills.SkillBook;
import org.skysongdev.skysongstats.commands.skills.SkillsCommand;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.StatActionRunner;
import org.skysongdev.skysongstats.commands.stat.StatAliasCommand;
import org.skysongdev.skysongstats.commands.stat.StatCommandRegistry;
import org.skysongdev.skysongstats.commands.stat.StatModifierCommand;
import org.skysongdev.skysongstats.commands.stat.StatPermissionRegistry;
import org.skysongdev.skysongstats.commands.stat.StatSheet;
import org.skysongdev.skysongstats.commands.stat.StatsRootCommand;
import org.skysongdev.skysongstats.database.Database;
import org.skysongdev.skysongstats.database.Pinger;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.DefinitionException;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigFile;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.definition.StatsConfigFile;
import org.skysongdev.skysongstats.items.ItemTagListener;
import org.skysongdev.skysongstats.listeners.*;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.MessagesFile;
import org.skysongdev.skysongstats.messages.PluginTexts;
import org.skysongdev.skysongstats.events.DowntimeChangeEvent;
import org.skysongdev.skysongstats.events.SkillChangeEvent;
import org.skysongdev.skysongstats.events.StatChangeEvent;
import org.skysongdev.skysongstats.placeholders.StatsExpansion;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.DowntimeStorageException;
import org.skysongdev.skysongstats.service.downtime.SqlDowntimeRepository;
import org.skysongdev.skysongstats.setup.SetupScreenListener;
import org.skysongdev.skysongstats.skill.LuckPermsNodes;
import org.skysongdev.skysongstats.skill.PlayerNodes;
import org.skysongdev.skysongstats.skill.SkillLevelPermissions;
import org.skysongdev.skysongstats.skill.SkillLevelSync;
import org.skysongdev.skysongstats.skill.SkillPermissionRegistry;
import org.skysongdev.skysongstats.storage.CharacterStorage;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.SkillRepository;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StatStorage;
import org.skysongdev.skysongstats.storage.StorageException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

public class SkysongStats extends JavaPlugin {
    private static SkysongStats plugin;
    public static SkysongStats getPlugin(){
        if(plugin == null){
            plugin = new SkysongStats();
        }
        return plugin;
    }
    private Utils utils;
    public Utils getUtils() { return utils; }
    private Database database;
    private DowntimeService downtimeService;
    private Messages messages;
    private StatsConfig statsConfig;
    private CharacterStorage storage;
    private StatService statService;
    private SkillService skillService;
    private SkillLevelPermissions skillLevels;
    private SkillLevelSync skillLevelSync;
    private SkillPermissionRegistry skillPermissions;

    // messages.yml for the stat commands and the /setup screen.
    private final PluginTexts texts = new PluginTexts(this::getMessages);
    private StatCommandRegistry statCommands;
    private StatPermissionRegistry statPermissions;

    public ProfileCommand profileCommand = new ProfileCommand();
    public SetupCommand setupCommand = new SetupCommand(texts);
    public CharacterCommand characterCommand = new CharacterCommand();
    public DowntimeCommand downtimeCommand;


    public Database getDatabase() { return database; }
    public DowntimeService getDowntimeService() { return downtimeService; }
    public Messages getMessages() { return messages; }
    public StatsConfig getStatsConfig() { return statsConfig; }
    public StatService getStatService() { return statService; }
    public SkillService getSkillService() { return skillService; }

    @Override
    public void onEnable() {
        plugin = this;
        utils = new Utils(plugin);

        saveDefaultConfig();
        loadMessages();
        try {
            statsConfig = loadStatsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix stats.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SkillsConfig skillsConfig;
        try {
            skillsConfig = loadSkillsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix skills.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!InitializeDatabase()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        InitializeDowntime();
        // The stats come before the commands, listeners and placeholders that use them.
        InitializeStats();
        InitializeSkills(skillsConfig);

        InitializeCommands();
        AssignListeners();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new StatsExpansion(this).register();
        }


        super.onEnable();

        Bukkit.getScheduler().runTaskTimer(this, new Pinger(), 0L, 6000L);

    }

    public void InitializeCommands(){
        Supplier<List<String>> onlinePlayers = () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        CommandTargets targets = new CommandTargets(statService, utils.getProfileManager()::activeCharacter,
                Bukkit::getPlayer, texts);
        StatActionRunner runner = new StatActionRunner(statService, targets, texts);
        StatsRootCommand statsCommand = new StatsRootCommand(statService, runner, targets,
                new StatSheet(statService, texts), texts, onlinePlayers);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("sstats").setExecutor(statsCommand);
        getCommand("modifier").setExecutor(new StatModifierCommand(statService, targets, texts, onlinePlayers));
        // The per-stat names from stats.yml (/heal, /damage, /temphp...).
        statCommands = new StatCommandRegistry(getServer().getCommandMap(), getName().toLowerCase(Locale.ROOT),
                label -> new StatAliasCommand(label, statService, runner, texts, onlinePlayers));
        statPermissions = new StatPermissionRegistry(getServer().getPluginManager());
        registerStatCommands();

        getCommand("profile").setExecutor(profileCommand);
        getCommand("setup").setExecutor(setupCommand);
        getCommand("skills").setExecutor(new SkillsCommand(skillService, targets, texts, new SkillBook(this::getMessages),
                onlinePlayers, getLogger()::info));
        getCommand("ssreload").setExecutor(new ReloadCommand());
        getCommand("schar").setExecutor(characterCommand);
        getCommand("char").setExecutor(characterCommand);
        downtimeCommand = new DowntimeCommand(downtimeService);
        getCommand("downtime").setExecutor(downtimeCommand);
    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new SetupScreenListener(), this);
        getServer().getPluginManager().registerEvents(skillLevelSync, this);
        getServer().getPluginManager().registerEvents(new PlayerShiftRightClickListener(), this);
        getServer().getPluginManager().registerEvents(
                new ItemTagListener(statService, utils.getProfileManager()::activeCharacter), this);
    }

    public void InitializeDowntime(){
        downtimeService = new DowntimeService(
                new SqlDowntimeRepository(() -> database.getConnection()),
                DowntimeSettings.fromConfig(getConfig().getConfigurationSection("downtime")),
                change -> Bukkit.getPluginManager().callEvent(new DowntimeChangeEvent(change)));
        try {
            downtimeService.load();
        } catch (DowntimeStorageException e) {
            Bukkit.getLogger().warning("[SkysongStats] Unable to load downtime from the database!");
            e.printStackTrace();
        }
    }

    // Registers the command names and permissions of the current stats.yml, and
    // updates the command lists players see.
    private void registerStatCommands(){
        List<String> taken = statCommands.register(statService.definitions());
        taken.forEach(label -> getLogger().log(Level.WARNING,
                "Another plugin already has /{0}, use /skysongstats:{0} for that stat command", label));
        statPermissions.register(statService.definitions());
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
    }

    @Override
    public void onDisable() {
        if (statCommands != null) {
            statCommands.unregisterAll();
            statPermissions.unregisterAll();
        }
        if (skillPermissions != null) {
            skillPermissions.unregisterAll();
        }
        if (storage != null) {
            storage.close();
        }
    }

    // Opens the stat tables and loads every character's stats. Profiles made
    // before the stat tables existed get their stats at the stats.yml start values.
    public void InitializeStats(){
        StatRepository repository = storage.repository();
        statService = new StatService(statsConfig.stats(), repository,
                change -> Bukkit.getPluginManager().callEvent(new StatChangeEvent(change)));
        try {
            repository.createTables();
            statService.load();
        } catch (StorageException e) {
            // Creating sheets now could overwrite the saved stats once MySQL is back.
            getLogger().log(Level.SEVERE, "Unable to load the stats from the database", e);
            return;
        }
        addStatsToOlderProfiles();
    }

    // Every profile has a row in skysong_setup. That table is null when the
    // database could not be read, and then there is nothing to add.
    private void addStatsToOlderProfiles(){
        if (ProfileManager.setupProfiles == null) {
            return;
        }
        for (SetupProfile profile : ProfileManager.setupProfiles) {
            statService.ensureCharacter(new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile()));
        }
    }

    // Loads every character's skill XP from the stat database, and keeps the
    // LuckPerms skill level nodes of active characters up to date.
    public void InitializeSkills(SkillsConfig config){
        SkillRepository repository = storage.skillRepository();
        skillService = new SkillService(config, repository, this::onSkillChange);
        skillLevels = new SkillLevelPermissions(playerNodes(), skillService::config, utils.getProfileManager()::isActive);
        skillLevelSync = new SkillLevelSync(skillLevels, skillService, utils.getProfileManager()::activeCharacter);
        skillPermissions = new SkillPermissionRegistry(getServer().getPluginManager());
        skillPermissions.register(config);
        try {
            repository.createTables();
            skillService.load();
        } catch (StorageException e) {
            // Creating characters now could overwrite the saved XP once MySQL is back.
            getLogger().log(Level.SEVERE, "Unable to load the skills from the database", e);
            return;
        }
        addSkillsToOlderProfiles();
    }

    private void onSkillChange(SkillChange change){
        skillLevels.onChange(change);
        Bukkit.getPluginManager().callEvent(new SkillChangeEvent(change));
    }

    // LuckPerms is a hard dependency, so it is only missing on a test server.
    private PlayerNodes playerNodes(){
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            getLogger().severe("LuckPerms is not running, skill level nodes will not be given");
            return (player, grant, revoke) -> { };
        }
        return new LuckPermsNodes(luckPerms, failure -> getLogger().log(Level.WARNING,
                "Unable to update a player's skill level nodes in LuckPerms", failure));
    }

    private void addSkillsToOlderProfiles(){
        if (ProfileManager.setupProfiles == null) {
            return;
        }
        for (SetupProfile profile : ProfileManager.setupProfiles) {
            skillService.ensureCharacter(new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile()));
        }
    }

    /**
     * Re-reads config.yml and applies every setting that can change while the
     * server runs. The database connection is only read on startup.
     */
    // Returns the files that had mistakes and were not reloaded, so /ssreload can
    // tell whoever ran it; the details are in the console.
    public List<String> reloadSettings(){
        reloadConfig();
        downtimeService.setSettings(DowntimeSettings.fromConfig(getConfig().getConfigurationSection("downtime")));
        loadMessages();
        List<String> notReloaded = new ArrayList<>();
        if (!reloadStatsConfig()) {
            notReloaded.add(StatsConfigFile.FILE_NAME);
        }
        if (!reloadSkillsConfig()) {
            notReloaded.add(SkillsConfigFile.FILE_NAME);
        }
        return notReloaded;
    }

    // A stats.yml with mistakes is reported and the stats already loaded are kept.
    private boolean reloadStatsConfig(){
        try {
            statsConfig = loadStatsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "stats.yml was not reloaded, keeping the stats from before", e);
            return false;
        }
        if (statService != null) {
            statService.setDefinitions(statsConfig.stats());
            registerStatCommands();
        }
        return true;
    }

    // A skills.yml with mistakes is reported and the skills already loaded are
    // kept. Otherwise everyone online gets the level nodes of the new file.
    private boolean reloadSkillsConfig(){
        SkillsConfig config;
        try {
            config = loadSkillsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "skills.yml was not reloaded, keeping the skills from before", e);
            return false;
        }
        if (skillService != null) {
            skillService.setConfig(config);
            skillPermissions.register(config);
            Bukkit.getOnlinePlayers().forEach(skillLevelSync::sync);
        }
        return true;
    }

    private SkillsConfig loadSkillsConfig(){
        return SkillsConfigFile.load(new File(getDataFolder(), SkillsConfigFile.FILE_NAME), bundled(SkillsConfigFile.FILE_NAME));
    }

    private StatsConfig loadStatsConfig(){
        return StatsConfigFile.load(new File(getDataFolder(), StatsConfigFile.FILE_NAME), bundled(StatsConfigFile.FILE_NAME));
    }

    private void loadMessages(){
        messages = MessagesFile.load(new File(getDataFolder(), MessagesFile.FILE_NAME), bundled(MessagesFile.FILE_NAME), getLogger()::warning);
    }

    private String bundled(String fileName){
        try (InputStream in = getResource(fileName)) {
            if (in == null) {
                throw new IllegalStateException(fileName + " is missing from the plugin jar");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The plugin can't work without MySQL, so this returns false, with one error
    // that says what to fix, when it can't be reached or read.
    public boolean InitializeDatabase(){
        DatabaseSettings settings = DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database"));
        this.database = new Database(
                getConfig().getString("database.host"),
                getConfig().getString("database.port"),
                getConfig().getString("database.user"),
                getConfig().getString("database.password"),
                getConfig().getString("database.database_name"));
        try {
            storage = openDatabase(settings);
            return true;
        } catch (SQLException | RuntimeException e) {
            getLogger().log(Level.SEVERE, "Unable to use the database " + settings
                    + ". SkysongStats needs it, so it is disabled. Check that MySQL is running and the"
                    + " database: section of config.yml, then restart the server.", e);
            return false;
        }
    }

    // Opens the stat and skill tables, and loads the profiles and characters of
    // the older tables. Tests replace it, since they have no MySQL.
    protected CharacterStorage openDatabase(DatabaseSettings settings) throws SQLException {
        CharacterStorage opened = StatStorage.open(settings, getLogger()::warning);
        try {
            database.initializeDatabase();
            database.dumpActiveStatsDatabase();
            database.dumpDatabaseData();
        } catch (SQLException e) {
            opened.close();
            throw e;
        }
        return opened;
    }
}
