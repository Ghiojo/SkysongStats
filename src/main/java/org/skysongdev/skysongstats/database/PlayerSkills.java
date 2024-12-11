package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.Utils.StatsManager;
import org.skysongdev.skysongstats.Utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class PlayerSkills {
    private String uuid;
    private String profile;

    private final ArrayList<Integer> skills;

    public PlayerSkills(String uuid){
        this.uuid = uuid;
        this.profile = "Default";
        this.skills = new ArrayList<>();
        for(Utils.Skills s : Utils.Skills.values()){
            this.skills.add(0);
        }
    }
    public PlayerSkills(String uuid, String profile){
        this.uuid = uuid;
        this.profile = profile;
        this.skills = new ArrayList<>();
        for(Utils.Skills s : Utils.Skills.values()){
            this.skills.add(0);
        }
    }
    public PlayerSkills(String uuid, String profile, ArrayList<Integer> skills){
        this.uuid = uuid;
        this.profile = profile;
        this.skills = skills;
    }
    public String getUuid() {
        return uuid;
    }
    public String getProfile() {
        return profile;
    }

    public int getSkill(Utils.Skills index){
        return skills.get(index.getValue());
    }

    public void setSkill(Utils.Skills index, int value){
        this.skills.set(index.getValue(), value);
    }

    public void addSkill(Utils.Skills index, int value){
        int newval = value + skills.get(index.getValue());
        skills.set(index.getValue(), newval);
    }
}
