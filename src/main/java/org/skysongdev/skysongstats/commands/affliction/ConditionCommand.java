package org.skysongdev.skysongstats.commands.affliction;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.messages.AfflictionTexts;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// /condition, on the conditions from conditions.yml:
//   /condition list                                the menu of every condition (ConditionMenu)
//   /condition search <text>                       the same menu, only names containing the text
//   /condition award <player> <condition> [level]  gives it directly, at level 1 when
//                                                  none is given; again changes the level
//   /condition remove <player> <condition>         takes back a direct award
// Conditions an injury or another condition gives come and go with what gives
// them. A condition is named by its id, in any case. A character's current
// conditions are on the /stats sheet.
public final class ConditionCommand implements TabExecutor {
    static final String KEYS = "condition.";
    static final String AWARD = "skysongstats.condition.award";
    static final String REMOVE = "skysongstats.condition.remove";
    private static final Pattern LEVEL = Pattern.compile("\\d{1,9}");

    private final AfflictionService afflictions;
    private final ConditionMenu menu;
    private final CommandTargets targets;
    private final Supplier<Messages> messages;
    private final AfflictionTexts texts;
    private final Supplier<List<String>> onlinePlayers;
    private final Consumer<String> log;
    private final ChangeCauses causes;

    // log gets a line for every change staff make, so the console sees who changed what.
    public ConditionCommand(AfflictionService afflictions, ConditionMenu menu, CommandTargets targets,
                            Supplier<Messages> messages, AfflictionTexts texts, Supplier<List<String>> onlinePlayers,
                            Consumer<String> log, ChangeCauses causes) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.texts = Objects.requireNonNull(texts, "texts");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.log = Objects.requireNonNull(log, "log");
        this.causes = Objects.requireNonNull(causes, "causes");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "list" -> menu.openAll(sender);
            case "search" -> search(sender, args);
            case "award" -> award(sender, args);
            case "remove" -> remove(sender, args);
            default -> send(sender, "unknown-subcommand", Map.of());
        }
        return true;
    }

    private void search(CommandSender sender, String... args) {
        if (args.length < 2) {
            send(sender, "search-usage", Map.of());
            return;
        }
        menu.openSearch(sender, String.join(" ", List.of(args).subList(1, args.length)));
    }

    private void award(CommandSender sender, String... args) {
        if (args.length < 3) {
            send(sender, "usage", Map.of());
            return;
        }
        OptionalInt level = level(sender, args.length > 3 ? args[3] : "1");
        if (level.isEmpty()) {
            return;
        }
        targets.resolve(sender, args[1], AWARD, AWARD).ifPresent(target -> condition(sender, args[2])
                .ifPresent(condition -> causes.during(ChangeCause.COMMAND,
                        () -> award(sender, target, condition, level.getAsInt()))));
    }

    private void award(CommandSender sender, CommandTargets.Target target, ConditionDefinition condition, int level) {
        boolean had = afflictions.condition(target.character(), condition.id()).isPresent();
        afflictions.awardCondition(target.character(), condition.id(), level);
        Map<String, Component> tag = Map.of("condition", texts.conditionTag(condition, level));
        messages.get().send(sender, KEYS + (had ? "award.success-updated" : "award.success"), player(target), tag);
        if (!target.player().equals(sender)) {
            messages.get().send(target.player(), KEYS + "award.received", Map.of(), tag);
        }
        log.accept(sender.getName() + " gave condition " + condition.id() + " at level " + level + " to "
                + target.player().getName() + " (" + target.character().profile() + ")");
    }

    private OptionalInt level(CommandSender sender, String typed) {
        if (LEVEL.matcher(typed).matches() && Integer.parseInt(typed) >= 1) {
            return OptionalInt.of(Integer.parseInt(typed));
        }
        send(sender, "invalid-level", Map.of("level", typed));
        return OptionalInt.empty();
    }

    private void remove(CommandSender sender, String... args) {
        if (args.length < 3) {
            send(sender, "usage", Map.of());
            return;
        }
        targets.resolve(sender, args[1], REMOVE, REMOVE).ifPresent(target -> condition(sender, args[2])
                .ifPresent(condition -> causes.during(ChangeCause.COMMAND,
                        () -> remove(sender, target, condition))));
    }

    private void remove(CommandSender sender, CommandTargets.Target target, ConditionDefinition condition) {
        Map<String, Component> tag = Map.of("condition", texts.conditionTag(condition, 1));
        AfflictionService.ConditionRemoval removal = afflictions.removeCondition(target.character(), condition.id());
        switch (removal) {
            case REMOVED -> {
                messages.get().send(sender, KEYS + "remove.success", player(target), tag);
                if (!target.player().equals(sender)) {
                    messages.get().send(target.player(), KEYS + "remove.lost", Map.of(), tag);
                }
            }
            case REMOVED_STILL_GIVEN -> messages.get().send(sender, KEYS + "remove.still-given", player(target), tag);
            default -> messages.get().send(sender, KEYS + "remove.not-direct", player(target), tag);
        }
        if (removal != AfflictionService.ConditionRemoval.NOT_DIRECT) {
            log.accept(sender.getName() + " removed condition " + condition.id() + " from "
                    + target.player().getName() + " (" + target.character().profile() + ")");
        }
    }

    private Optional<ConditionDefinition> condition(CommandSender sender, String id) {
        Optional<ConditionDefinition> condition = afflictions.conditionDefinitions().get(id);
        if (condition.isEmpty()) {
            send(sender, "unknown-condition", Map.of("condition", id));
        }
        return condition;
    }

    private static Map<String, String> player(CommandTargets.Target target) {
        return Map.of("player", target.player().getName());
    }

    private void send(CommandSender to, String key, Map<String, String> placeholders) {
        messages.get().send(to, KEYS + key, placeholders);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions(sender, args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> suggestions(CommandSender sender, String... args) {
        if (args.length == 1) {
            Stream<String> staff = Stream.of("award", "remove")
                    .filter(sub -> sender.hasPermission("skysongstats.condition." + sub));
            return Stream.concat(Stream.of("list", "search"), staff);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean staff = "award".equals(sub) || "remove".equals(sub);
        if (args.length == 2 && staff) {
            return onlinePlayers.get().stream();
        }
        if (args.length == 3 && staff) {
            return afflictions.conditionDefinitions().all().stream().map(ConditionDefinition::id);
        }
        return Stream.empty();
    }
}
