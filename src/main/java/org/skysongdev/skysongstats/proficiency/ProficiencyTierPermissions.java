package org.skysongdev.skysongstats.proficiency;

import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.ProficiencyTiers;
import org.skysongdev.skysongstats.skill.PlayerNodes;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Keeps a player's proficiency tier nodes in step with their active character:
// on every tier change of that character, and through sync() when the player
// switches profile, joins, or proficiencies.yml is reloaded. Changes to a
// character the player isn't playing leave the nodes alone; they apply when the
// player switches to it. The same as SkillLevelPermissions for skill levels.
public final class ProficiencyTierPermissions {
    private final PlayerNodes nodes;
    private final Supplier<ProficienciesConfig> config;
    private final Predicate<ProfileKey> isActive;

    public ProficiencyTierPermissions(PlayerNodes nodes, Supplier<ProficienciesConfig> config,
                                      Predicate<ProfileKey> isActive) {
        this.nodes = Objects.requireNonNull(nodes, "nodes");
        this.config = Objects.requireNonNull(config, "config");
        this.isActive = Objects.requireNonNull(isActive, "isActive");
    }

    public void onTierChange(ProficiencyTierChange change) {
        if (!isActive.test(change.character())) {
            return;
        }
        ProficiencyTiers tiers = config.get().tiers();
        Set<String> grant = ProficiencyPermissions.reached(change.proficiency(), tiers, change.after());
        nodes.update(change.character().player(), grant, revoked(change.proficiency(), tiers, grant));
    }

    // Sets every proficiency's nodes to the character's tiers.
    public void sync(ProfileKey character, ProficiencyService proficiencies) {
        ProficiencyTiers tiers = proficiencies.config().tiers();
        Set<String> grant = new LinkedHashSet<>();
        Set<String> revoke = new LinkedHashSet<>();
        for (ProficiencyDefinition proficiency : proficiencies.config().proficiencies().all()) {
            ProficiencyTier tier = proficiencies.tier(character, proficiency);
            Set<String> reached = ProficiencyPermissions.reached(proficiency, tiers, tier);
            grant.addAll(reached);
            revoke.addAll(revoked(proficiency, tiers, reached));
        }
        nodes.update(character.player(), grant, revoke);
    }

    private static Set<String> revoked(ProficiencyDefinition proficiency, ProficiencyTiers tiers, Set<String> granted) {
        Set<String> revoke = new LinkedHashSet<>(ProficiencyPermissions.all(proficiency, tiers));
        revoke.removeAll(granted);
        return revoke;
    }
}
