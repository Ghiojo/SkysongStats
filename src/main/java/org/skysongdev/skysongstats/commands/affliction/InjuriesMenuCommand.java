package org.skysongdev.skysongstats.commands.affliction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.menu.AfflictionMenus;
import org.skysongdev.skysongstats.menu.ListMenu;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /injuries, the menu of the injuries your character knows, in injuries.yml order:
//   /injuries                    all of them
//   /injuries view [severity]    only those of one severity
//   /injuries search <text>      only those whose name contains the text
// With skysongstats.injuries.all the menu holds every injury of injuries.yml,
// known or not, with the same filters. It changes only what the menu shows: the
// character learns nothing.
public final class InjuriesMenuCommand implements TabExecutor {
    static final String PERMISSION = "skysongstats.injuries";
    static final String SEE_ALL_PERMISSION = PERMISSION + ".all";
    private static final String KEYS = "injury.menu.";

    private final AfflictionService afflictions;
    private final CommandTargets targets;
    private final Supplier<Messages> messages;
    private final AfflictionMenus menus;

    public InjuriesMenuCommand(AfflictionService afflictions, CommandTargets targets, Supplier<Messages> messages,
                               AfflictionMenus menus) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "view";
        switch (sub) {
            case "view" -> {
                if (args.length > 1) {
                    severity(sender, args[1]);
                } else {
                    open(sender, injury -> true, "title", Map.of(), false);
                }
            }
            case "search" -> search(sender, args);
            default -> send(sender, "usage", Map.of());
        }
        return true;
    }

    private void severity(CommandSender sender, String typed) {
        String severity = typed.toLowerCase(Locale.ROOT);
        if (!afflictions.injuryDefinitions().severities().contains(severity)) {
            send(sender, "unknown-severity", Map.of("severity", typed));
            return;
        }
        open(sender, injury -> injury.severity().equals(severity), "severity-title",
                Map.of("severity", severity), false);
    }

    private void search(CommandSender sender, String... args) {
        if (args.length < 2) {
            send(sender, "usage", Map.of());
            return;
        }
        String text = String.join(" ", List.of(args).subList(1, args.length));
        open(sender, injury -> AfflictionMenus.nameContains(injury.name(), text), "search-title",
                Map.of("search", text), true);
    }

    // emptyIsMessage: a search that finds nothing says so instead of opening an empty menu.
    private void open(CommandSender sender, Predicate<InjuryDefinition> shown, String title,
                      Map<String, String> placeholders, boolean emptyIsMessage) {
        if (!(sender instanceof Player)) {
            messages.get().send(sender, "general.players-only", Map.of());
            return;
        }
        targets.resolve(sender, null, PERMISSION, null).ifPresent(target -> {
            Set<String> known = afflictions.known(target.character());
            boolean seesAll = sender.hasPermission(SEE_ALL_PERMISSION);
            List<InjuryDefinition> injuries = afflictions.injuryDefinitions().all().stream()
                    .filter(injury -> seesAll || known.contains(injury.id())).filter(shown).toList();
            if (injuries.isEmpty() && emptyIsMessage) {
                send(sender, "no-results", placeholders);
                return;
            }
            ListMenu menu = menus.injuries(injuries, title, placeholders);
            target.player().openInventory(menu.getInventory());
        });
    }

    private void send(CommandSender to, String key, Map<String, String> placeholders) {
        messages.get().send(to, KEYS + key, placeholders);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        Stream<String> options = Stream.empty();
        if (args.length == 1) {
            options = Stream.of("view", "search");
        } else if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            // Only the severities that have injuries.
            options = afflictions.injuryDefinitions().usedSeverities().stream();
        }
        return options.filter(option -> option.startsWith(typed)).toList();
    }
}
