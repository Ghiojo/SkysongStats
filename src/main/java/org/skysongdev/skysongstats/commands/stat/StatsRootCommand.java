package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /stats <set|add|reduce|damage> <stat> <amount> [player], for any stat in
// stats.yml by id or alias. The per-stat command names (/heal...) run the same
// actions through StatAliasCommand.
// /stats and /stats view [player] show the stat sheet (see StatSheet). Viewing
// someone else's needs skysongstats.stats.view.other, which everyone has by
// default, as before.
public final class StatsRootCommand implements TabExecutor {
    static final String VIEW_OTHER = "skysongstats.stats.view.other";
    private static final String USAGE = "/stats <view|set|add|reduce|damage> ...";
    private static final String VIEW = "view";

    private final StatService stats;
    private final StatActionRunner runner;
    private final CommandTargets targets;
    private final StatSheet sheet;
    private final MessageSink messages;
    private final Supplier<List<String>> onlinePlayers;

    public StatsRootCommand(StatService stats, StatActionRunner runner, CommandTargets targets, StatSheet sheet,
                            MessageSink messages, Supplier<List<String>> onlinePlayers) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0 || VIEW.equalsIgnoreCase(args[0])) {
            view(sender, args);
        } else {
            act(sender, args);
        }
        return true;
    }

    // /stats <set|add|reduce|damage> <stat> <amount> [player]
    private void act(CommandSender sender, String... args) {
        Optional<StatAction> action = StatAction.fromConfigName(args[0]);
        if (action.isEmpty() || args.length < 3 || args.length > 4) {
            usage(sender);
            return;
        }
        Optional<StatDefinition> stat = stats.definitions().find(args[1]);
        if (stat.isEmpty()) {
            messages.send(sender, StatActionRunner.ERRORS + "unknown-stat", Map.of("stat", args[1]));
            return;
        }
        runner.run(sender, stat.orElseThrow(), action.orElseThrow(), args[2], args.length > 3 ? args[3] : null);
    }

    private void view(CommandSender sender, String... args) {
        if (args.length > 2) {
            usage(sender);
            return;
        }
        targets.resolve(sender, args.length == 2 ? args[1] : null, null, VIEW_OTHER)
                .ifPresent(target -> sheet.show(sender, target));
    }

    private void usage(CommandSender sender) {
        messages.send(sender, StatActionRunner.ERRORS + "usage", Map.of("usage", USAGE));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options(args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> options(String... args) {
        if (args.length > 1 && VIEW.equalsIgnoreCase(args[0])) {
            return args.length == 2 ? onlinePlayers.get().stream() : Stream.empty();
        }
        return switch (args.length) {
            case 1 -> Stream.concat(Stream.of(VIEW), Arrays.stream(StatAction.values()).map(StatAction::configName));
            case 2 -> statNames().stream();
            case 4 -> onlinePlayers.get().stream();
            default -> Stream.empty();
        };
    }

    private List<String> statNames() {
        List<String> names = new ArrayList<>();
        for (StatDefinition stat : stats.definitions().all()) {
            names.add(stat.id());
            names.addAll(stat.display().aliases());
        }
        return names;
    }
}
