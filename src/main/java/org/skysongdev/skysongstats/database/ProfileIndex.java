package org.skysongdev.skysongstats.database;

public class ProfileIndex {
    private String uuid;
    private String profile;

    public String getUuid() {
        return uuid;
    }

    public String getProfile() {
        return profile;
    }

    public ProfileIndex(String uuid, String profile){
        this.uuid = uuid;
        this.profile = profile;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
