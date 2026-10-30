package org.skysongdev.skysongstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
import org.skysongdev.skysongstats.commands.downtime.DowntimeCommand;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.commands.profile.ProfileCommand;
import org.skysongdev.skysongstats.commands.setup.SetupCommand;
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
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.definition.StatsConfigFile;
import org.skysongdev.skysongstats.items.ItemTagListener;
import org.skysongdev.skysongstats.listeners.*;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.MessagesFile;
import org.skysongdev.skysongstats.messages.PluginTexts;
import org.skysongdev.skysongstats.events.DowntimeChangeEvent;
import org.skysongdev.skysongstats.events.StatChangeEvent;
import org.skysongdev.skysongstats.placeholders.StatsExpansion;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.DowntimeStorageException;
import org.skysongdev.skysongstats.service.downtime.SqlDowntimeRepository;
import org.skysongdev.skysongstats.setup.SetupScreenListener;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StatStorage;
import org.skysongdev.skysongstats.storage.StorageException;
import org.skysongdev.skysongstats.storage.UnsavedStatRepository;

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
    private StatStorage statStorage;
    private StatService statService;

    // messages.yml for the stat commands and the /setup screen.
    private final PluginTexts texts = new PluginTexts(this::getMessages);
    private StatCommandRegistry statCommands;
    private StatPermissionRegistry statPermissions;

    public ProfileCommand profileCommand = new ProfileCommand();
    public SetupCommand setupCommand = new SetupCommand(texts);
    public SkillsCommand skillsCommand = new SkillsCommand();
    public CharacterCommand characterCommand = new CharacterCommand();
    public DowntimeCommand downtimeCommand;


    public Database getDatabase() { return database; }
    public DowntimeService getDowntimeService() { return downtimeService; }
    public Messages getMessages() { return messages; }
    public StatsConfig getStatsConfig() { return statsConfig; }
    public StatService getStatService() { return statService; }

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
        InitializeDatabase();
        try {
            database.dumpDatabaseData();
        }catch (SQLException e){
            getLogger().log(Level.WARNING, "Unable to load the profiles, skills and characters from the database", e);
        }
        InitializeDowntime();
        // The stats come before the commands, listeners and placeholders that use them.
        InitializeStats();

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
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("ssreload").setExecutor(new ReloadCommand());
        getCommand("schar").setExecutor(characterCommand);
        getCommand("char").setExecutor(characterCommand);
        downtimeCommand = new DowntimeCommand(downtimeService);
        getCommand("downtime").setExecutor(downtimeCommand);
        //getCommand("fictis").setExecutor(new Fictis());
        //getCommand("arcrot").setExecutor(new Arcrot());
    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new ProfileUpdateListener(), this);
        getServer().getPluginManager().registerEvents(new SkillLevelListener(), this);
        getServer().getPluginManager().registerEvents(new SetupScreenListener(), this);
        getServer().getPluginManager().registerEvents(new SkillAllocGUIListener(), this);
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
        if (statStorage != null) {
            statStorage.close();
        }
    }

    // Opens the stat tables and loads every character's stats. Profiles made
    // before the stat tables existed get their stats at the stats.yml start values.
    public void InitializeStats(){
        StatRepository repository = openStatStorage();
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

    private StatRepository openStatStorage(){
        try {
            statStorage = StatStorage.open(DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database")),
                    getLogger()::warning);
            return statStorage.repository();
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, "Unable to open the stat tables, stat changes will not be saved", e);
            return new UnsavedStatRepository(getLogger()::warning);
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

    public void InitializeDatabase(){
        try{
            this.database = new Database(
                    getConfig().getString("database.host"),
                    getConfig().getString("database.port"),
                    getConfig().getString("database.user"),
                    getConfig().getString("database.password"),
                    getConfig().getString("database.database_name"));
            database.initializeDatabase();
            database.dumpActiveStatsDatabase();
        }catch(SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to connect to database and create tables!");
            e.printStackTrace();
        }
    }
}
