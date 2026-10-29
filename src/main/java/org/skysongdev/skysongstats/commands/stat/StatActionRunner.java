package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.character.StatChange;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

// Runs one stat action for a command: /heal 5 Steve, /stats set str 2, ... Every
// stat command goes through here, so they all check the same things: permission
// (generated per stat and action), the target player, the amount, and that the
// target has a character. Then StatService applies the action and the player and
// whoever ran the command are told the result.
public final class StatActionRunner {
    static final String ERRORS = "stats.errors.";
    private static final String STATS = "stats.";
    private static final String DEFAULT = "default";

    private final StatService stats;
    private final CommandTargets targets;
    private final MessageSink messages;

    public StatActionRunner(StatService stats, CommandTargets targets, MessageSink messages) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // targetName is the player typed in the command, or null for the sender.
    public void run(CommandSender sender, StatDefinition stat, StatAction action, String amountText, String targetName) {
        Optional<CommandTargets.Target> target = targets.resolve(sender, targetName,
                StatPermissions.self(stat, action), StatPermissions.other(stat, action));
        OptionalInt amount = target.isPresent() ? amount(sender, action, amountText) : OptionalInt.empty();
        if (amount.isEmpty()) {
            return;
        }
        if (action == StatAction.DAMAGE && !hasShield(stat)) {
            messages.send(sender, ERRORS + "no-shield", Map.of("name", stat.display().name()));
            return;
        }
        CommandTargets.Target resolved = target.orElseThrow();
        int value = amount.getAsInt();
        report(sender, resolved.player(), stats.apply(resolved.character(), stat, action, value), action, value);
    }

    // A whole number; only set may be negative (str can be set to -2).
    private OptionalInt amount(CommandSender sender, StatAction action, String text) {
        try {
            int amount = Integer.parseInt(text.trim());
            if (amount < 0 && action != StatAction.SET) {
                messages.send(sender, ERRORS + "negative-amount", Map.of("input", text));
                return OptionalInt.empty();
            }
            return OptionalInt.of(amount);
        } catch (NumberFormatException e) {
            messages.send(sender, "general.not-a-number", Map.of("input", text));
            return OptionalInt.empty();
        }
    }

    private static boolean hasShield(StatDefinition stat) {
        return stat.isBar() && stat.bar().shield() != null;
    }

    // Tells the player what changed and, when someone else ran the command, them
    // too. A stat uses its own messages (stats.<id>.<action>) when messages.yml has
    // them, otherwise stats.default.<action>.
    private void report(CommandSender sender, Player target, StatChange change, StatAction action, int amount) {
        StatDefinition stat = change.stat();
        String own = STATS + stat.id() + "." + action.configName();
        String base = messages.has(own + ".player") ? own : STATS + DEFAULT + "." + action.configName();
        Map<String, String> placeholders = Map.of(
                "amount", Integer.toString(amount),
                "value", Integer.toString(change.newValue()),
                "old", Integer.toString(change.oldValue()),
                "player", target.getName(),
                "sender", sender.getName(),
                "name", stat.display().name(),
                "stat", stat.id());
        messages.send(target, base + ".player", placeholders);
        if (!target.equals(sender)) {
            messages.send(sender, base + ".sender", placeholders);
        }
    }
}
