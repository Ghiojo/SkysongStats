package org.skysongdev.skysongstats.commands.ancestry;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AncestryService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /ancestry, on the ancestries from ancestries.yml:
//   /ancestry choose                      makes your character's pending choices
//   /ancestry set <player> <ancestry>     gives the character that ancestry (staff)
//   /ancestry rechoose <player>           makes its choices pending again (staff)
//   /ancestry remove <player>             leaves it with no ancestry (staff)
// Staff commands work on the player's active character, online or offline. set
// ignores the ancestry's permission: and, given the ancestry it already has,
// puts its current version from ancestries.yml on.
public final class AncestryCommand implements TabExecutor {
    static final String KEYS = "ancestry.command.";
    private static final String PERMISSION = "skysongstats.ancestry.";
    private static final List<String> STAFF = List.of("set", "rechoose", "remove");

    // Finds a player who has joined before by name, and the character they are
    // playing now when it has stats.
    public record Lookup(Function<String, Optional<OfflinePlayer>> player,
                         Function<UUID, Optional<ProfileKey>> character) {
        public Lookup {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(character, "character");
        }
    }

    private record Target(OfflinePlayer player, String name, ProfileKey character) {
    }

    private final AncestryService ancestries;
    private final AncestryPrompt prompt;
    private final Lookup lookup;
    private final Supplier<Messages> messages;
    private final Supplier<List<String>> onlinePlayers;
    private final Consumer<String> log;
    private final ChangeCauses causes;

    // log gets a line for every change staff make, so the console sees who changed what.
    public AncestryCommand(AncestryService ancestries, AncestryPrompt prompt, Lookup lookup,
                           Supplier<Messages> messages, Supplier<List<String>> onlinePlayers, Consumer<String> log,
                           ChangeCauses causes) {
        this.ancestries = Objects.requireNonNull(ancestries, "ancestries");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.log = Objects.requireNonNull(log, "log");
        this.causes = Objects.requireNonNull(causes, "causes");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if ("choose".equals(sub)) {
            choose(sender);
        } else if (STAFF.contains(sub)) {
            staff(sender, sub, args);
        } else {
            send(sender, "usage", Map.of());
        }
        return true;
    }

    private void choose(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "choose.players-only", Map.of());
            return;
        }
        if (!allowed(sender, PERMISSION + "choose")) {
            return;
        }
        switch (prompt.openChoices(player)) {
            case NO_CHARACTER -> send(sender, "choose.no-character", Map.of());
            case NOTHING_PENDING -> send(sender, "choose.nothing-pending", Map.of());
            case ANCESTRY_GONE -> send(sender, "choose.ancestry-gone", Map.of());
            default -> {
                // The page is open.
            }
        }
    }

    private void staff(CommandSender sender, String sub, String... args) {
        int needed = "set".equals(sub) ? 3 : 2;
        if (args.length < needed) {
            send(sender, sub + ".usage", Map.of());
            return;
        }
        if (!allowed(sender, PERMISSION + sub)) {
            return;
        }
        target(sender, args[1]).ifPresent(target -> {
            switch (sub) {
                case "set" -> ancestry(sender, args[2]).ifPresent(ancestry -> set(sender, target, ancestry));
                case "rechoose" -> rechoose(sender, target);
                default -> remove(sender, target);
            }
        });
    }

    private void set(CommandSender sender, Target target, AncestryDefinition ancestry) {
        boolean pending = causes.during(ChangeCause.COMMAND,
                () -> ancestries.set(target.character(), ancestry, Optional.empty())).after().orElseThrow()
                .choicesPending();
        Map<String, String> placeholders = placeholders(target, ancestry.name());
        send(sender, pending ? "set.success-pending" : "set.success", placeholders);
        tellPlayer(sender, target, "set.player", placeholders);
        logChange(sender, "gave ancestry " + ancestry.id() + " to", target);
    }

    private void rechoose(CommandSender sender, Target target) {
        Optional<String> name = ancestries.definitionOf(target.character()).map(AncestryDefinition::name);
        if (causes.during(ChangeCause.COMMAND, () -> ancestries.rechoose(target.character())).isEmpty()) {
            send(sender, "rechoose.none", placeholders(target, ""));
            return;
        }
        // The player gets the prompt to choose.
        send(sender, "rechoose.success", placeholders(target, name.orElse("")));
        logChange(sender, "opened the ancestry choices of", target);
    }

    private void remove(CommandSender sender, Target target) {
        Optional<String> name = ancestries.definitionOf(target.character()).map(AncestryDefinition::name)
                .or(() -> ancestries.of(target.character()).map(StoredAncestry::ancestry));
        if (causes.during(ChangeCause.COMMAND, () -> ancestries.remove(target.character())).isEmpty()) {
            send(sender, "remove.none", placeholders(target, ""));
            return;
        }
        Map<String, String> placeholders = placeholders(target, name.orElse(""));
        send(sender, "remove.success", placeholders);
        tellPlayer(sender, target, "remove.player", placeholders);
        logChange(sender, "removed the ancestry of", target);
    }

    private Optional<Target> target(CommandSender sender, String name) {
        Optional<OfflinePlayer> player = lookup.player().apply(name);
        if (player.isEmpty()) {
            send(sender, "player-not-found", Map.of("player", name));
            return Optional.empty();
        }
        String known = player.get().getName() != null ? player.get().getName() : name;
        Optional<ProfileKey> character = lookup.character().apply(player.get().getUniqueId());
        if (character.isEmpty()) {
            send(sender, "no-character", Map.of("player", known));
            return Optional.empty();
        }
        return Optional.of(new Target(player.get(), known, character.get()));
    }

    private Optional<AncestryDefinition> ancestry(CommandSender sender, String id) {
        Optional<AncestryDefinition> ancestry = ancestries.definitions().get(id);
        if (ancestry.isEmpty()) {
            send(sender, "unknown-ancestry", Map.of("ancestry", id));
        }
        return ancestry;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.get().send(sender, "general.no-permission", Map.of("permission", permission));
        return false;
    }

    // Only when someone else changed it and the player is online.
    private void tellPlayer(CommandSender sender, Target target, String key, Map<String, String> placeholders) {
        Player online = target.player().getPlayer();
        if (online != null && !online.equals(sender)) {
            send(online, key, placeholders);
        }
    }

    private static Map<String, String> placeholders(Target target, String ancestry) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.name());
        placeholders.put("profile", target.character().profile());
        placeholders.put("ancestry", ancestry);
        return placeholders;
    }

    private void logChange(CommandSender sender, String what, Target target) {
        log.accept(sender.getName() + " " + what + " " + target.name() + " (" + target.character().profile() + ")");
    }

    private void send(CommandSender to, String key, Map<String, String> placeholders) {
        messages.get().send(to, KEYS + key, placeholders);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions(sender, args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> suggestions(CommandSender sender, String... args) {
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            return Stream.concat(Stream.of("choose"), STAFF.stream())
                    .filter(option -> sender.hasPermission(PERMISSION + option));
        }
        if (!STAFF.contains(sub) || !sender.hasPermission(PERMISSION + sub)) {
            return Stream.empty();
        }
        if (args.length == 2) {
            return onlinePlayers.get().stream();
        }
        if (args.length == 3 && "set".equals(sub)) {
            return ancestries.definitions().all().stream().map(AncestryDefinition::id);
        }
        return Stream.empty();
    }
}
