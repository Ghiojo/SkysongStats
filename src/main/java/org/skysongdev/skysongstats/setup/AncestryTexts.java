package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.skysongdev.skysongstats.definition.AncestryAdjustment;
import org.skysongdev.skysongstats.definition.AncestryChoice;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryPackage;
import org.skysongdev.skysongstats.definition.PackageChoice;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatPickChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// How an ancestry's adjustments and choices read on the /setup screens, from the
// setup.ancestry.* keys of messages.yml. Stats are named by their stats.yml name.
final class AncestryTexts {
    private static final String ANCESTRY = "setup.ancestry.";

    private final MenuTexts texts;
    private final StatDefinitions stats;

    AncestryTexts(MenuTexts texts, StatDefinitions stats) {
        this.texts = Objects.requireNonNull(texts, "texts");
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    // +1, -1: the way amounts read everywhere.
    static String signed(int amount) {
        return amount > 0 ? "+" + amount : Integer.toString(amount);
    }

    // A choice's name, or setup.ancestry.default-choice-name with its number
    // (from 1) when ancestries.yml gives none.
    String choiceName(AncestryChoice choice, int index) {
        if (!choice.name().isBlank()) {
            return choice.name();
        }
        return plain(texts.line(ANCESTRY + "default-choice-name", Map.of("number", Integer.toString(index + 1))));
    }

    String statName(String id) {
        return stats.get(id).map(stat -> stat.display().name()).orElse(id);
    }

    // "+2 Strength, +1 Dexterity".
    String adjustments(List<AncestryAdjustment> adjustments) {
        String separator = plain(texts.line(ANCESTRY + "separator", Map.of()));
        return adjustments.stream().map(adjustment -> signed(adjustment.amount()) + " " + statName(adjustment.stat()))
                .collect(Collectors.joining(separator));
    }

    // The generated lore of an ancestry in the list: one line per fixed
    // adjustment, then one per choice.
    List<Component> summary(AncestryDefinition ancestry) {
        List<Component> lines = new ArrayList<>();
        ancestry.adjustments().forEach(adjustment -> lines.add(texts.line(ANCESTRY + "adjustment", Map.of(
                "amount", signed(adjustment.amount()), "stat", statName(adjustment.stat())))));
        for (int index = 0; index < ancestry.choices().size(); index++) {
            lines.add(choiceSummary(ancestry.choices().get(index), index));
        }
        return lines;
    }

    private Component choiceSummary(AncestryChoice choice, int index) {
        String name = choiceName(choice, index);
        if (choice instanceof StatPickChoice picks) {
            Map<String, String> values = Map.of("name", name, "amount", signed(picks.amount()),
                    "picks", Integer.toString(picks.picks()), "stats", statNames(picks.from()));
            return texts.line(ANCESTRY + (picks.anyStat() ? "choice-any" : "choice-stats"), values);
        }
        PackageChoice packages = (PackageChoice) choice;
        String separator = plain(texts.line(ANCESTRY + "package-separator", Map.of()));
        String options = packages.options().stream()
                .map(option -> option.name() + " (" + adjustments(option.adjustments()) + ")")
                .collect(Collectors.joining(separator));
        return texts.line(ANCESTRY + "choice-package", Map.of("name", name, "options", options));
    }

    String packageText(AncestryPackage option) {
        return adjustments(option.adjustments());
    }

    private String statNames(List<String> ids) {
        String separator = plain(texts.line(ANCESTRY + "separator", Map.of()));
        return ids.stream().map(this::statName).collect(Collectors.joining(separator));
    }

    StatDefinition stat(String id) {
        return stats.get(id).orElseThrow(() -> new IllegalStateException("'" + id + "' is not in stats.yml"));
    }

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }
}
