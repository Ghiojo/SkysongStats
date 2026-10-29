package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.character.CharacterSheet;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.StoredModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Stream;

// /modifier on the stats from stats.yml:
//   /modifier add <stat|max_<bar>> <amount> [player]
//   /modifier del|delete <number> [player]     number as shown by view
//   /modifier view [page] [player]             10 per page
// A modifier targets a stat (by id or alias) or a bar's max, e.g. max_potion.
// Acting on someone else needs skysongstats.modifier.<add|del|view>.other, as
// before.
public final class StatModifierCommand implements TabExecutor {
    static final String MODIFIER = "modifier.";
    static final int PAGE_SIZE = 10;
    private static final String USAGE = "/modifier <add|del|view> ...";
    private static final String OTHER = "skysongstats.modifier.%s.other";

    private final StatService stats;
    private final CommandTargets targets;
    private final MessageSink messages;
    private final Supplier<List<String>> onlinePlayers;

    public StatModifierCommand(StatService stats, CommandTargets targets, MessageSink messages,
                               Supplier<List<String>> onlinePlayers) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "add" -> add(sender, args);
            case "del", "delete" -> delete(sender, args);
            case "view" -> view(sender, args);
            default -> usage(sender);
        }
        return true;
    }

    private void add(CommandSender sender, String... args) {
        if (args.length < 3 || args.length > 4) {
            usage(sender);
            return;
        }
        Optional<String> target = modifierTarget(args[1]);
        if (target.isEmpty()) {
            messages.send(sender, MODIFIER + "not-a-target", Map.of("input", args[1]));
            return;
        }
        OptionalInt amount = number(sender, args[2]);
        if (amount.isEmpty()) {
            return;
        }
        targets.resolve(sender, argument(args, 3), null, String.format(OTHER, "add")).ifPresent(character -> {
            StoredModifier modifier = stats.addModifier(character.character(), target.orElseThrow(), amount.getAsInt());
            messages.send(sender, key("added", modifier.target()),
                    placeholders(modifier, character.player().getName(), 0));
        });
    }

    private void delete(CommandSender sender, String... args) {
        if (args.length < 2 || args.length > 3) {
            usage(sender);
            return;
        }
        OptionalInt number = number(sender, args[1]);
        if (number.isEmpty()) {
            return;
        }
        targets.resolve(sender, argument(args, 2), null, String.format(OTHER, "del")).ifPresent(character -> {
            List<StoredModifier> modifiers = modifiers(character);
            int index = number.getAsInt();
            if (index < 1 || index > modifiers.size()) {
                messages.send(sender, MODIFIER + "no-such-modifier", Map.of("index", args[1]));
                return;
            }
            StoredModifier modifier = modifiers.get(index - 1);
            stats.removeModifier(character.character(), modifier.id());
            messages.send(sender, key("removed", modifier.target()),
                    placeholders(modifier, character.player().getName(), index));
        });
    }

    private void view(CommandSender sender, String... args) {
        if (args.length > 3) {
            usage(sender);
            return;
        }
        OptionalInt page = args.length > 1 ? number(sender, args[1]) : OptionalInt.of(1);
        if (page.isEmpty()) {
            return;
        }
        if (page.getAsInt() < 1) {
            messages.send(sender, MODIFIER + "invalid-page", Map.of("input", args[1]));
            return;
        }
        targets.resolve(sender, argument(args, 2), null, String.format(OTHER, "view"))
                .ifPresent(character -> showPage(sender, character, page.getAsInt()));
    }

    private void showPage(CommandSender sender, CommandTargets.Target character, int page) {
        List<StoredModifier> modifiers = modifiers(character);
        String player = character.player().getName();
        Map<String, String> pageInfo = Map.of("player", player, "page", Integer.toString(page));
        messages.send(sender, MODIFIER + "view.header", pageInfo);
        if (modifiers.isEmpty()) {
            messages.send(sender, MODIFIER + "view.empty", pageInfo);
        }
        int end = Math.min(page * PAGE_SIZE, modifiers.size());
        for (int i = (page - 1) * PAGE_SIZE; i < end; i++) {
            StoredModifier modifier = modifiers.get(i);
            messages.send(sender, key("view.line", modifier.target()), placeholders(modifier, player, i + 1));
        }
        messages.send(sender, MODIFIER + "view.footer", pageInfo);
    }

    private List<StoredModifier> modifiers(CommandTargets.Target character) {
        return stats.sheet(character.character()).map(CharacterSheet::modifiers).orElse(List.of());
    }

    // A stat id or alias gives the stat's id; max_<bar> stays as it is.
    private Optional<String> modifierTarget(String input) {
        Optional<String> stat = stats.definitions().find(input).map(StatDefinition::id);
        if (stat.isPresent()) {
            return stat;
        }
        return stats.definitions().findBarByMaxId(input).flatMap(StatDefinition::maxId);
    }

    // Messages about a bar's max use their own key ("...-max"), so the text can
    // say "Max HP" in any language.
    private String key(String message, String target) {
        boolean max = stats.definitions().findBarByMaxId(target).isPresent();
        return MODIFIER + message + (max ? "-max" : "");
    }

    private Map<String, String> placeholders(StoredModifier modifier, String player, int index) {
        Map<String, String> values = new HashMap<>();
        values.put("target", modifier.target());
        values.put("name", displayName(modifier.target()));
        values.put("amount", Integer.toString(modifier.amount()));
        values.put("player", player);
        values.put("index", Integer.toString(index));
        return values;
    }

    // The stat's name, or the bar's name for a max. A stat that has left stats.yml
    // shows its id.
    private String displayName(String target) {
        return stats.definitions().get(target)
                .or(() -> stats.definitions().findBarByMaxId(target))
                .map(stat -> stat.display().name())
                .orElse(target);
    }

    private OptionalInt number(CommandSender sender, String text) {
        try {
            return OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            messages.send(sender, "general.not-a-number", Map.of("input", text));
            return OptionalInt.empty();
        }
    }

    private static String argument(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    private void usage(CommandSender sender) {
        messages.send(sender, StatActionRunner.ERRORS + "usage", Map.of("usage", USAGE));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options(args).filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> options(String... args) {
        if (args.length == 1) {
            return Stream.of("add", "del", "view");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean playerSlot = ("add".equals(sub) && args.length == 4)
                || (List.of("del", "delete", "view").contains(sub) && args.length == 3);
        if (playerSlot) {
            return onlinePlayers.get().stream();
        }
        return "add".equals(sub) && args.length == 2 ? targetNames().stream() : Stream.empty();
    }

    private List<String> targetNames() {
        List<String> names = new ArrayList<>();
        for (StatDefinition stat : stats.definitions().all()) {
            names.add(stat.id());
            stat.maxId().ifPresent(names::add);
        }
        return names;
    }
}
