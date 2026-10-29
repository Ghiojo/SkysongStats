package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.placeholders.StatPlaceholders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// The stat sheet /stats view shows. Its lines are stats.sheet in messages.yml,
// where {player} and the stat placeholders ({str}, {str_base}, {str_modifiers},
// {hp}, {max_hp}...) are filled in. A stat the lines never mention, such as one
// just added to stats.yml, still shows: it gets a stats.sheet-auto line after
// them. stats.sheet-footer closes the sheet.
public final class StatSheet {
    static final String SHEET = "stats.sheet";
    static final String AUTO_VALUE = "stats.sheet-auto.value";
    static final String AUTO_BAR = "stats.sheet-auto.bar";
    static final String FOOTER = "stats.sheet-footer";
    private static final String BASE = "_base";
    private static final String MODIFIERS = "_modifiers";
    // The same placeholder names messages.yml fills in.
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z0-9_-]+)}");

    private final StatService stats;
    private final MessageSink messages;

    public StatSheet(StatService stats, MessageSink messages) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void show(CommandSender to, CommandTargets.Target target) {
        Map<String, String> values = values(target);
        messages.send(to, SHEET, values);
        Set<String> shown = mentioned(messages.lines(SHEET));
        for (StatDefinition stat : stats.definitions().all()) {
            if (names(stat).noneMatch(shown::contains)) {
                messages.send(to, stat.isBar() ? AUTO_BAR : AUTO_VALUE, autoLine(stat, values));
            }
        }
        messages.send(to, FOOTER, values);
    }

    // {player} and every stat placeholder of the character.
    private Map<String, String> values(CommandTargets.Target target) {
        StatPlaceholders placeholders = new StatPlaceholders(stats);
        Map<String, String> values = new HashMap<>();
        values.put("player", target.player().getName());
        stats.definitions().all().stream().flatMap(StatSheet::names).forEach(name ->
                placeholders.resolve(target.character(), name).ifPresent(value -> values.put(name, value)));
        return values;
    }

    private static Map<String, String> autoLine(StatDefinition stat, Map<String, String> values) {
        Map<String, String> line = new HashMap<>(values);
        line.put("stat", stat.id());
        line.put("name", stat.display().name());
        line.put("value", values.get(stat.id()));
        line.put("base", values.get(stat.id() + BASE));
        line.put("modifiers", values.get(stat.id() + MODIFIERS));
        stat.maxId().ifPresent(max -> {
            line.put("max", values.get(max));
            line.put("max_modifiers", values.get(max + MODIFIERS));
        });
        return line;
    }

    // The placeholder names a stat has.
    private static Stream<String> names(StatDefinition stat) {
        Stream<String> own = Stream.of(stat.id(), stat.id() + BASE, stat.id() + MODIFIERS);
        return Stream.concat(own, stat.maxId().stream().flatMap(max -> Stream.of(max, max + MODIFIERS)));
    }

    private static Set<String> mentioned(List<String> lines) {
        Set<String> names = new HashSet<>();
        for (String line : lines) {
            Matcher matcher = PLACEHOLDER.matcher(line);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }
}
