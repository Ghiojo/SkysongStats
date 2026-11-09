package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.definition.InjuryDefinitions;
import org.skysongdev.skysongstats.storage.AfflictionRepository;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAfflictions;
import org.skysongdev.skysongstats.storage.StoredCondition;
import org.skysongdev.skysongstats.storage.StoredKnownInjury;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

// Every character's injuries, directly given conditions and known injuries
// ("afflictions" is the code's word for injuries and conditions together), and
// the conditions those bring. Changes are saved through the repository and
// reported to the listener. Kept in memory, loaded once at startup, like
// StatService. Call it from the server thread only.
//
// Saved ids that injuries.yml or conditions.yml no longer have stay saved but
// are left out of every answer, and come back if the id does.
public class AfflictionService {
    private final AfflictionRepository repository;
    private final AfflictionChangeListener listener;
    private final LongSupplier clock;
    private final Map<ProfileKey, Afflictions> characters = new HashMap<>();
    private volatile ConditionDefinitions conditions;
    private volatile InjuryDefinitions injuries;

    // What one character has, as saved.
    private static final class Afflictions {
        private final Map<String, Long> injuries = new LinkedHashMap<>();
        private final Map<String, StoredCondition> conditions = new LinkedHashMap<>();
        private final Map<String, StoredKnownInjury> known = new LinkedHashMap<>();
    }

    public enum DiscoverResult { UNKNOWN_INJURY, ALREADY_KNOWN, LEARNT }

    public enum ForgetResult { UNKNOWN_INJURY, NOT_KNOWN, STILL_INJURED, FORGOTTEN }

    public enum ConditionRemoval { UNKNOWN_CONDITION, NOT_DIRECT, REMOVED, REMOVED_STILL_GIVEN }

    // clock gives the time saved with each award, in epoch millis.
    public AfflictionService(ConditionDefinitions conditions, InjuryDefinitions injuries,
                             AfflictionRepository repository, AfflictionChangeListener listener, LongSupplier clock) {
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.injuries = Objects.requireNonNull(injuries, "injuries");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void load() {
        characters.clear();
        repository.loadAll().forEach((key, stored) -> characters.put(key, copy(stored)));
    }

    private static Afflictions copy(StoredAfflictions stored) {
        Afflictions afflictions = new Afflictions();
        stored.injuries().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> afflictions.injuries.put(entry.getKey(), entry.getValue()));
        afflictions.conditions.putAll(stored.conditions());
        afflictions.known.putAll(stored.known());
        return afflictions;
    }

    public ConditionDefinitions conditionDefinitions() {
        return conditions;
    }

    public InjuryDefinitions injuryDefinitions() {
        return injuries;
    }

    // Swaps in reloaded conditions.yml and injuries.yml. Characters whose current
    // conditions change with the new files (an injury that now gives another
    // condition, a condition removed from the file) are reported to the listener.
    public void setDefinitions(ConditionDefinitions conditions, InjuryDefinitions injuries) {
        Map<ProfileKey, List<ActiveCondition>> before = new HashMap<>();
        characters.keySet().forEach(key -> before.put(key, conditions(key)));
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.injuries = Objects.requireNonNull(injuries, "injuries");
        before.forEach(this::reportConditions);
    }

    // The character's current injuries, oldest first.
    public List<CharacterInjury> injuries(ProfileKey character) {
        Afflictions afflictions = characters.get(character);
        if (afflictions == null) {
            return List.of();
        }
        List<CharacterInjury> current = new ArrayList<>();
        afflictions.injuries.forEach((id, awardedAt) ->
                injuries.get(id).ifPresent(injury -> current.add(new CharacterInjury(injury, awardedAt))));
        return current;
    }

    public boolean hasInjury(ProfileKey character, String injuryId) {
        return injuries(character).stream().anyMatch(injury -> injury.injury().id().equals(injuryId));
    }

    // Gives the character an injury and teaches it to them. Awarding one it already
    // has keeps the one, with the new time. Empty when injuries.yml has no such id.
    public Optional<InjuryAward> awardInjury(ProfileKey character, String injuryId) {
        return injuries.get(injuryId).map(injury -> award(character, injury));
    }

    private InjuryAward award(ProfileKey character, InjuryDefinition injury) {
        List<ActiveCondition> before = conditions(character);
        Afflictions afflictions = characters.computeIfAbsent(character, key -> new Afflictions());
        long now = clock.getAsLong();
        boolean isNew = afflictions.injuries.remove(injury.id()) == null;
        afflictions.injuries.put(injury.id(), now);
        repository.saveInjury(character, injury.id(), now);
        boolean learnt = learn(character, injury, LearnSource.AWARD);
        InjuryAward award = new InjuryAward(character, new CharacterInjury(injury, now), isNew, learnt);
        listener.onInjuryAwarded(award);
        if (learnt) {
            listener.onInjuryLearnt(character, injury, LearnSource.AWARD);
        }
        reportConditions(character, before);
        return award;
    }

    // Removes an injury, and with it the conditions it gives. The character still
    // knows it. False when the character doesn't have it.
    public boolean removeInjury(ProfileKey character, String injuryId) {
        Optional<InjuryDefinition> injury = injuries.get(injuryId).filter(found -> hasInjury(character, found.id()));
        injury.ifPresent(found -> {
            List<ActiveCondition> before = conditions(character);
            characters.get(character).injuries.remove(found.id());
            repository.deleteInjury(character, found.id());
            listener.onInjuryRemoved(character, found);
            reportConditions(character, before);
        });
        return injury.isPresent();
    }

    // The ids of the injuries the character knows, in the order it learnt them.
    public Set<String> known(ProfileKey character) {
        Afflictions afflictions = characters.get(character);
        if (afflictions == null) {
            return Set.of();
        }
        Set<String> known = new LinkedHashSet<>();
        afflictions.known.keySet().stream().filter(id -> injuries.get(id).isPresent()).forEach(known::add);
        return known;
    }

    public boolean knows(ProfileKey character, String injuryId) {
        return injuries.get(injuryId).map(injury -> known(character).contains(injury.id())).orElse(false);
    }

    // Teaches the character an injury without giving it.
    public DiscoverResult discover(ProfileKey character, String injuryId, LearnSource source) {
        return injuries.get(injuryId).map(injury -> {
            if (!learn(character, injury, source)) {
                return DiscoverResult.ALREADY_KNOWN;
            }
            listener.onInjuryLearnt(character, injury, source);
            return DiscoverResult.LEARNT;
        }).orElse(DiscoverResult.UNKNOWN_INJURY);
    }

    // Undoes a discover, for staff mistakes. Refused while the character has the injury.
    public ForgetResult forget(ProfileKey character, String injuryId) {
        return injuries.get(injuryId).map(InjuryDefinition::id).map(id -> {
            if (!knows(character, id)) {
                return ForgetResult.NOT_KNOWN;
            }
            if (hasInjury(character, id)) {
                return ForgetResult.STILL_INJURED;
            }
            characters.get(character).known.remove(id);
            repository.deleteKnownInjury(character, id);
            return ForgetResult.FORGOTTEN;
        }).orElse(ForgetResult.UNKNOWN_INJURY);
    }

    // The character's current conditions, in conditions.yml order: the direct
    // ones, the ones its injuries give, and the ones those give.
    public List<ActiveCondition> conditions(ProfileKey character) {
        Afflictions afflictions = characters.get(character);
        if (afflictions == null) {
            return List.of();
        }
        List<InjuryDefinition> current = injuries(character).stream().map(CharacterInjury::injury).toList();
        return new ConditionResolver(conditions).resolve(afflictions.conditions, current);
    }

    public Optional<ActiveCondition> condition(ProfileKey character, String conditionId) {
        return conditions.get(conditionId).flatMap(found -> conditions(character).stream()
                .filter(active -> active.id().equals(found.id())).findFirst());
    }

    // Gives the character a condition directly, at a level of 1 or more. Giving it
    // again changes the level. Empty when conditions.yml has no such id.
    public Optional<ActiveCondition> awardCondition(ProfileKey character, String conditionId, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("A condition's level is at least 1, got " + level);
        }
        return conditions.get(conditionId).map(ConditionDefinition::id).flatMap(id -> {
            List<ActiveCondition> before = conditions(character);
            StoredCondition stored = new StoredCondition(level, clock.getAsLong());
            characters.computeIfAbsent(character, key -> new Afflictions()).conditions.put(id, stored);
            repository.saveCondition(character, id, stored);
            reportConditions(character, before);
            return condition(character, id);
        });
    }

    // Removes a direct award. A condition an injury or another condition also gives
    // stays (REMOVED_STILL_GIVEN); one that is only given can't be removed (NOT_DIRECT).
    public ConditionRemoval removeCondition(ProfileKey character, String conditionId) {
        return conditions.get(conditionId).map(ConditionDefinition::id)
                .map(id -> removeDirect(character, id))
                .orElse(ConditionRemoval.UNKNOWN_CONDITION);
    }

    private ConditionRemoval removeDirect(ProfileKey character, String id) {
        Afflictions afflictions = characters.get(character);
        if (afflictions == null || !afflictions.conditions.containsKey(id)) {
            return ConditionRemoval.NOT_DIRECT;
        }
        List<ActiveCondition> before = conditions(character);
        afflictions.conditions.remove(id);
        repository.deleteCondition(character, id);
        reportConditions(character, before);
        return condition(character, id).isPresent() ? ConditionRemoval.REMOVED_STILL_GIVEN : ConditionRemoval.REMOVED;
    }

    // Removes everything of a character, for deleted profiles. Nothing is reported:
    // the character is gone.
    public void deleteCharacter(ProfileKey character) {
        characters.remove(character);
        repository.deleteCharacter(character);
    }

    // True when the character didn't know it yet.
    private boolean learn(ProfileKey character, InjuryDefinition injury, LearnSource source) {
        Afflictions afflictions = characters.computeIfAbsent(character, key -> new Afflictions());
        if (afflictions.known.containsKey(injury.id())) {
            return false;
        }
        StoredKnownInjury known = new StoredKnownInjury(clock.getAsLong(), source);
        afflictions.known.put(injury.id(), known);
        repository.saveKnownInjury(character, injury.id(), known);
        return true;
    }

    private void reportConditions(ProfileKey character, List<ActiveCondition> before) {
        List<ConditionChange> changes = ConditionDiff.between(character, before, conditions(character), conditions);
        if (!changes.isEmpty()) {
            listener.onConditionsChanged(character, changes);
        }
    }
}
