package org.skysongdev.skysongstats.commands.downtime;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.service.downtime.DowntimeChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

/**
 * A player named in a downtime command. Online and offline players both work,
 * as long as they have joined before and have a SkysongStats profile.
 */
record DowntimeTarget(UUID uuid, String name) {

    /** Finds the player by exact name (any case), online first; {@code null} if they never joined or have no profile. */
    static DowntimeTarget find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        OfflinePlayer player = online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
        if (player == null) {
            return null;
        }
        DowntimeTarget target = new DowntimeTarget(player.getUniqueId(), player.getName() != null ? player.getName() : name);
        return target.profiles().isEmpty() ? null : target;
    }

    static DowntimeTarget of(Player player) {
        return new DowntimeTarget(player.getUniqueId(), player.getName());
    }

    List<String> profiles() {
        return getPlugin().getUtils().getProfileManager().findAllProfiles(uuid.toString());
    }

    /** Like {@link #find(String)}, but tells the sender when the player is not found. */
    static DowntimeTarget find(CommandSender sender, String name) {
        DowntimeTarget target = find(name);
        if (target == null) {
            getPlugin().getMessages().send(sender, "downtime.player-not-found", Map.of("player", name));
        }
        return target;
    }

    /** Like {@link #findProfile(String)}, but tells the sender when the profile is not found. */
    String findProfile(CommandSender sender, String profile) {
        String found = findProfile(profile);
        if (found == null) {
            getPlugin().getMessages().send(sender, "downtime.profile-not-found", Map.of("profile", profile, "player", name));
        }
        return found;
    }

    /** The profile name as stored, matched without case; {@code null} if the player has no such profile. */
    String findProfile(String profile) {
        for (String current : profiles()) {
            if (current.equalsIgnoreCase(profile)) {
                return current;
            }
        }
        return null;
    }

    String activeProfile() {
        return getPlugin().getUtils().getProfileManager().getActiveProfileName(uuid.toString());
    }

    /** Online player names starting with {@code prefix}, ignoring case, for tab completion. */
    static List<String> onlineNames(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .toList();
    }

    /** "global" plus the named player's profiles, starting with {@code prefix}, for tab completion. */
    static List<String> poolNames(String playerName, String prefix) {
        String lower = prefix.toLowerCase();
        DowntimeTarget target = find(playerName);
        List<String> options = new ArrayList<>();
        options.add("global");
        if (target != null) {
            options.addAll(target.profiles());
        }
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).toList();
    }

    /** Like {@link #parseAmount(String)}, but tells the sender when the amount is wrong. */
    static int parseAmount(CommandSender sender, String text) {
        int amount = parseAmount(text);
        if (amount < 0) {
            getPlugin().getMessages().send(sender, "downtime.invalid-amount");
        }
        return amount;
    }

    /** Parses a downtime amount: a whole number of 1 or more, otherwise {@code -1}. */
    static int parseAmount(String text) {
        try {
            int amount = Integer.parseInt(text);
            return amount >= 1 ? amount : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The {@code {preference}} word from messages.yml: global or character spending. */
    static String preferenceName(boolean spendsGlobal) {
        return plainText(spendsGlobal ? "downtime.preference-global" : "downtime.preference-character");
    }

    private static String plainText(String key) {
        return PlainTextComponentSerializer.plainText().serialize(getPlugin().getMessages().get(key));
    }

    /** {amount}, {player}, {profile} and {total} for the add and remove messages. */
    static Map<String, String> changePlaceholders(DowntimeTarget target, DowntimeChange change, int amount) {
        return Map.of(
                "amount", String.valueOf(amount),
                "player", target.name(),
                "profile", change.profile() == null ? "" : change.profile(),
                "total", String.valueOf(change.newValue()));
    }
}
