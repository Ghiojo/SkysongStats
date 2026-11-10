package org.skysongdev.skysongstats.character;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// What was picked for an ancestry's choices, by the choice's index in
// ancestries.yml (from 0): stat ids for a stat-pick choice, the package's index
// (from 0) for a package choice. Immutable; with() gives a copy with one more pick.
public final class AncestryPicks {
    public sealed interface Pick permits StatsPick, PackagePick {
    }

    public record StatsPick(List<String> stats) implements Pick {
        public StatsPick {
            stats = List.copyOf(stats);
        }
    }

    public record PackagePick(int option) implements Pick {
    }

    private static final AncestryPicks NONE = new AncestryPicks(Map.of());

    private final Map<Integer, Pick> picks;

    private AncestryPicks(Map<Integer, Pick> picks) {
        this.picks = Map.copyOf(picks);
    }

    public static AncestryPicks none() {
        return NONE;
    }

    public AncestryPicks withStats(int choice, List<String> stats) {
        return with(choice, new StatsPick(stats));
    }

    public AncestryPicks withPackage(int choice, int option) {
        return with(choice, new PackagePick(option));
    }

    // A copy without the pick for this choice.
    public AncestryPicks without(int choice) {
        Map<Integer, Pick> copy = new LinkedHashMap<>(picks);
        copy.remove(choice);
        return new AncestryPicks(copy);
    }

    public Optional<Pick> get(int choice) {
        return Optional.ofNullable(picks.get(choice));
    }

    // Every pick, by choice index.
    public Map<Integer, Pick> all() {
        return picks;
    }

    private AncestryPicks with(int choice, Pick pick) {
        Objects.requireNonNull(pick, "pick");
        Map<Integer, Pick> copy = new LinkedHashMap<>(picks);
        copy.put(choice, pick);
        return new AncestryPicks(copy);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AncestryPicks that && picks.equals(that.picks);
    }

    @Override
    public int hashCode() {
        return picks.hashCode();
    }

    @Override
    public String toString() {
        return "AncestryPicks" + picks;
    }
}
