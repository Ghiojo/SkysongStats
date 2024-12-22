package org.skysongdev.skysongstats.database;

public class CharacterProfile {
    private String uuid;
    private String profile;
    private String name;
    private String age;
    private String gender;
    private String ancestry;
    private String pronouns;
    private String description;

    public CharacterProfile(String uuid) {
        this.uuid = uuid;
        this.profile = "Default";
        this.name = "[Not Set]";
        this.age = "[Not Set]";
        this.gender = "[Not Set]";
        this.ancestry = "[Not Set]";
        this.pronouns = "[Not Set]";
        this.description = "[Not Set]";
    }

    public CharacterProfile(String uuid, String profile, String name, String age, String gender, String ancestry, String pronouns, String description) {
        this.uuid = uuid;
        this.profile = profile;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.ancestry = ancestry;
        this.pronouns = pronouns;
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAncestry() {
        return ancestry;
    }

    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
