package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;

// Told about every change AfflictionService makes, after it is made and saved:
// the plugin turns them into messages and API events. Every method does nothing
// unless overridden.
public interface AfflictionChangeListener {
    AfflictionChangeListener NONE = new AfflictionChangeListener() { };

    default void onInjuryAwarded(InjuryAward award) {
        // Nothing to do unless overridden.
    }

    default void onInjuryRemoved(ProfileKey character, InjuryDefinition injury) {
        // Nothing to do unless overridden.
    }

    default void onInjuryLearnt(ProfileKey character, InjuryDefinition injury, LearnSource source) {
        // Nothing to do unless overridden.
    }

    // Called last for every change to a character's injuries or conditions, and for
    // every character after a reload, so the plugin can update what they do to stats.
    default void onAfflictionsChanged(ProfileKey character) {
        // Nothing to do unless overridden.
    }

    // Every condition that started, stopped or changed in one change, in
    // conditions.yml order.
    default void onConditionsChanged(ProfileKey character, List<ConditionChange> changes) {
        // Nothing to do unless overridden.
    }
}
