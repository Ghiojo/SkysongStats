package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class Pinger implements Runnable{
    @Override
    public void run() {
        try{
            getPlugin().getDatabase().Ping();
            Bukkit.getLogger().info("[Skysong Stats] Database pinged!");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
