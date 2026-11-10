package org.skysongdev.skysongstats.commands.affliction;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.messages.AfflictionTexts;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.storage.LearnSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /injury, on the injuries from injuries.yml:
//   /injury list [severity]             the menu of the injuries you know (InjuryMenu)
//   /injury search <text>               the same menu, only names containing the text
//   /injury award <player> <injury>     gives it, and teaches it (staff)
//   /injury remove <player> <injury>    takes it away; still known (staff)
//   /injury discover <player> <injury>  teaches it without giving it (staff)
//   /injury forget <player> <injury>    undoes a discover (staff)
// An injury is named by its id, in any case. A character's current injuries are
// on the /stats sheet.
public final class InjuryCommand implements TabExecutor {
    static final String KEYS = "injury.";
    private static final String STAFF_PERMISSION = "skysongstats.injury.";
    private static final List<String> STAFF = List.of("award", "remove", "discover", "forget");

    private final AfflictionService afflictions;
    private final InjuryMenu menu;
    private final CommandTargets targets;
    private final Supplier<Messages> messages;
    private final AfflictionTexts texts;
    private final Supplier<List<String>> onlinePlayers;
    private final Consumer<String> log;
    private final ChangeCauses causes;

    // log gets a line for every change staff make, so the console sees who changed what.
    public InjuryCommand(AfflictionService afflictions, InjuryMenu menu, CommandTargets targets,
                         Supplier<Messages> messages, AfflictionTexts texts, Supplier<List<String>> onlinePlayers,
                         Consumer<String> log, ChangeCauses causes) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.texts = Objects.requireNonNull(texts, "texts");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.log = Objects.requireNonNull(log, "log");
        this.causes = Objects.requireNonNull(causes, "causes");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
        if ("list".equals(sub)) {
            list(sender, args);
        } else if ("search".equals(sub)) {
            search(sender, args);
        } else if (STAFF.contains(sub)) {
            staff(sender, sub, args);
        } else {
            send(sender, "unknown-subcommand", Map.of());
        }
        return true;
    }

    private void list(CommandSender sender, String... args) {
        if (args.length > 1) {
            menu.openSeverity(sender, args[1]);
        } else {
            menu.openAll(sender);
        }
    }

    private void search(CommandSender sender, String... args) {
        if (args.length < 2) {
            send(sender, "search-usage", Map.of());
            return;
        }
        menu.openSearch(sender, String.join(" ", List.of(args).subList(1, args.length)));
    }

    private void staff(CommandSender sender, String sub, String... args) {
        if (args.length < 3) {
            send(sender, "usage", Map.of());
            return;
        }
        String permission = permission(sub);
        targets.resolve(sender, args[1], permission, permission).ifPresent(target ->
                injury(sender, args[2]).ifPresent(injury ->
                        causes.during(ChangeCause.COMMAND, () -> run(sender, sub, target, injury))));
    }

    private void run(CommandSender sender, String sub, CommandTargets.Target target, InjuryDefinition injury) {
        switch (sub) {
            case "award" -> award(sender, target, injury);
            case "remove" -> remove(sender, target, injury);
            case "discover" -> discover(sender, target, injury);
            default -> forget(sender, target, injury);
        }
    }

    private void award(CommandSender sender, CommandTargets.Target target, InjuryDefinition injury) {
        boolean isNew = afflictions.awardInjury(target.character(), injury.id()).orElseThrow().isNew();
        send(sender, isNew ? "award.success" : "award.already-has", player(target), injury);
        logChange(sender, "gave", injury, target);
    }

    private void remove(CommandSender sender, CommandTargets.Target target, InjuryDefinition injury) {
        if (!afflictions.removeInjury(target.character(), injury.id())) {
            send(sender, "remove.not-injured", player(target), injury);
            return;
        }
        send(sender, "remove.success", player(target), injury);
        if (!target.player().equals(sender)) {
            send(target.player(), "remove.lost", Map.of(), injury);
        }
        logChange(sender, "removed", injury, target);
    }

    private void discover(CommandSender sender, CommandTargets.Target target, InjuryDefinition injury) {
        if (afflictions.discover(target.character(), injury.id(), LearnSource.DISCOVER)
                == AfflictionService.DiscoverResult.ALREADY_KNOWN) {
            send(sender, "discover.already-known", player(target), injury);
            return;
        }
        send(sender, "discover.success", player(target), injury);
        if (!target.player().equals(sender)) {
            send(target.player(), "discover.learnt", Map.of(), injury);
        }
        logChange(sender, "taught", injury, target);
    }

    private void forget(CommandSender sender, CommandTargets.Target target, InjuryDefinition injury) {
        switch (afflictions.forget(target.character(), injury.id())) {
            case FORGOTTEN -> {
                send(sender, "forget.success", player(target), injury);
                logChange(sender, "made forget", injury, target);
            }
            case STILL_INJURED -> send(sender, "forget.still-injured", player(target), injury);
            default -> send(sender, "forget.not-known", player(target), injury);
        }
    }

    // Forgetting undoes a discover, so it needs the same node.
    static String permission(String sub) {
        return STAFF_PERMISSION + ("forget".equals(sub) ? "discover" : sub);
    }

    private Optional<InjuryDefinition> injury(CommandSender sender, String id) {
        Optional<InjuryDefinition> injury = afflictions.injuryDefinitions().get(id);
        if (injury.isEmpty()) {
            send(sender, "unknown-injury", Map.of("injury", id));
        }
        return injury;
    }

    private static Map<String, String> player(CommandTargets.Target target) {
        return Map.of("player", target.player().getName());
    }

    private void logChange(CommandSender sender, String what, InjuryDefinition injury, CommandTargets.Target target) {
        log.accept(sender.getName() + " " + what + " injury " + injury.id() + " to " + target.player().getName()
                + " (" + target.character().profile() + ")");
    }

    private void send(CommandSender to, String key, Map<String, String> placeholders) {
        messages.get().send(to, KEYS + key, placeholders);
    }

    // {injury} is the injury's tag: its name, with the description on hover.
    private void send(CommandSender to, String key, Map<String, String> placeholders, InjuryDefinition injury) {
        Map<String, Component> components = Map.of("injury", texts.injuryTag(injury));
        messages.get().send(to, KEYS + key, placeholders, components);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions(sender, args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> suggestions(CommandSender sender, String... args) {
        if (args.length == 1) {
            Stream<String> staff = STAFF.stream().filter(sub -> sender.hasPermission(permission(sub)));
            return Stream.concat(Stream.of("list", "search"), staff);
        }
        boolean staff = STAFF.contains(args[0].toLowerCase(Locale.ROOT));
        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            return menu.severities().stream();
        }
        if (args.length == 2 && staff) {
            return onlinePlayers.get().stream();
        }
        if (args.length == 3 && staff) {
            return afflictions.injuryDefinitions().all().stream().map(InjuryDefinition::id);
        }
        return Stream.empty();
    }
}
