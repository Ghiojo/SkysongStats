package org.skysongdev.skysongstats.commands.proficiency;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.Training;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.MessageSink;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.skysongdev.skysongstats.commands.proficiency.ProficiencyArguments.PROFICIENCY;

// The staff side of /proficiency, all behind skysongstats.proficiencies.manage:
//   points add <player> <amount> <skill> <max-tier>   training, from a business
//   points remove <player> <amount>
//   points set <player> <amount>
//   tier set <player> <proficiency> <tier>
// Training only counts when the player's focus belongs to the skill and is below
// the maximum tier; otherwise both the sender and the player are told why.
final class ProficiencyManagement {
    static final String MANAGE = "skysongstats.proficiencies.manage";
    private static final String POINTS = PROFICIENCY + "points.";
    private static final String TIER = PROFICIENCY + "tier.";

    private final ProficiencyService proficiencies;
    private final ProficiencyArguments arguments;
    private final MessageSink messages;
    private final Consumer<String> log;

    ProficiencyManagement(ProficiencyService proficiencies, ProficiencyArguments arguments, MessageSink messages,
                          Consumer<String> log) {
        this.proficiencies = Objects.requireNonNull(proficiencies, "proficiencies");
        this.arguments = Objects.requireNonNull(arguments, "arguments");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.log = Objects.requireNonNull(log, "log");
    }

    // args: "points", then the action and its arguments.
    void points(CommandSender sender, String[] args) {
        if (!allowed(sender)) {
            return;
        }
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "set" -> set(sender, args);
            default -> messages.send(sender, POINTS + "usage", Map.of());
        }
    }

    // args: "tier", "set", player, proficiency, tier.
    void tier(CommandSender sender, String[] args) {
        if (!allowed(sender)) {
            return;
        }
        if (args.length < 5 || !"set".equalsIgnoreCase(args[1])) {
            messages.send(sender, TIER + "usage", Map.of());
            return;
        }
        target(sender, args[2]).ifPresent(target -> arguments.proficiency(sender, args[3]).ifPresent(proficiency ->
                arguments.tier(sender, args[4]).ifPresent(tier -> setTier(sender, target, proficiency, tier))));
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length < 6) {
            messages.send(sender, POINTS + "add.usage", Map.of());
            return;
        }
        target(sender, args[2]).ifPresent(target -> arguments.amount(sender, args[3], 1).ifPresent(amount ->
                arguments.skill(sender, args[4]).ifPresent(skill -> arguments.tier(sender, args[5]).ifPresent(max ->
                        train(sender, target, new TrainingOrder(amount, skill, max))))));
    }

    private record TrainingOrder(int amount, SkillDefinition skill, ProficiencyTier maxTier) {
    }

    private void train(CommandSender sender, CommandTargets.Target target, TrainingOrder order) {
        ProfileKey character = target.character();
        Training training = proficiencies.train(character, order.amount(), order.skill().id(), order.maxTier());
        Map<String, String> placeholders = trainingPlaceholders(target, order, training);
        String key = POINTS + "add." + training.outcome().name().toLowerCase(Locale.ROOT).replace('_', '-');
        tell(sender, target.player(), key, placeholders);
        if (training.outcome() == Training.Outcome.TIER_UP && proficiencies.config().tiers().isTop(training.tier())) {
            messages.send(target.player(), POINTS + "add.mastered", placeholders);
        }
        log.accept("Training of " + order.amount() + " " + order.skill().id() + " points up to "
                + order.maxTier().id() + " for " + target.player().getName() + ": " + training.outcome());
    }

    private Map<String, String> trainingPlaceholders(CommandTargets.Target target, TrainingOrder order,
                                                    Training training) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.player().getName());
        placeholders.put("amount", Integer.toString(order.amount()));
        placeholders.put("skill", order.skill().name());
        placeholders.put("max-tier", order.maxTier().name());
        if (training.proficiency() != null) {
            ProficiencyFocus after = new ProficiencyFocus(training.proficiency(), training.points());
            placeholders.put("proficiency", training.proficiency().name());
            placeholders.put("tier", training.tier().name());
            placeholders.put("points", Integer.toString(training.points()));
            placeholders.put("cost", Integer.toString(proficiencies.pointsNeeded(target.character(), after)));
        }
        return placeholders;
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, POINTS + "remove.usage", Map.of());
            return;
        }
        target(sender, args[2]).ifPresent(target -> arguments.amount(sender, args[3], 1).ifPresent(amount ->
                proficiencies.removePoints(target.character(), amount)
                        .ifPresentOrElse(after -> reportPoints(sender, target, after, "remove.removed"),
                                () -> noFocus(sender, target))));
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, POINTS + "set.usage", Map.of());
            return;
        }
        target(sender, args[2]).ifPresent(target -> arguments.amount(sender, args[3], 0).ifPresent(points ->
                withFocus(sender, target).ifPresent(focus -> setPoints(sender, target, focus, points))));
    }

    private void setPoints(CommandSender sender, CommandTargets.Target target, ProficiencyFocus focus, int points) {
        int needed = proficiencies.pointsNeeded(target.character(), focus);
        if (points >= needed) {
            messages.send(sender, POINTS + "set.too-high", Map.of("proficiency", focus.proficiency().name(),
                    "max", Integer.toString(needed - 1)));
            return;
        }
        ProficiencyFocus after = proficiencies.setPoints(target.character(), points).orElseThrow();
        reportPoints(sender, target, after, "set.set");
    }

    private void reportPoints(CommandSender sender, CommandTargets.Target target, ProficiencyFocus after, String key) {
        messages.send(sender, POINTS + key, Map.of("player", target.player().getName(),
                "proficiency", after.proficiency().name(),
                "points", Integer.toString(after.points()),
                "cost", Integer.toString(proficiencies.pointsNeeded(target.character(), after))));
        log.accept("Set " + target.player().getName() + "'s " + after.proficiency().id() + " focus points to "
                + after.points());
    }

    private Optional<ProficiencyFocus> withFocus(CommandSender sender, CommandTargets.Target target) {
        Optional<ProficiencyFocus> focus = proficiencies.focus(target.character());
        if (focus.isEmpty()) {
            noFocus(sender, target);
        }
        return focus;
    }

    private void noFocus(CommandSender sender, CommandTargets.Target target) {
        messages.send(sender, POINTS + "no-focus", Map.of("player", target.player().getName()));
    }

    private void setTier(CommandSender sender, CommandTargets.Target target, ProficiencyDefinition proficiency,
                         ProficiencyTier tier) {
        proficiencies.setTier(target.character(), proficiency, tier);
        tell(sender, target.player(), TIER + "set", Map.of("player", target.player().getName(),
                "proficiency", proficiency.name(), "tier", tier.name()));
        log.accept("Set " + target.player().getName() + "'s " + proficiency.id() + " to " + tier.id());
    }

    // Sends key to the sender and key-target to the player, once when they are
    // the same person.
    private void tell(CommandSender sender, Player target, String key, Map<String, String> placeholders) {
        messages.send(sender, key, placeholders);
        if (!target.equals(sender)) {
            messages.send(target, key + "-target", placeholders);
        }
    }

    private Optional<CommandTargets.Target> target(CommandSender sender, String name) {
        return arguments.target(sender, name, null, null);
    }

    private boolean allowed(CommandSender sender) {
        if (sender.hasPermission(MANAGE)) {
            return true;
        }
        messages.send(sender, "general.no-permission", Map.of("permission", MANAGE));
        return false;
    }
}
