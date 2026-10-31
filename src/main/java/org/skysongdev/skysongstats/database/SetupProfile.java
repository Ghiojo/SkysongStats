package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.ProficiencyTiers;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupProfile {
    private String uuid;
    private String profile;
    private boolean isSetUp;

    // The stat values picked on the /setup point screen, by stat id.
    private Map<String, Integer> chosenStats = Map.of();

    public Map<String, Integer> getChosenStats() {
        return chosenStats;
    }

    public void setChosenStats(Map<String, Integer> chosenStats) {
        this.chosenStats = Map.copyOf(chosenStats);
    }

    public String getUuid() {
        return uuid;
    }

    public String getProfile() {
        return profile;
    }

    public boolean isSetUp() {
        return isSetUp;
    }
    public void setSetUp(boolean setUp) {
        isSetUp = setUp;
    }
    public SetupProfile(String uuid){
        this.uuid = uuid;
        this.profile = "Default";
        this.isSetUp = false;
    }

    public SetupProfile(String uuid, String profile, boolean isSetUp) {
        this.uuid = uuid;
        this.profile = profile;
        this.isSetUp = isSetUp;
    }

    // Gives the character the stats picked on the point screen, the skills picked
    // on the skill screen and the proficiencies from the proficiency screen, and
    // marks the profile as set up.
    public void finishProfileSetup(StatService stats, SkillService skills, List<SkillDefinition> pickedSkills,
                                   ProficiencyService proficiencies, List<ProficiencyDefinition> startingProficiencies){
        applyChosenStats(stats);
        applyPickedSkills(skills, pickedSkills);
        applyStartingProficiencies(proficiencies, startingProficiencies);

        this.isSetUp = true;
        try{
        getPlugin().getDatabase().updateSetupData(uuid, profile, true);
        } catch (Exception e){
            Bukkit.getLogger().warning("[Skysong Stats] Failed to update player setup data!");
        }
    }

    // Sets the character's stats to the values picked on the point screen, then
    // fills the bars that start full (HP) to their new max. A stat that left
    // stats.yml since then is skipped.
    public void applyChosenStats(StatService stats){
        ProfileKey character = new ProfileKey(UUID.fromString(uuid), profile);
        chosenStats.forEach((id, value) -> stats.definitions().get(id)
                .ifPresent(stat -> stats.apply(character, stat, StatAction.SET, value)));
        stats.fillBarsThatStartFull(character);
    }

    // The trained and picked proficiencies start at the tier after the first
    // (Proficient), never higher. The tier events and LuckPerms nodes follow, as
    // for any tier change.
    public void applyStartingProficiencies(ProficiencyService proficiencies, List<ProficiencyDefinition> starting){
        ProfileKey character = new ProfileKey(UUID.fromString(uuid), profile);
        ProficiencyTiers tiers = proficiencies.config().tiers();
        ProficiencyTier tier = tiers.next(tiers.first()).orElseThrow();
        starting.forEach(proficiency -> proficiencies.setTier(character, proficiency, tier));
    }

    // Each picked skill starts at the XP of the setup: picked-level of skills.yml.
    public void applyPickedSkills(SkillService skills, List<SkillDefinition> pickedSkills){
        ProfileKey character = new ProfileKey(UUID.fromString(uuid), profile);
        int xp = skills.config().setup().pickedLevel().xp();
        pickedSkills.forEach(skill -> skills.setXp(character, skill, xp));
    }
}
