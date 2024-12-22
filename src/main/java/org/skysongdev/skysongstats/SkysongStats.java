package org.skysongdev.skysongstats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
import org.skysongdev.skysongstats.commands.hpManagement.AddHP;
import org.skysongdev.skysongstats.commands.hpManagement.DamageHP;
import org.skysongdev.skysongstats.commands.hpManagement.ReduceHP;
import org.skysongdev.skysongstats.commands.hpManagement.TempHp;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.commands.modifier.ModifierCommand;
import org.skysongdev.skysongstats.commands.profile.ProfileCommand;
import org.skysongdev.skysongstats.commands.setup.SetupCommand;
import org.skysongdev.skysongstats.commands.skills.SkillsCommand;
import org.skysongdev.skysongstats.commands.stats.StatsCommand;
import org.skysongdev.skysongstats.database.Database;
import org.skysongdev.skysongstats.listeners.PlayerRegisterListener;
import org.skysongdev.skysongstats.listeners.ProfileUpdateListener;
import org.skysongdev.skysongstats.listeners.SkillLevelListener;
import org.skysongdev.skysongstats.listeners.StatlineGUIListener;

import java.sql.SQLException;

public final class SkysongStats extends JavaPlugin {
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


    public Database getDatabase() { return database; }

    @Override
    public void onEnable() {
        plugin = this;
        utils = new Utils(plugin);

        saveDefaultConfig();
        InitializeDatabase();

        InitializeCommands();
        AssignListeners();

        super.onEnable();

        try {
            database.dumpDatabaseData();
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to dump stats into the plugin!");
            e.printStackTrace();
        }

    }

    public void InitializeCommands(){
        this.getCommand("heal").setExecutor(addHP);
        this.getCommand("addhp").setExecutor(addHP);
        this.getCommand("damage").setExecutor(damageHP);
        this.getCommand("reducehp").setExecutor(reduceHP);
        this.getCommand("rhp").setExecutor(reduceHP);
        this.getCommand("temphp").setExecutor(tempHp);

        this.getCommand("modifier").setExecutor(modifierCommand);
        this.getCommand("profile").setExecutor(profileCommand);
        this.getCommand("setup").setExecutor(setupCommand);
        this.getCommand("skills").setExecutor(skillsCommand);
        this.getCommand("stats").setExecutor(statsCommand);
        this.getCommand("ssreload").setExecutor(new ReloadCommand());
        this.getCommand("schar").setExecutor(characterCommand);

    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new ProfileUpdateListener(), this);
        getServer().getPluginManager().registerEvents(new SkillLevelListener(), this);
        getServer().getPluginManager().registerEvents(new StatlineGUIListener(), this);
        getServer().getPluginManager().registerEvents(new StatlineGUIListener(), this);
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
