package org.skysongdev.skysongstats.apiimpl;

import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.DowntimeChange;
import org.skysongdev.skysongstats.api.DowntimeTarget;
import org.skysongdev.skysongstats.api.Focus;
import org.skysongdev.skysongstats.api.Proficiency;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkillChange;
import org.skysongdev.skysongstats.api.SkillLevel;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.StatType;
import org.skysongdev.skysongstats.api.Tier;
import org.skysongdev.skysongstats.api.TierChange;
import org.skysongdev.skysongstats.api.TrainingResult;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.character.Training;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.service.downtime.DowntimePool;
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

    public static Proficiency proficiency(ProficiencyDefinition proficiency) {
        return new Proficiency(proficiency.id(), proficiency.name(), proficiency.color(), proficiency.skill(),
                proficiency.aliases());
    }

    public static Tier tier(ProficiencyTier tier) {
        return new Tier(tier.id(), tier.name(), tier.color());
    }

    // Null stays null: the focus events use it for "no focus".
    public static Focus focus(ProficiencyFocus focus) {
        return focus == null ? null : new Focus(proficiency(focus.proficiency()), focus.points());
    }

    public static TierChange tierChange(ProficiencyTierChange change) {
        return new TierChange(character(change.character()), proficiency(change.proficiency()), tier(change.before()),
                tier(change.after()), change.tierUp());
    }

    static TrainingResult training(Training training) {
        return new TrainingResult(TrainingResult.Outcome.valueOf(training.outcome().name()),
                training.proficiency() == null ? null : proficiency(training.proficiency()),
                training.tier() == null ? null : tier(training.tier()), training.points());
    }

    public static DowntimeChange downtimeChange(org.skysongdev.skysongstats.service.downtime.DowntimeChange change) {
        DowntimeTarget target = change.pool() == DowntimePool.GLOBAL
                ? DowntimeTarget.global(change.player())
                : DowntimeTarget.character(new CharacterId(change.player(), change.profile()));
        return new DowntimeChange(target, DowntimeChange.Kind.valueOf(change.cause().name()), change.oldValue(),
                change.newValue());
    }
}
