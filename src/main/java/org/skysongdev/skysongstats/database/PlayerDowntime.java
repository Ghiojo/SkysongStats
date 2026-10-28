package org.skysongdev.skysongstats.database;

public class PlayerDowntime {
    private String uuid;
    private int currentDowntime;

    public PlayerDowntime(String uuid, int currentDowntime) {
        this.uuid = uuid;
        this.currentDowntime = currentDowntime;
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
