package org.skysongdev.skysongstats.commands.affliction;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.menu.AfflictionMenus;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// The menu of every condition in conditions.yml (conditions aren't learnt), in
// file order, for /condition list and /condition search <text>. Any player with
// skysongstats.conditions can look, with or without a character.
public final class ConditionMenu {
    static final String PERMISSION = "skysongstats.conditions";
    private static final String KEYS = "condition.menu.";

    private final AfflictionService afflictions;
    private final Supplier<Messages> messages;
    private final AfflictionMenus menus;

    public ConditionMenu(AfflictionService afflictions, Supplier<Messages> messages, AfflictionMenus menus) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    void openAll(CommandSender sender) {
        if (mayOpen(sender)) {
            show((Player) sender, afflictions.conditionDefinitions().all(), "title", Map.of());
        }
    }

    // A search that finds nothing says so instead of opening an empty menu.
    void openSearch(CommandSender sender, String text) {
        if (!mayOpen(sender)) {
            return;
        }
        List<ConditionDefinition> found = afflictions.conditionDefinitions().all().stream()
                .filter(condition -> AfflictionMenus.nameContains(condition.name(), text)).toList();
        Map<String, String> placeholders = Map.of("search", text);
        if (found.isEmpty()) {
            messages.get().send(sender, KEYS + "no-results", placeholders);
            return;
        }
        show((Player) sender, found, "search-title", placeholders);
    }

    private void show(Player player, List<ConditionDefinition> conditions, String title,
                      Map<String, String> placeholders) {
        player.openInventory(menus.conditions(conditions, title, placeholders).getInventory());
    }

    // Tells the sender when they can't.
    private boolean mayOpen(CommandSender sender) {
        if (!(sender instanceof Player)) {
            messages.get().send(sender, "general.players-only", Map.of());
            return false;
        }
        if (!sender.hasPermission(PERMISSION)) {
            messages.get().send(sender, "general.no-permission", Map.of("permission", PERMISSION));
            return false;
        }
        return true;
    }
}
