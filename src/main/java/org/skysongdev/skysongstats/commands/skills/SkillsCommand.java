package org.skysongdev.skysongstats.commands.skills;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.SkillChange;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.MessageSink;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /skills on the skills from skills.yml:
//   /skills [view] [player]               opens the skills book
//   /skills addxp <skill> <xp> [player]   needs skysongstats.addxp
//   /skills setxp <skill> <xp> [player]   needs skysongstats.setxp
// A skill is named by its id or one of its aliases, in any case. The console
// must name a player for addxp and setxp, and can't open the book.
public final class SkillsCommand implements TabExecutor {
    static final String SKILLS = "skills.";
    private static final List<String> SUBCOMMANDS = List.of("addxp", "setxp", "view");

    private final SkillService skills;
    private final CommandTargets targets;
    private final MessageSink messages;
    private final SkillBook book;
    private final Supplier<List<String>> onlinePlayers;
    private final Consumer<String> log;

    // log gets a line for every XP change, so the console sees who changed what.
    public SkillsCommand(SkillService skills, CommandTargets targets, MessageSink messages, SkillBook book,
                         Supplier<List<String>> onlinePlayers, Consumer<String> log) {
        this.skills = Objects.requireNonNull(skills, "skills");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.book = Objects.requireNonNull(book, "book");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "view";
        switch (sub) {
            case "view" -> view(sender, argument(args, 1));
            case "addxp" -> changeXp(sender, args, false);
            case "setxp" -> changeXp(sender, args, true);
            default -> messages.send(sender, SKILLS + "invalid-subcommand", Map.of());
        }
        return true;
    }

    private void view(CommandSender sender, String targetName) {
        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "general.players-only", Map.of());
            return;
        }
        targets.resolve(sender, targetName, null, null).ifPresent(target ->
                book.open(viewer, skills.config(), skill -> skills.xp(target.character(), skill).orElse(0)));
    }

    // args: the subcommand, skill, xp and an optional player. The permission is
    // skysongstats.<subcommand>.
    private void changeXp(CommandSender sender, String[] args, boolean set) {
        String name = set ? "setxp" : "addxp";
        if (!allowed(sender, "skysongstats." + name)) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, SKILLS + name + ".usage", Map.of());
            return;
        }
        skill(sender, args[1]).ifPresent(skill -> xp(sender, args[2]).ifPresent(xp ->
                targets.resolve(sender, argument(args, 3), null, null)
                        .ifPresent(target -> apply(sender, target, skill, xp, set))));
    }

    private void apply(CommandSender sender, CommandTargets.Target target, SkillDefinition skill, int xp, boolean set) {
        ProfileKey character = target.character();
        SkillChange change = set ? skills.setXp(character, skill, xp) : skills.addXp(character, skill, xp);
        report(sender, target.player(), change, xp, set);
    }

    private Optional<SkillDefinition> skill(CommandSender sender, String input) {
        Optional<SkillDefinition> skill = skills.config().skills().find(input);
        if (skill.isEmpty()) {
            messages.send(sender, SKILLS + "invalid-skill", Map.of());
        }
        return skill;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "general.no-permission", Map.of("permission", permission));
        return false;
    }

    private void report(CommandSender sender, Player target, SkillChange change, int xp, boolean set) {
        Map<String, String> placeholders = Map.of("xp", Integer.toString(xp), "player", target.getName(),
                "skill", change.skill().name());
        if (set) {
            messages.send(sender, SKILLS + "setxp.set", placeholders);
            log.accept("Set " + target.getName() + "'s " + change.skill().id() + " XP to " + change.after());
            return;
        }
        messages.send(sender, SKILLS + "addxp.added", placeholders);
        if (!target.equals(sender)) {
            messages.send(target, SKILLS + "addxp.gained", placeholders);
        }
        log.accept("Added " + xp + " XP to " + target.getName() + "'s " + change.skill().id()
                + ", now " + change.after());
    }

    private OptionalInt xp(CommandSender sender, String input) {
        OptionalInt xp = parseXp(input);
        if (xp.isEmpty()) {
            messages.send(sender, SKILLS + "invalid-xp", Map.of());
        }
        return xp;
    }

    // XP typed in a command: a whole number of 0 or more.
    private static OptionalInt parseXp(String input) {
        try {
            int xp = Integer.parseInt(input);
            return xp < 0 ? OptionalInt.empty() : OptionalInt.of(xp);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private static String argument(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions(args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> suggestions(String... args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream();
        }
        boolean changesXp = !"view".equalsIgnoreCase(args[0]);
        if (changesXp && args.length == 2) {
            return skills.config().skills().all().stream().map(SkillDefinition::id);
        }
        boolean namesPlayer = changesXp ? args.length == 4 : args.length == 2;
        return namesPlayer ? onlinePlayers.get().stream() : Stream.empty();
    }
}
