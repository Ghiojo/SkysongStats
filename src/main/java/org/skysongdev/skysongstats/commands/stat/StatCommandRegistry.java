package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatDefinitions.CommandBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// Registers the command names from stats.yml ("commands:" blocks) with the
// server, and swaps them when stats.yml is reloaded: names that left the file are
// removed, new ones are added. Each name also gets the usual
// "skysongstats:<name>" form.
public final class StatCommandRegistry {
    private final CommandMap commandMap;
    private final String fallbackPrefix;
    private final Function<String, Command> commandFactory;
    private final Map<String, Command> registered = new LinkedHashMap<>();

    // commandFactory makes the command for a name (a StatAliasCommand).
    public StatCommandRegistry(CommandMap commandMap, String fallbackPrefix, Function<String, Command> commandFactory) {
        this.commandMap = Objects.requireNonNull(commandMap, "commandMap");
        this.fallbackPrefix = Objects.requireNonNull(fallbackPrefix, "fallbackPrefix");
        this.commandFactory = Objects.requireNonNull(commandFactory, "commandFactory");
    }

    // Registers every command name in the definitions, replacing the previous set.
    // Returns the names another plugin already owns: those only work as
    // /skysongstats:<name>.
    public List<String> register(StatDefinitions definitions) {
        unregisterAll();
        List<String> taken = new ArrayList<>();
        for (CommandBinding binding : definitions.commands()) {
            Command command = commandFactory.apply(binding.label());
            if (!commandMap.register(binding.label(), fallbackPrefix, command)) {
                taken.add(binding.label());
            }
            registered.put(binding.label(), command);
        }
        return taken;
    }

    public void unregisterAll() {
        Map<String, Command> known = commandMap.getKnownCommands();
        registered.forEach((label, command) -> {
            command.unregister(commandMap);
            known.remove(label, command);
            known.remove(fallbackPrefix + ":" + label, command);
        });
        registered.clear();
    }

    public List<String> labels() {
        return List.copyOf(registered.keySet());
    }
}
