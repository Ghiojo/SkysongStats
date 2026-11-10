package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.storage.AncestryRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;
import org.skysongdev.skysongstats.storage.StoredAncestryAdjustment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Every character's ancestry, and the one place that puts its adjustments on the
// stats' base values and takes them off again. What a character got is saved as
// it was on the day (ancestries.yml may change later), and a change always starts
// by taking off exactly what was saved, so a character can go from any state to
// any other without losing stat changes made since by commands or other plugins.
//
// The choices of an ancestry can be pending: given the ancestry (and its fixed
// stats:) without picks, the player picks later.
public class AncestryService {

    // Moves a stat's base value; the plugin runs it through the StatService so bars
    // are capped and the usual stat events fire. A stat no longer in stats.yml is
    // skipped.
    public interface BaseValues {
        void adjust(ProfileKey character, String stat, int delta);
    }

    // Told after every change, with the character's ancestry before and after.
    public interface Listener {
        void onAncestryChanged(AncestryChange change);
    }

    public record AncestryChange(ProfileKey character, Optional<StoredAncestry> before,
                                 Optional<StoredAncestry> after) {
    }

    private final AncestryRepository repository;
    private final BaseValues baseValues;
    private final Listener listener;
    private final Map<ProfileKey, StoredAncestry> ancestries = new HashMap<>();
    private AncestryDefinitions definitions;

    public AncestryService(AncestryDefinitions definitions, AncestryRepository repository, BaseValues baseValues,
                           Listener listener) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.baseValues = Objects.requireNonNull(baseValues, "baseValues");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public void load() {
        ancestries.clear();
        ancestries.putAll(repository.loadAll());
    }

    public AncestryDefinitions definitions() {
        return definitions;
    }

    // A reload: characters keep what they were given.
    public void setDefinitions(AncestryDefinitions definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
    }

    public Optional<StoredAncestry> of(ProfileKey character) {
        return Optional.ofNullable(ancestries.get(character));
    }

    // The character's ancestry as ancestries.yml has it now; empty when the
    // character has none, or its ancestry has left the file.
    public Optional<AncestryDefinition> definitionOf(ProfileKey character) {
        return of(character).flatMap(saved -> definitions.get(saved.ancestry()));
    }

    // Every character with an ancestry.
    public Set<ProfileKey> characters() {
        return Set.copyOf(ancestries.keySet());
    }

    // Gives the character this ancestry: takes off every adjustment of the one it
    // had (if any), then puts on the fixed stats: and, with picks, the choices. An
    // ancestry with choices and no picks leaves them pending. Giving the same
    // ancestry again puts its current version on. Throws IllegalArgumentException,
    // changing nothing, when the picks have problems (AncestryRules).
    public AncestryChange set(ProfileKey character, AncestryDefinition ancestry, Optional<AncestryPicks> picks) {
        Objects.requireNonNull(ancestry, "ancestry");
        picks.ifPresent(chosen -> requireValid(ancestry, chosen));
        Optional<StoredAncestry> before = of(character);
        before.ifPresent(saved -> takeOff(character, saved.adjustments()));
        List<StoredAncestryAdjustment> given = new ArrayList<>(AncestryRules.fixedAdjustments(ancestry));
        picks.ifPresent(chosen -> given.addAll(AncestryRules.choiceAdjustments(ancestry, chosen)));
        putOn(character, given);
        boolean pending = ancestry.hasChoices() && picks.isEmpty();
        return save(character, before, new StoredAncestry(ancestry.id(), pending, given));
    }

    // Makes a character's pending choices with these picks. Empty, changing
    // nothing, when the character has no pending choices or its ancestry has left
    // ancestries.yml. Throws IllegalArgumentException when the picks have problems.
    public Optional<AncestryChange> choose(ProfileKey character, AncestryPicks picks) {
        Optional<StoredAncestry> before = of(character).filter(StoredAncestry::choicesPending);
        Optional<AncestryDefinition> ancestry = before.flatMap(saved -> definitions.get(saved.ancestry()));
        if (ancestry.isEmpty()) {
            return Optional.empty();
        }
        requireValid(ancestry.get(), picks);
        List<StoredAncestryAdjustment> given = new ArrayList<>(before.get().adjustments());
        given.addAll(putOn(character, AncestryRules.choiceAdjustments(ancestry.get(), picks)));
        return Optional.of(save(character, before, new StoredAncestry(ancestry.get().id(), false, given)));
    }

    // Takes off what the character's choices gave and makes them pending again;
    // the fixed stats: stay. Empty, changing nothing, when the character has no
    // ancestry, or its ancestry has left ancestries.yml or has no choices.
    public Optional<AncestryChange> rechoose(ProfileKey character) {
        Optional<StoredAncestry> before = of(character);
        if (definitionOf(character).filter(AncestryDefinition::hasChoices).isEmpty()) {
            return Optional.empty();
        }
        List<StoredAncestryAdjustment> fixed = new ArrayList<>();
        List<StoredAncestryAdjustment> chosen = new ArrayList<>();
        before.get().adjustments().forEach(adjustment -> (adjustment.isFixed() ? fixed : chosen).add(adjustment));
        takeOff(character, chosen);
        return Optional.of(save(character, before, new StoredAncestry(before.get().ancestry(), true, fixed)));
    }

    // Takes off every adjustment and leaves the character with no ancestry. Empty
    // when it had none.
    public Optional<AncestryChange> remove(ProfileKey character) {
        Optional<StoredAncestry> before = of(character);
        if (before.isEmpty()) {
            return Optional.empty();
        }
        takeOff(character, before.get().adjustments());
        ancestries.remove(character);
        repository.delete(character);
        AncestryChange change = new AncestryChange(character, before, Optional.empty());
        listener.onAncestryChanged(change);
        return Optional.of(change);
    }

    // For a deleted character: forgets its ancestry without touching stats, which
    // are deleted with it.
    public void deleteCharacter(ProfileKey character) {
        if (ancestries.remove(character) != null) {
            repository.delete(character);
        }
    }

    private static void requireValid(AncestryDefinition ancestry, AncestryPicks picks) {
        List<String> problems = AncestryRules.problems(ancestry, picks);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", problems));
        }
    }

    // Remembers and saves the character's new ancestry; its adjustments are
    // already on the base values.
    private AncestryChange save(ProfileKey character, Optional<StoredAncestry> before, StoredAncestry after) {
        ancestries.put(character, after);
        repository.save(character, after);
        AncestryChange change = new AncestryChange(character, before, Optional.of(after));
        listener.onAncestryChanged(change);
        return change;
    }

    private List<StoredAncestryAdjustment> putOn(ProfileKey character, List<StoredAncestryAdjustment> adjustments) {
        adjustments.forEach(adjustment -> baseValues.adjust(character, adjustment.stat(), adjustment.amount()));
        return adjustments;
    }

    private void takeOff(ProfileKey character, List<StoredAncestryAdjustment> adjustments) {
        adjustments.forEach(adjustment -> baseValues.adjust(character, adjustment.stat(), -adjustment.amount()));
    }
}
