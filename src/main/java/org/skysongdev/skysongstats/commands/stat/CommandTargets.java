package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Works out whose character a stat or modifier command is for: the sender, or
// the player they named. Checks the permission for acting on yourself or on
// someone else, that the player is online, and that they have a character.
// Every failure is told to the sender.
public final class CommandTargets {
    private final StatService stats;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;
    private final Function<String, Player> findPlayer;
    private final MessageSink messages;

    public record Target(Player player, ProfileKey character) {
    }

    // findPlayer finds an online player by the name typed in the command, or null.
    public CommandTargets(StatService stats, Function<Player, Optional<ProfileKey>> activeCharacter,
                          Function<String, Player> findPlayer, MessageSink messages) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
        this.findPlayer = Objects.requireNonNull(findPlayer, "findPlayer");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // targetName is the player typed in the command, or null for the sender.
    // selfPermission may be null when acting on yourself needs no extra node.
    public Optional<Target> resolve(CommandSender sender, String targetName, String selfPermission,
                                    String otherPermission) {
        return player(sender, targetName, selfPermission, otherPermission).flatMap(player -> character(sender, player));
    }

    private Optional<Player> player(CommandSender sender, String targetName, String selfPermission,
                                    String otherPermission) {
        if (targetName == null) {
            if (sender instanceof Player self) {
                return allowed(sender, selfPermission) ? Optional.of(self) : Optional.empty();
            }
            messages.send(sender, "general.console-needs-player", Map.of());
            return Optional.empty();
        }
        Player target = findPlayer.apply(targetName);
        if (target == null) {
            messages.send(sender, "general.player-not-found", Map.of("player", targetName));
            return Optional.empty();
        }
        String permission = target.equals(sender) ? selfPermission : otherPermission;
        return allowed(sender, permission) ? Optional.of(target) : Optional.empty();
    }

    private Optional<Target> character(CommandSender sender, Player player) {
        Optional<ProfileKey> character = activeCharacter.apply(player).filter(key -> stats.sheet(key).isPresent());
        if (character.isEmpty()) {
            messages.send(sender, StatActionRunner.ERRORS + "no-character", Map.of("player", player.getName()));
        }
        return character.map(key -> new Target(player, key));
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (permission == null || sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "general.no-permission", Map.of("permission", permission));
        return false;
    }
}
