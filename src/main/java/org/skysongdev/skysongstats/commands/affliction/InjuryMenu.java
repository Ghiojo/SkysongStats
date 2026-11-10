package org.skysongdev.skysongstats.commands.affliction;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

// The menu of the injuries your character knows, in injuries.yml order, for
// /injury list [severity] and /injury search <text>. With
// skysongstats.injuries.all it holds every injury of injuries.yml, known or not,
// with the same filters. It changes only what the menu shows: the character
// learns nothing.
public final class InjuryMenu {
    static final String PERMISSION = "skysongstats.injuries";
    static final String SEE_ALL_PERMISSION = PERMISSION + ".all";
    private static final String KEYS = "injury.menu.";

    private final AfflictionService afflictions;
    private final CommandTargets targets;
    private final Supplier<Messages> messages;
    private final AfflictionMenus menus;

    public InjuryMenu(AfflictionService afflictions, CommandTargets targets, Supplier<Messages> messages,
                      AfflictionMenus menus) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    // Every injury the menu may hold.
    void openAll(CommandSender sender) {
        open(sender, injury -> true, "title", Map.of(), false);
    }

    void openSeverity(CommandSender sender, String typed) {
        String severity = typed.toLowerCase(Locale.ROOT);
        if (!afflictions.injuryDefinitions().severities().contains(severity)) {
            send(sender, "unknown-severity", Map.of("severity", typed));
            return;
        }
        open(sender, injury -> injury.severity().equals(severity), "severity-title",
                Map.of("severity", severity), false);
    }

    void openSearch(CommandSender sender, String text) {
        open(sender, injury -> AfflictionMenus.nameContains(injury.name(), text), "search-title",
                Map.of("search", text), true);
    }

    // Only the severities that have injuries, for tab completion.
    List<String> severities() {
        return afflictions.injuryDefinitions().usedSeverities();
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
}
