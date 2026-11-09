package org.skysongdev.skysongstats.apiimpl;

import org.skysongdev.skysongstats.api.ActiveCondition;
import org.skysongdev.skysongstats.api.ConditionType;
import org.skysongdev.skysongstats.api.Effect;
import org.skysongdev.skysongstats.api.EffectModifier;
import org.skysongdev.skysongstats.api.Injury;
import org.skysongdev.skysongstats.api.InjuryType;
import org.skysongdev.skysongstats.character.CharacterInjury;
import org.skysongdev.skysongstats.character.StatEffect;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.Icon;
import org.skysongdev.skysongstats.definition.InjuryDefinition;

import java.time.Instant;
import java.util.List;

// Turns the plugin's injury and condition records into the API's, like ApiTypes.
public final class AfflictionApiTypes {
    private AfflictionApiTypes() {
    }

    public static InjuryType injuryType(InjuryDefinition injury) {
        return new InjuryType(injury.id(), injury.name(), injury.description(), icon(injury.icon()), injury.severity(),
                injury.message(), modifiers(injury.modifiers()), injury.conditions());
    }

    public static ConditionType conditionType(ConditionDefinition condition) {
        return new ConditionType(condition.id(), condition.name(), condition.description(), icon(condition.icon()),
                modifiers(condition.modifiers()), condition.conditions());
    }

    public static Injury injury(CharacterInjury injury) {
        return new Injury(injury.injury().id(), Instant.ofEpochMilli(injury.awardedAt()));
    }

    public static ActiveCondition condition(org.skysongdev.skysongstats.character.ActiveCondition condition) {
        return new ActiveCondition(condition.id(), condition.level(), condition.direct(), condition.fromInjuries(),
                condition.fromConditions());
    }

    public static Effect effect(StatEffect effect) {
        return new Effect(effect.target(), effect.amount(), effect.source(), effect.sourceId());
    }

    // As written in the files: a Material name, or model:<namespace:id>.
    static String icon(Icon icon) {
        return icon.isModel() ? Icon.MODEL_PREFIX + icon.model() : icon.material();
    }

    private static List<EffectModifier> modifiers(List<org.skysongdev.skysongstats.definition.EffectModifier> modifiers) {
        return modifiers.stream()
                .map(modifier -> new EffectModifier(modifier.target(), modifier.amount(), modifier.perLevel()))
                .toList();
    }
}
