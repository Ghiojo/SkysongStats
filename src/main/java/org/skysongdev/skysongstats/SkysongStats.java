package org.skysongdev.skysongstats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongitems.SkysongItems;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.commands.afflictions.PotionSickness;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
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
import org.skysongdev.skysongstats.placeholders.StatsExpansion;

import java.sql.SQLException;

public final class SkysongStats extends JavaPlugin {
    private static SkysongStats plugin;
    private SkysongItems skySongItems;
    public static SkysongStats getPlugin(){
        if(plugin == null){
            plugin = new SkysongStats();
        }
        return plugin;
    }
    public SkysongItems getSkySongItems(){return skySongItems;}
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
        //getCommand("fictis").setExecutor(new Fictis());
        //getCommand("arcrot").setExecutor(new Arcrot());
    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new ProfileUpdateListener(), this);
        getServer().getPluginManager().registerEvents(new SkillLevelListener(), this);
        getServer().getPluginManager().registerEvents(new StatlineGUIListener(), this);
        getServer().getPluginManager().registerEvents(new StatPointsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new SkillAllocGUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShiftRightClickListener(), this);
        getServer().getPluginManager().registerEvents(new AfflictionsListener(), this);
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
