package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.character.StatChange;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.HeightScale;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.height.HeightText;

import java.util.HashMap;
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
        if (isSplitHeight(sender, stat, amountText, targetName)) {
            return;
        }
        Optional<CommandTargets.Target> target = targets.resolve(sender, targetName,
                StatPermissions.self(stat, action), StatPermissions.other(stat, action));
        OptionalInt amount = target.isPresent() ? amount(sender, stat, action, amountText) : OptionalInt.empty();
        if (amount.isEmpty()) {
            return;
        }
        if (action == StatAction.DAMAGE && !hasShield(stat)) {
            messages.send(sender, ERRORS + "no-shield", Map.of("name", stat.display().name()));
            return;
        }
        CommandTargets.Target resolved = target.orElseThrow();
        int value = amount.getAsInt();
        if (!isAllowedHeight(sender, resolved, stat, action, value)) {
            return;
        }
        report(sender, resolved.player(), stats.apply(resolved.character(), stat, action, value), action, value);
    }

    // "/height 5' 10\"": the space split the height, so 10" is not a player name.
    private boolean isSplitHeight(CommandSender sender, StatDefinition stat, String amountText, String targetName) {
        if (!stat.isHeight() || !HeightText.isLoneInches(targetName)) {
            return false;
        }
        messages.send(sender, ERRORS + "height-space", Map.of("input", amountText + " " + targetName));
        return true;
    }

    private OptionalInt amount(CommandSender sender, StatDefinition stat, StatAction action, String text) {
        if (!stat.isHeight()) {
            return wholeNumber(sender, action, text);
        }
        OptionalInt height = HeightText.parse(text);
        if (height.isEmpty()) {
            messages.send(sender, ERRORS + "invalid-height", Map.of("input", text));
        }
        return height;
    }

    // Heights outside the stat's scale min and max are refused, so the player
    // knows how tall or short a character can be. Other stats are always allowed.
    private boolean isAllowedHeight(CommandSender sender, CommandTargets.Target target, StatDefinition stat,
                                    StatAction action, int amount) {
        if (!stat.isHeight()) {
            return true;
        }
        int base = stats.sheet(target.character()).map(sheet -> stats.calculator(sheet).base(stat)).orElse(stat.baseline());
        int height = switch (action) {
            case ADD -> base + amount;
            case REDUCE -> base - amount;
            default -> amount;
        };
        HeightScale scale = stat.scale();
        if (height > scale.max()) {
            messages.send(sender, ERRORS + "too-tall", heightLimit("max", scale.max()));
            return false;
        }
        if (height < scale.min()) {
            messages.send(sender, ERRORS + "too-short", heightLimit("min", scale.min()));
            return false;
        }
        return true;
    }

    private static Map<String, String> heightLimit(String name, int centimetres) {
        return Map.of(name, HeightText.metres(centimetres), name + "_feet", HeightText.feet(centimetres));
    }

    // A whole number; only set may be negative (str can be set to -2).
    private OptionalInt wholeNumber(CommandSender sender, StatAction action, String text) {
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

    private static void number(Map<String, String> placeholders, StatDefinition stat, String name, int value) {
        if (stat.isHeight()) {
            placeholders.put(name, HeightText.metres(value));
            placeholders.put(name + "_feet", HeightText.feet(value));
        } else {
            placeholders.put(name, Integer.toString(value));
        }
    }

    private static boolean hasShield(StatDefinition stat) {
        return stat.isBar() && stat.bar().shield() != null;
    }

    // Tells the player what changed and, when someone else ran the command, them
    // too. A stat uses its own messages (stats.<id>.<action>) when messages.yml has
    // them, otherwise stats.default.<action>. Heights show in metres, and also get
    // {amount_feet}, {value_feet} and {old_feet}.
    private void report(CommandSender sender, Player target, StatChange change, StatAction action, int amount) {
        StatDefinition stat = change.stat();
        String own = STATS + stat.id() + "." + action.configName();
        String base = messages.has(own + ".player") ? own : STATS + DEFAULT + "." + action.configName();
        Map<String, String> placeholders = new HashMap<>();
        number(placeholders, stat, "amount", amount);
        number(placeholders, stat, "value", change.newValue());
        number(placeholders, stat, "old", change.oldValue());
        placeholders.put("player", target.getName());
        placeholders.put("sender", sender.getName());
        placeholders.put("name", stat.display().name());
        placeholders.put("stat", stat.id());
        messages.send(target, base + ".player", placeholders);
        if (!target.equals(sender)) {
            messages.send(sender, base + ".sender", placeholders);
        }
    }
}
