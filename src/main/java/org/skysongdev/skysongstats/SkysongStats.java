package org.skysongdev.skysongstats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.Database;

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

    public Database getDatabase() { return database; }

    @Override
    public void onEnable() {
        plugin = this;
        utils = new Utils(plugin);

        saveDefaultConfig();
        InitializeDatabase();
        super.onEnable();

        try {
            database.dumpDatabaseData();
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to dump stats into the plugin!");
            e.printStackTrace();
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
