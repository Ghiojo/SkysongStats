package org.skysongdev.skysongstats.skill;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Sets a player's skill level nodes to their active character's levels when they
// join and when they switch profile. The plugin also calls sync() for everyone
// online after /ssreload, as skills.yml may have changed the levels.
public final class SkillLevelSync implements Listener {
    private final SkillLevelPermissions permissions;
    private final SkillService skills;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;

    public SkillLevelSync(SkillLevelPermissions permissions, SkillService skills,
                          Function<Player, Optional<ProfileKey>> activeCharacter) {
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.skills = Objects.requireNonNull(skills, "skills");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
    }

    // After the listener that gives a new player their first character.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileSwitch(ProfileUpdateEvent event) {
        sync(event.getPlayer());
    }

    public void sync(Player player) {
        activeCharacter.apply(player).filter(skills::hasCharacter)
                .ifPresent(character -> permissions.sync(character, skills));
    }
}
