package org.skysongdev.skysongstats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.commands.afflictions.PotionSickness;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
import org.skysongdev.skysongstats.commands.downtime.DowntimeCommand;
import org.skysongdev.skysongstats.commands.hpManagement.*;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.commands.modifier.ModifierCommand;
import org.skysongdev.skysongstats.commands.profile.ProfileCommand;
import org.skysongdev.skysongstats.commands.setup.SetupCommand;
import org.skysongdev.skysongstats.commands.skills.SkillsCommand;
import org.skysongdev.skysongstats.commands.stats.StatsCommand;
import org.skysongdev.skysongstats.database.Database;
import org.skysongdev.skysongstats.database.Pinger;
import org.skysongdev.skysongstats.listeners.*;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.MessagesFile;
import org.skysongdev.skysongstats.events.DowntimeChangeEvent;
import org.skysongdev.skysongstats.placeholders.StatsExpansion;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.DowntimeStorageException;
import org.skysongdev.skysongstats.service.downtime.SqlDowntimeRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

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

    public AddHP addHP = new AddHP();
    public DamageHP damageHP = new DamageHP();
    public ReduceHP reduceHP = new ReduceHP();
    public TempHp tempHp = new TempHp();

    public ModifierCommand modifierCommand = new ModifierCommand();
    public ProfileCommand profileCommand = new ProfileCommand();
    public SetupCommand setupCommand = new SetupCommand();
    public SkillsCommand skillsCommand = new SkillsCommand();
    public StatsCommand statsCommand = new StatsCommand();
    public CharacterCommand characterCommand = new CharacterCommand();
    public DowntimeCommand downtimeCommand;


    public Database getDatabase() { return database; }
    public DowntimeService getDowntimeService() { return downtimeService; }
    public Messages getMessages() { return messages; }

    @Override
    public void onEnable() {
        plugin = this;
        utils = new Utils(plugin);

        saveDefaultConfig();
        loadMessages();
        InitializeDatabase();
        InitializeDowntime();

        InitializeCommands();
        AssignListeners();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new StatsExpansion(this).register();
        }


        super.onEnable();

        try {
            database.dumpDatabaseData();
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to dump stats into the plugin!");
            e.printStackTrace();
        }
        Bukkit.getScheduler().runTaskTimer(this, new Pinger(), 0L, 6000L);

    }

    public void InitializeCommands(){
        getCommand("heal").setExecutor(addHP);
        getCommand("addhp").setExecutor(addHP);
        getCommand("damage").setExecutor(damageHP);
        getCommand("reducehp").setExecutor(reduceHP);
        getCommand("rhp").setExecutor(reduceHP);
        getCommand("dhp").setExecutor(damageHP);
        getCommand("temphp").setExecutor(tempHp);

        getCommand("modifier").setExecutor(modifierCommand);
        getCommand("profile").setExecutor(profileCommand);
        getCommand("setup").setExecutor(setupCommand);
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("sstats").setExecutor(statsCommand);
        getCommand("ssreload").setExecutor(new ReloadCommand());
        getCommand("schar").setExecutor(characterCommand);
        getCommand("char").setExecutor(characterCommand);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("ac").setExecutor(new ACCommand());
        getCommand("potionsickness").setExecutor(new PotionSickness());
        downtimeCommand = new DowntimeCommand(downtimeService);
        getCommand("downtime").setExecutor(downtimeCommand);
        //getCommand("fictis").setExecutor(new Fictis());
        //getCommand("arcrot").setExecutor(new Arcrot());
    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new ProfileUpdateListener(), this);
        getServer().getPluginManager().registerEvents(new SkillLevelListener(), this);
        getServer().getPluginManager().registerEvents(new StatPointsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new SkillAllocGUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShiftRightClickListener(), this);
        getServer().getPluginManager().registerEvents(new AfflictionsListener(), this);
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

    /**
     * Re-reads config.yml and applies every setting that can change while the
     * server runs. The database connection is only read on startup.
     */
    public void reloadSettings(){
        reloadConfig();
        downtimeService.setSettings(DowntimeSettings.fromConfig(getConfig().getConfigurationSection("downtime")));
        loadMessages();
    }

    private void loadMessages(){
        messages = MessagesFile.load(new File(getDataFolder(), MessagesFile.FILE_NAME), bundledMessages(), getLogger()::warning);
    }

    private String bundledMessages(){
        try (InputStream in = getResource(MessagesFile.FILE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(MessagesFile.FILE_NAME + " is missing from the plugin jar");
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
