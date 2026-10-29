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
public final class StatsRootCommand implements TabExecutor {
    private static final String USAGE = "/stats <set|add|reduce|damage> <stat> <amount> [player]";

    private final StatService stats;
    private final StatActionRunner runner;
    private final MessageSink messages;
    private final Supplier<List<String>> onlinePlayers;

    public StatsRootCommand(StatService stats, StatActionRunner runner, MessageSink messages,
                            Supplier<List<String>> onlinePlayers) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        Optional<StatAction> action = args.length > 0 ? StatAction.fromConfigName(args[0]) : Optional.empty();
        if (action.isEmpty() || args.length < 3 || args.length > 4) {
            messages.send(sender, StatActionRunner.ERRORS + "usage", Map.of("usage", USAGE));
            return true;
        }
        Optional<StatDefinition> stat = stats.definitions().find(args[1]);
        if (stat.isEmpty()) {
            messages.send(sender, StatActionRunner.ERRORS + "unknown-stat", Map.of("stat", args[1]));
            return true;
        }
        runner.run(sender, stat.orElseThrow(), action.orElseThrow(), args[2], args.length > 3 ? args[3] : null);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        Stream<String> options = switch (args.length) {
            case 1 -> Arrays.stream(StatAction.values()).map(StatAction::configName);
            case 2 -> statNames().stream();
            case 4 -> onlinePlayers.get().stream();
            default -> Stream.empty();
        };
        return options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
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
