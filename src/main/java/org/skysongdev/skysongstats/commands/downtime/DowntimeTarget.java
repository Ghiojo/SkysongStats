package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

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
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + name + " not found or has no profile."));
        }
        return target;
    }

    /** Like {@link #findProfile(String)}, but tells the sender when the profile is not found. */
    String findProfile(CommandSender sender, String profile) {
        String found = findProfile(profile);
        if (found == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Profile " + profile + " not found for player " + name + "."));
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
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Amount must be a whole number of 1 or more."));
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
}
