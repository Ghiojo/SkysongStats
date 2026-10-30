package org.skysongdev.skysongstats.commands.proficiency;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.MessageSink;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.skysongdev.skysongstats.commands.proficiency.ProficiencyArguments.PROFICIENCY;

// /proficiency (alias /prof) on the proficiencies from proficiencies.yml:
//   /proficiency [view] [player]       the proficiency sheet
//   /proficiency focus <proficiency>   picks what to train; asks first when that
//                                      would lose points
//   /proficiency focus confirm         the [Yes] of that question
// and the staff commands in ProficiencyManagement. A proficiency is named by its
// id or one of its aliases, in any case.
public final class ProficiencyCommand implements TabExecutor {
    static final String VIEW = "skysongstats.proficiencies.view";
    static final String VIEW_OTHER = VIEW + ".other";
    static final String FOCUS = "skysongstats.proficiencies.focus";
    private static final String FOCUS_KEYS = PROFICIENCY + "focus.";
    private static final String CONFIRM = "confirm";

    private final ProficiencyService proficiencies;
    private final SkillService skills;
    private final MessageSink messages;
    private final ProficiencySheet sheet;
    private final PendingFocusChanges pending;
    private final Supplier<List<String>> onlinePlayers;
    private final ProficiencyArguments arguments;
    private final ProficiencyManagement management;

    // log gets a line for every change staff make, so the console sees who changed what.
    public ProficiencyCommand(ProficiencyService proficiencies, SkillService skills, CommandTargets targets,
                              MessageSink messages, ProficiencySheet sheet, PendingFocusChanges pending,
                              Supplier<List<String>> onlinePlayers, Consumer<String> log) {
        this.proficiencies = Objects.requireNonNull(proficiencies, "proficiencies");
        this.skills = Objects.requireNonNull(skills, "skills");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.pending = Objects.requireNonNull(pending, "pending");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.arguments = new ProficiencyArguments(proficiencies, skills, targets, messages);
        this.management = new ProficiencyManagement(proficiencies, arguments, messages, log);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "view";
        switch (sub) {
            case "view" -> view(sender, args.length > 1 ? args[1] : null);
            case "focus" -> focus(sender, args);
            case "points" -> management.points(sender, args);
            case "tier" -> management.tier(sender, args);
            default -> messages.send(sender, PROFICIENCY + "invalid-subcommand", Map.of());
        }
        return true;
    }

    private void view(CommandSender sender, String targetName) {
        arguments.target(sender, targetName, VIEW, VIEW_OTHER).ifPresent(target ->
                sheet.send(sender, target.player().getName(), proficiencies, skills.config(), target.character()));
    }

    private void focus(CommandSender sender, String... args) {
        if (args.length < 2) {
            messages.send(sender, FOCUS_KEYS + "usage", Map.of());
            return;
        }
        arguments.target(sender, null, FOCUS, null).ifPresent(target -> {
            if (CONFIRM.equalsIgnoreCase(args[1])) {
                confirm(sender, target.character());
            } else {
                arguments.proficiency(sender, args[1])
                        .ifPresent(proficiency -> requestFocus(sender, target.character(), proficiency));
            }
        });
    }

    private void requestFocus(CommandSender sender, ProfileKey character, ProficiencyDefinition proficiency) {
        Map<String, String> placeholders = Map.of("proficiency", proficiency.name());
        switch (proficiencies.requestFocus(character, proficiency)) {
            case FOCUSED -> messages.send(sender, FOCUS_KEYS + "focused", placeholders);
            case ALREADY_FOCUSED -> messages.send(sender, FOCUS_KEYS + "already-focused", placeholders);
            case MASTERED -> messages.send(sender, FOCUS_KEYS + "mastered", placeholders);
            default -> askToConfirm(sender, character, proficiency);
        }
    }

    // FocusRequest.NEEDS_CONFIRMATION: the current focus has points to lose.
    private void askToConfirm(CommandSender sender, ProfileKey character, ProficiencyDefinition proficiency) {
        ProficiencyFocus current = proficiencies.focus(character).orElseThrow();
        pending.put(character, proficiency.id());
        messages.send(sender, FOCUS_KEYS + "confirm", Map.of("proficiency", proficiency.name(),
                "current", current.proficiency().name(), "points", Integer.toString(current.points())));
    }

    private void confirm(CommandSender sender, ProfileKey character) {
        pending.take(character)
                .flatMap(id -> proficiencies.config().proficiencies().get(id))
                .ifPresentOrElse(proficiency -> changeFocus(sender, character, proficiency),
                        () -> messages.send(sender, FOCUS_KEYS + "nothing-to-confirm", Map.of()));
    }

    // A tier set while the question was open may have mastered it since.
    private void changeFocus(CommandSender sender, ProfileKey character, ProficiencyDefinition proficiency) {
        Map<String, String> placeholders = Map.of("proficiency", proficiency.name());
        if (proficiencies.config().tiers().isTop(proficiencies.tier(character, proficiency))) {
            messages.send(sender, FOCUS_KEYS + "mastered", placeholders);
            return;
        }
        proficiencies.changeFocus(character, proficiency);
        messages.send(sender, FOCUS_KEYS + "focused", placeholders);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions(sender, args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> suggestions(CommandSender sender, String... args) {
        if (args.length == 1) {
            return subcommands(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("view".equals(sub)) {
            return args.length == 2 ? onlinePlayers.get().stream() : Stream.empty();
        }
        if ("focus".equals(sub)) {
            return args.length == 2 ? Stream.concat(Stream.of(CONFIRM), proficiencyIds()) : Stream.empty();
        }
        if (!sender.hasPermission(ProficiencyManagement.MANAGE)) {
            return Stream.empty();
        }
        return "points".equals(sub) ? pointsSuggestions(args) : tierSuggestions(args);
    }

    private Stream<String> subcommands(CommandSender sender) {
        Stream<String> forPlayers = sender.hasPermission(FOCUS) ? Stream.of("view", "focus") : Stream.of("view");
        Stream<String> forStaff = sender.hasPermission(ProficiencyManagement.MANAGE)
                ? Stream.of("points", "tier") : Stream.empty();
        return Stream.concat(forPlayers, forStaff);
    }

    // points add <player> <amount> <skill> <max-tier>, points remove|set <player> <amount>
    private Stream<String> pointsSuggestions(String... args) {
        boolean adding = "add".equalsIgnoreCase(args[1]);
        return switch (args.length) {
            case 2 -> Stream.of("add", "remove", "set");
            case 3 -> onlinePlayers.get().stream();
            case 5 -> adding ? trainedSkillIds() : Stream.empty();
            case 6 -> adding ? tierIds() : Stream.empty();
            default -> Stream.empty();
        };
    }

    // tier set <player> <proficiency> <tier>
    private Stream<String> tierSuggestions(String... args) {
        if (!"tier".equalsIgnoreCase(args[0])) {
            return Stream.empty();
        }
        return switch (args.length) {
            case 2 -> Stream.of("set");
            case 3 -> onlinePlayers.get().stream();
            case 4 -> proficiencyIds();
            case 5 -> tierIds();
            default -> Stream.empty();
        };
    }

    // The skills that have proficiencies to train.
    private Stream<String> trainedSkillIds() {
        return skills.config().skills().all().stream().map(SkillDefinition::id)
                .filter(skill -> !proficiencies.config().proficiencies().ofSkill(skill).isEmpty());
    }

    private Stream<String> proficiencyIds() {
        return proficiencies.config().proficiencies().all().stream().map(ProficiencyDefinition::id);
    }

    private Stream<String> tierIds() {
        return proficiencies.config().tiers().all().stream().map(ProficiencyTier::id);
    }
}
