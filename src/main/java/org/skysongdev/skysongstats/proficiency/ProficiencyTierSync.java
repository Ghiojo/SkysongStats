package org.skysongdev.skysongstats.proficiency;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Sets a player's proficiency tier nodes to their active character's tiers when
// they join and when they switch profile. The plugin also calls sync() for
// everyone online after /ssreload, as proficiencies.yml may have changed.
public final class ProficiencyTierSync implements Listener {
    private final ProficiencyTierPermissions permissions;
    private final ProficiencyService proficiencies;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;

    public ProficiencyTierSync(ProficiencyTierPermissions permissions, ProficiencyService proficiencies,
                               Function<Player, Optional<ProfileKey>> activeCharacter) {
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.proficiencies = Objects.requireNonNull(proficiencies, "proficiencies");
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
        activeCharacter.apply(player).filter(proficiencies::hasCharacter)
                .ifPresent(character -> permissions.sync(character, proficiencies));
    }
}
