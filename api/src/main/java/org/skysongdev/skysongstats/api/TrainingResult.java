package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What a training lesson did, and the focused proficiency with its tier and points
 * afterwards.
 *
 * <p>Two results are equal when every part is equal.
 */
public final class TrainingResult {

    /** What a training lesson did. */
    public enum Outcome {
        /** The points were added and the focus stays at its tier. */
        ADDED,
        /** The points reached the next tier: the tier went up and the points are back at 0. */
        TIER_UP,
        /** Nothing added: the character has no focus. */
        NO_FOCUS,
        /** Nothing added: the focus belongs to another skill than the training. */
        OTHER_SKILL,
        /** Nothing added: the focus is already at or above the trainer's maximum tier. */
        AT_MAX_TIER
    }

    private final Outcome outcome;
    private final Proficiency proficiency;
    private final Tier tier;
    private final int points;

    /**
     * @param outcome     what the lesson did
     * @param proficiency the focused proficiency, null when the character has no focus
     * @param tier        its tier afterwards, null when the character has no focus
     * @param points      its points afterwards
     */
    public TrainingResult(@NotNull Outcome outcome, @Nullable Proficiency proficiency, @Nullable Tier tier,
                          int points) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.proficiency = proficiency;
        this.tier = tier;
        this.points = points;
    }

    /** What the lesson did. */
    public @NotNull Outcome outcome() {
        return outcome;
    }

    /** Whether points were added: {@link Outcome#ADDED} or {@link Outcome#TIER_UP}. */
    public boolean added() {
        return outcome == Outcome.ADDED || outcome == Outcome.TIER_UP;
    }

    /** The focused proficiency, empty when the character has no focus. */
    public @NotNull Optional<Proficiency> proficiency() {
        return Optional.ofNullable(proficiency);
    }

    /** The focused proficiency's tier afterwards, empty when the character has no focus. */
    public @NotNull Optional<Tier> tier() {
        return Optional.ofNullable(tier);
    }

    /** The focus's points afterwards, 0 after a tier up or without a focus. */
    public int points() {
        return points;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TrainingResult result && parts().equals(result.parts());
    }

    @Override
    public int hashCode() {
        return parts().hashCode();
    }

    private List<Object> parts() {
        return Arrays.asList(outcome, proficiency, tier, points);
    }

    @Override
    public String toString() {
        return "TrainingResult[" + outcome + ", " + proficiency + ", " + tier + ", points=" + points + "]";
    }
}
