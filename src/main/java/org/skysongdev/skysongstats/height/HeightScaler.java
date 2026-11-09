package org.skysongdev.skysongstats.height;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Function;

// Makes a player's Minecraft scale follow their active character's height: the
// total of the stat with a "scale:" block in stats.yml. The scale is a modifier
// named skysongstats:height that multiplies whatever scale the player already has,
// so /attribute and other plugins still stack with it. It is transient (never
// saved in the player's data): it is put back when they join, and leaves with the
// plugin. Players without a loaded character, or with no height stat in stats.yml,
// get no modifier.
//
// The plugin calls refresh() when a character's totals change and after
// /ssreload; joining and switching profile are listened to here.
public final class HeightScaler implements Listener {
    public static final NamespacedKey MODIFIER = new NamespacedKey("skysongstats", "height");

    private final StatService stats;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;
    private final Function<UUID, Player> onlinePlayer;
    private final Function<Player, AttributeInstance> scaleAttribute;

    public HeightScaler(StatService stats, Function<Player, Optional<ProfileKey>> activeCharacter,
                        Function<UUID, Player> onlinePlayer) {
        this(stats, activeCharacter, onlinePlayer, player -> player.getAttribute(Attribute.SCALE));
    }

    // For tests, whose fake players have no scale attribute.
    HeightScaler(StatService stats, Function<Player, Optional<ProfileKey>> activeCharacter,
                 Function<UUID, Player> onlinePlayer, Function<Player, AttributeInstance> scaleAttribute) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
        this.onlinePlayer = Objects.requireNonNull(onlinePlayer, "onlinePlayer");
        this.scaleAttribute = Objects.requireNonNull(scaleAttribute, "scaleAttribute");
    }

    // After the listener that gives a new player their first character.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileSwitch(ProfileUpdateEvent event) {
        refresh(event.getPlayer());
    }

    // For a change to this character: only matters while its player is online and
    // playing it.
    public void refresh(ProfileKey character) {
        Player player = onlinePlayer.apply(character.player());
        if (player != null && activeCharacter.apply(player).filter(character::equals).isPresent()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        AttributeInstance scale = scaleAttribute.apply(player);
        if (scale == null) {
            return;
        }
        // MULTIPLY_SCALAR_1 multiplies the scale by (1 + amount).
        OptionalDouble target = scaleOf(player);
        AttributeModifier current = scale.getModifier(MODIFIER);
        if (current != null && target.isPresent() && Double.compare(current.getAmount(), target.getAsDouble() - 1) == 0) {
            return;
        }
        if (current != null) {
            scale.removeModifier(MODIFIER);
        }
        target.ifPresent(value -> scale.addTransientModifier(
                new AttributeModifier(MODIFIER, value - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1)));
    }

    // Takes the height modifier off, for when the plugin stops.
    public void remove(Player player) {
        AttributeInstance scale = scaleAttribute.apply(player);
        if (scale != null && scale.getModifier(MODIFIER) != null) {
            scale.removeModifier(MODIFIER);
        }
    }

    // The scale the player's active character should have, or empty when there is
    // none to give.
    private OptionalDouble scaleOf(Player player) {
        Optional<StatDefinition> height = stats.definitions().height();
        Optional<ProfileKey> character = activeCharacter.apply(player);
        if (height.isEmpty() || character.isEmpty()) {
            return OptionalDouble.empty();
        }
        return stats.sheet(character.get())
                .map(sheet -> OptionalDouble.of(height.get().scale().scaleFor(stats.calculator(sheet).total(height.get()))))
                .orElse(OptionalDouble.empty());
    }
}
