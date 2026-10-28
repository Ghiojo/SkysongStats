package org.skysongdev.skysongstats.database;

public class CharacterDowntime {
    String profile;
    String uuid;
    int currentDowntime;

    public CharacterDowntime(String profile, String uuid, int currentDowntime) {
        this.profile = profile;
        this.uuid = uuid;
        this.currentDowntime = currentDowntime;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCurrentDowntime() {
        return currentDowntime;
    }

    public void setCurrentDowntime(int currentDowntime) {
        this.currentDowntime = currentDowntime;
    }
}
