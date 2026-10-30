package org.skysongdev.skysongstats.skill;

import org.skysongdev.skysongstats.character.SkillChange;
import org.skysongdev.skysongstats.character.SkillChangeListener;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillLevels;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Keeps a player's skill level nodes in step with their active character: on
// every XP change of that character, and through sync() when the player switches
// profile, joins, or skills.yml is reloaded. Changes to a character the player
// isn't playing leave the nodes alone; they apply when the player switches to it.
public final class SkillLevelPermissions implements SkillChangeListener {
    private final PlayerNodes nodes;
    private final Supplier<SkillsConfig> config;
    private final Predicate<ProfileKey> isActive;

    public SkillLevelPermissions(PlayerNodes nodes, Supplier<SkillsConfig> config, Predicate<ProfileKey> isActive) {
        this.nodes = Objects.requireNonNull(nodes, "nodes");
        this.config = Objects.requireNonNull(config, "config");
        this.isActive = Objects.requireNonNull(isActive, "isActive");
    }

    @Override
    public void onChange(SkillChange change) {
        if (!isActive.test(change.character())) {
            return;
        }
        SkillLevels levels = config.get().levels();
        Set<String> grant = SkillPermissions.reached(change.skill(), levels, change.after());
        nodes.update(change.character().player(), grant, revoked(change.skill(), levels, grant));
    }

    // Sets every skill's nodes to the character's levels.
    public void sync(ProfileKey character, SkillService skills) {
        SkillLevels levels = skills.config().levels();
        Set<String> grant = new LinkedHashSet<>();
        Set<String> revoke = new LinkedHashSet<>();
        for (SkillDefinition skill : skills.config().skills().all()) {
            Set<String> reached = SkillPermissions.reached(skill, levels, skills.xp(character, skill).orElse(0));
            grant.addAll(reached);
            revoke.addAll(revoked(skill, levels, reached));
        }
        nodes.update(character.player(), grant, revoke);
    }

    private static Set<String> revoked(SkillDefinition skill, SkillLevels levels, Set<String> granted) {
        Set<String> revoke = new LinkedHashSet<>(SkillPermissions.all(skill, levels));
        revoke.removeAll(granted);
        return revoke;
    }
}
