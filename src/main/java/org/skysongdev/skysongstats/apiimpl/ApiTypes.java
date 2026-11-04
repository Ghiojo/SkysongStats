package org.skysongdev.skysongstats.apiimpl;

import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkillChange;
import org.skysongdev.skysongstats.api.SkillLevel;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.StatType;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

// Turns the plugin's internal records into the API's, for the sub-APIs and for
// the events the plugin fires, so no internal type reaches another plugin.
public final class ApiTypes {
    private ApiTypes() {
    }

    public static CharacterId character(ProfileKey key) {
        return new CharacterId(key.player(), key.profile());
    }

    static ProfileKey key(CharacterId character) {
        return new ProfileKey(character.player(), character.profile());
    }

    public static Skill skill(SkillDefinition skill) {
        return new Skill(skill.id(), skill.name(), skill.shortName(), skill.color(), skill.aliases());
    }

    public static SkillLevel level(org.skysongdev.skysongstats.definition.SkillLevel level) {
        return new SkillLevel(level.id(), level.name(), level.color(), level.xp());
    }

    public static SkillChange skillChange(org.skysongdev.skysongstats.character.SkillChange change) {
        return new SkillChange(character(change.character()), skill(change.skill()), change.before(), change.after());
    }

    public static Stat stat(StatDefinition stat) {
        return new Stat(stat.id(), stat.display().name(), stat.display().color(), stat.display().aliases(),
                stat.isBar() ? StatType.BAR : StatType.VALUE, stat.min(), stat.isBar() ? stat.bar().shield() : null);
    }

    public static StatChange statChange(org.skysongdev.skysongstats.character.StatChange change) {
        return new StatChange(character(change.character()), stat(change.stat()), change.oldValue(),
                change.newValue());
    }

    static org.skysongdev.skysongstats.definition.StatAction action(StatAction action) {
        return org.skysongdev.skysongstats.definition.StatAction.valueOf(action.name());
    }
}
