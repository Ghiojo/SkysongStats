package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.character.AncestryPicks.PackagePick;
import org.skysongdev.skysongstats.character.AncestryPicks.Pick;
import org.skysongdev.skysongstats.character.AncestryPicks.StatsPick;
import org.skysongdev.skysongstats.definition.AncestryAdjustment;
import org.skysongdev.skysongstats.definition.AncestryChoice;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.PackageChoice;
import org.skysongdev.skysongstats.definition.StatPickChoice;
import org.skysongdev.skysongstats.storage.StoredAncestryAdjustment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The rules of an ancestry's choices, and the adjustments an ancestry gives for a
// set of picks. No stacking: a pick of a choice without stack: true can't move a
// stat the same way (up for a boost, down for a negative) as the ancestry's fixed
// stats: or another choice without stack: true. Choices with stack: true are
// exempt both ways. Moving a stat the other way is always fine.
public final class AncestryRules {
    private AncestryRules() {
    }

    // Why the picks can't be used for this ancestry, in English for logs and
    // developers; empty when every choice is picked and the picks follow the rules.
    public static List<String> problems(AncestryDefinition ancestry, AncestryPicks picks) {
        List<String> problems = new ArrayList<>();
        List<AncestryChoice> choices = ancestry.choices();
        for (int index : picks.all().keySet()) {
            if (index < 0 || index >= choices.size()) {
                problems.add("choice " + (index + 1) + " doesn't exist, " + ancestry.id() + " has "
                        + choices.size() + " choice(s)");
            }
        }
        for (int index = 0; index < choices.size(); index++) {
            AncestryChoice choice = choices.get(index);
            Optional<Pick> pick = picks.get(index);
            String which = "choice " + (index + 1) + " of " + ancestry.id();
            if (pick.isEmpty()) {
                problems.add(which + " is not picked");
            } else if (choice instanceof StatPickChoice stats) {
                checkStatPick(ancestry, picks, index, stats, pick.get(), which, problems);
            } else if (choice instanceof PackageChoice packages) {
                checkPackagePick(ancestry, picks, index, packages, pick.get(), which, problems);
            }
        }
        return problems;
    }

    // Whether moving stat by amount through this choice would break the no
    // stacking rule, given the other choices' picks. The choice's own picks don't
    // count, so the choices page can tell which stats are still open.
    public static boolean conflicts(AncestryDefinition ancestry, AncestryPicks picks, int choice, String stat,
                                    int amount) {
        if (ancestry.choices().get(choice).stack()) {
            return false;
        }
        boolean boost = amount > 0;
        return ancestry.adjustments().stream().anyMatch(fixed -> movesSameWay(fixed, stat, boost))
                || picks.all().entrySet().stream()
                .filter(other -> other.getKey() != choice && countsForStacking(ancestry, other.getKey()))
                .flatMap(other -> adjustments(ancestry.choices().get(other.getKey()), other.getValue()).stream())
                .anyMatch(given -> movesSameWay(given, stat, boost));
    }

    // Another choice's picks count for the no stacking rule unless it may stack.
    private static boolean countsForStacking(AncestryDefinition ancestry, int index) {
        return index >= 0 && index < ancestry.choices().size() && !ancestry.choices().get(index).stack();
    }

    private static boolean movesSameWay(AncestryAdjustment adjustment, String stat, boolean boost) {
        return adjustment.stat().equals(stat) && adjustment.isBoost() == boost;
    }

    // The ancestry's fixed stats: as saved adjustments.
    public static List<StoredAncestryAdjustment> fixedAdjustments(AncestryDefinition ancestry) {
        return ancestry.adjustments().stream()
                .map(adjustment -> new StoredAncestryAdjustment(adjustment.stat(), adjustment.amount(),
                        StoredAncestryAdjustment.FIXED))
                .toList();
    }

    // What the picks give, choice by choice, as saved adjustments. The picks must
    // have no problems.
    public static List<StoredAncestryAdjustment> choiceAdjustments(AncestryDefinition ancestry, AncestryPicks picks) {
        List<StoredAncestryAdjustment> given = new ArrayList<>();
        for (int index = 0; index < ancestry.choices().size(); index++) {
            String source = StoredAncestryAdjustment.choice(index + 1);
            int choice = index;
            picks.get(index).ifPresent(pick -> adjustments(ancestry.choices().get(choice), pick).forEach(adjustment ->
                    given.add(new StoredAncestryAdjustment(adjustment.stat(), adjustment.amount(), source))));
        }
        return given;
    }

    // What the ancestry gives each stat in total with these picks, by stat id.
    public static Map<String, Integer> totals(AncestryDefinition ancestry, AncestryPicks picks) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        List<StoredAncestryAdjustment> all = new ArrayList<>(fixedAdjustments(ancestry));
        all.addAll(choiceAdjustments(ancestry, picks));
        all.forEach(adjustment -> totals.merge(adjustment.stat(), adjustment.amount(), Integer::sum));
        return totals;
    }

    private static List<AncestryAdjustment> adjustments(AncestryChoice choice, Pick pick) {
        if (choice instanceof StatPickChoice stats && pick instanceof StatsPick picked) {
            return picked.stats().stream().map(stat -> new AncestryAdjustment(stat, stats.amount())).toList();
        }
        if (choice instanceof PackageChoice packages && pick instanceof PackagePick picked
                && picked.option() >= 0 && picked.option() < packages.options().size()) {
            return packages.options().get(picked.option()).adjustments();
        }
        return List.of();
    }

    private static void checkStatPick(AncestryDefinition ancestry, AncestryPicks picks, int index,
                                      StatPickChoice choice, Pick pick, String which, List<String> problems) {
        if (!(pick instanceof StatsPick picked)) {
            problems.add(which + " picks stats, not a package");
            return;
        }
        if (picked.stats().size() != choice.picks()) {
            problems.add(which + " needs " + choice.picks() + " stat(s), got " + picked.stats().size());
        }
        Set<String> seen = new HashSet<>();
        for (String stat : picked.stats()) {
            if (!seen.add(stat)) {
                problems.add(which + " picks '" + stat + "' twice");
            } else if (!choice.from().contains(stat)) {
                problems.add(which + " can't pick '" + stat + "', only " + choice.from());
            } else if (conflicts(ancestry, picks, index, stat, choice.amount())) {
                problems.add(which + " can't pick '" + stat + "': this ancestry already "
                        + (choice.isBoost() ? "raises" : "lowers") + " it");
            }
        }
    }

    private static void checkPackagePick(AncestryDefinition ancestry, AncestryPicks picks, int index,
                                         PackageChoice choice, Pick pick, String which, List<String> problems) {
        if (!(pick instanceof PackagePick picked)) {
            problems.add(which + " picks a package, not stats");
            return;
        }
        if (picked.option() < 0 || picked.option() >= choice.options().size()) {
            problems.add(which + " has no package " + (picked.option() + 1) + ", it has " + choice.options().size());
            return;
        }
        for (AncestryAdjustment adjustment : choice.options().get(picked.option()).adjustments()) {
            if (conflicts(ancestry, picks, index, adjustment.stat(), adjustment.amount())) {
                problems.add(which + " can't take package '" + choice.options().get(picked.option()).name()
                        + "': this ancestry already " + (adjustment.isBoost() ? "raises" : "lowers")
                        + " '" + adjustment.stat() + "'");
            }
        }
    }
}
