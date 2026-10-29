package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinitions.CommandBinding;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// One command name from a stat's "commands:" block in stats.yml, e.g. /heal
// <amount> [player], which runs "add" on hp exactly like /stats add hp. These
// are registered at runtime by StatCommandRegistry instead of being listed in
// plugin.yml.
public final class StatAliasCommand extends Command {
    private final StatService stats;
    private final StatActionRunner runner;
    private final MessageSink messages;
    private final Supplier<List<String>> onlinePlayers;

    public StatAliasCommand(String label, StatService stats, StatActionRunner runner, MessageSink messages,
                            Supplier<List<String>> onlinePlayers) {
        super(label);
        this.stats = Objects.requireNonNull(stats, "stats");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        setUsage("/" + label + " <amount> [player]");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        // Looked up on every use, so a reloaded stats.yml applies straight away.
        CommandBinding binding = stats.definitions().findCommand(getName()).orElse(null);
        if (binding == null) {
            messages.send(sender, StatActionRunner.ERRORS + "unknown-command", Map.of("command", getName()));
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            messages.send(sender, StatActionRunner.ERRORS + "usage", Map.of("usage", getUsage()));
            return true;
        }
        runner.run(sender, binding.stat(), binding.action(), args[0], args.length > 1 ? args[1] : null);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        String typed = args[1].toLowerCase(Locale.ROOT);
        return onlinePlayers.get().stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
