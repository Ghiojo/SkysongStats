package org.skysongdev.skysongstats.commands.affliction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.menu.AfflictionMenus;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// /conditions, the menu of every condition in conditions.yml (conditions aren't
// learnt), in file order:
//   /conditions                 all of them
//   /conditions search <text>   only those whose name contains the text
public final class ConditionsMenuCommand implements TabExecutor {
    private static final String KEYS = "condition.menu.";

    private final AfflictionService afflictions;
    private final Supplier<Messages> messages;
    private final AfflictionMenus menus;

    public ConditionsMenuCommand(AfflictionService afflictions, Supplier<Messages> messages, AfflictionMenus menus) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            open(sender, afflictions.conditionDefinitions().all(), "title", Map.of());
        } else if ("search".equalsIgnoreCase(args[0]) && args.length > 1) {
            search(sender, String.join(" ", List.of(args).subList(1, args.length)));
        } else {
            messages.get().send(sender, KEYS + "usage", Map.of());
        }
        return true;
    }

    private void search(CommandSender sender, String text) {
        List<ConditionDefinition> found = afflictions.conditionDefinitions().all().stream()
                .filter(condition -> AfflictionMenus.nameContains(condition.name(), text)).toList();
        Map<String, String> placeholders = Map.of("search", text);
        if (found.isEmpty()) {
            messages.get().send(sender, KEYS + "no-results", placeholders);
            return;
        }
        open(sender, found, "search-title", placeholders);
    }

    // Conditions aren't learnt, so any player can look, with or without a character.
    // plugin.yml checks the permission.
    private void open(CommandSender sender, List<ConditionDefinition> conditions, String title,
                      Map<String, String> placeholders) {
        if (sender instanceof Player player) {
            player.openInventory(menus.conditions(conditions, title, placeholders).getInventory());
        } else {
            messages.get().send(sender, "general.players-only", Map.of());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return args.length == 1 && "search".startsWith(typed) ? List.of("search") : List.of();
    }
}
