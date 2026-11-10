package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.messages.SheetPart;
import org.skysongdev.skysongstats.placeholders.StatPlaceholders;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// The stat sheet /stats view shows. Its lines are stats.sheet in messages.yml,
// where {player} and the stat placeholders ({str}, {str_base}, {str_modifiers},
// {hp}, {max_hp}, {max_hp_base}...) are filled in. A stat the lines never mention, such as one
// just added to stats.yml, still shows: it gets a stats.sheet-auto line after
// them, unless stats.sheet-hidden lists its id (the height, which shows on the
// character sheet instead). stats.sheet-footer closes the sheet. Other plugins' %placeholders%
// (PlaceholderAPI) in any of these lines are filled in for the sheet's player.
public final class StatSheet {
    static final String SHEET = "stats.sheet";
    static final String AUTO_VALUE = "stats.sheet-auto.value";
    static final String AUTO_BAR = "stats.sheet-auto.bar";
    static final String FOOTER = "stats.sheet-footer";
    static final String HIDDEN = "stats.sheet-hidden";
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
        for (SheetPart part : parts(target.character(), target.player().getName())) {
            messages.send(to, part.key(), part.placeholders(), target.player());
        }
    }

    // The sheet's keys in order, filled in for the character, with {player} set to
    // name: stats.sheet, a sheet-auto line for each stat it doesn't show, the footer.
    public List<SheetPart> parts(ProfileKey character, String name) {
        Map<String, String> values = placeholders(character, name);
        List<SheetPart> parts = new ArrayList<>();
        parts.add(new SheetPart(SHEET, values));
        Set<String> shown = mentioned(messages.lines(SHEET));
        Set<String> hidden = hidden();
        for (StatDefinition stat : stats.definitions().all()) {
            if (!hidden.contains(stat.id()) && names(stat).noneMatch(shown::contains)) {
                parts.add(new SheetPart(stat.isBar() ? AUTO_BAR : AUTO_VALUE, autoLine(stat, values)));
            }
        }
        parts.add(new SheetPart(FOOTER, values));
        return parts;
    }

    // {player} (set to name) and every stat placeholder of the character.
    public Map<String, String> placeholders(ProfileKey character, String name) {
        StatPlaceholders placeholders = new StatPlaceholders(stats);
        Map<String, String> values = new HashMap<>();
        values.put("player", name);
        stats.definitions().all().stream().flatMap(StatSheet::names).forEach(stat ->
                placeholders.resolve(character, stat).ifPresent(value -> values.put(stat, value)));
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
            line.put("max_base", values.get(max + BASE));
            line.put("max_modifiers", values.get(max + MODIFIERS));
        });
        return line;
    }

    // The placeholder names a stat has.
    private static Stream<String> names(StatDefinition stat) {
        Stream<String> own = Stream.of(stat.id(), stat.id() + BASE, stat.id() + MODIFIERS);
        return Stream.concat(own, stat.maxId().stream().flatMap(max -> Stream.of(max, max + BASE, max + MODIFIERS)));
    }

    private Set<String> hidden() {
        Set<String> ids = new HashSet<>();
        messages.lines(HIDDEN).forEach(id -> ids.add(id.trim().toLowerCase(Locale.ROOT)));
        return ids;
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
