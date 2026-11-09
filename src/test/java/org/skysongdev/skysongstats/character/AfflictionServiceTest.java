package org.skysongdev.skysongstats.character;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.SqlAfflictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.character.AfflictionFixtures.conditions;
import static org.skysongdev.skysongstats.character.AfflictionFixtures.injuries;

/**
 * {@link AfflictionService} on the shared fixtures, saving to an in-memory H2
 * database so saving and loading back is checked too. The clock goes up by one
 * on every read.
 */
class AfflictionServiceTest {
    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProfileKey ALICE_ALT = new ProfileKey(ALICE.player(), "alt");

    private final AtomicLong clock = new AtomicLong(100);
    private final List<String> events = new ArrayList<>();
    private SqlAfflictionRepository repository;
    private AfflictionService service;

    private final AfflictionChangeListener listener = new AfflictionChangeListener() {
        @Override
        public void onInjuryAwarded(InjuryAward award) {
            events.add("awarded " + award.injury().injury().id() + (award.isNew() ? " new" : " again")
                    + (award.learnt() ? " learnt" : ""));
        }

        @Override
        public void onInjuryRemoved(ProfileKey character, InjuryDefinition injury) {
            events.add("removed " + injury.id());
        }

        @Override
        public void onInjuryLearnt(ProfileKey character, InjuryDefinition injury, LearnSource source) {
            events.add("learnt " + injury.id() + " by " + source);
        }

        @Override
        public void onConditionsChanged(ProfileKey character, List<ConditionChange> changes) {
            changes.forEach(change -> events.add("condition " + describe(change)));
        }
    };

    private static String describe(ConditionChange change) {
        String id = change.after().or(change::before).orElseThrow().id();
        if (change.started()) {
            return id + " started at " + change.after().orElseThrow().level();
        }
        if (change.stopped()) {
            return id + " stopped";
        }
        return id + " now " + change.after().orElseThrow().level();
    }

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlAfflictionRepository(dataSource);
        repository.createTables();
        service = newService();
        service.load();
    }

    private AfflictionService newService() {
        return new AfflictionService(conditions(), injuries(), repository, listener, clock::incrementAndGet);
    }

    // Each current condition as id:level, in conditions.yml order.
    private List<String> conditionsOf(ProfileKey character, AfflictionService from) {
        return from.conditions(character).stream().map(condition -> condition.id() + ":" + condition.level()).toList();
    }

    private List<String> conditionsOf(ProfileKey character) {
        return conditionsOf(character, service);
    }

    private List<String> injuriesOf(ProfileKey character, AfflictionService from) {
        return from.injuries(character).stream().map(injury -> injury.injury().id()).toList();
    }

    @Test
    void aNewCharacterHasNothing() {
        assertEquals(List.of(), service.injuries(ALICE));
        assertEquals(List.of(), service.conditions(ALICE));
        assertTrue(service.known(ALICE).isEmpty());
    }

    @Test
    void anAwardGivesTheInjuryTeachesItAndReportsItsConditions() {
        InjuryAward award = service.awardInjury(ALICE, "broken-leg").orElseThrow();

        assertTrue(award.isNew());
        assertTrue(award.learnt());
        assertEquals(101, award.injury().awardedAt());
        assertTrue(service.hasInjury(ALICE, "broken-leg"));
        assertTrue(service.knows(ALICE, "broken-leg"));
        assertEquals(List.of("weakened:1", "dazed:2"), conditionsOf(ALICE));
        assertEquals(List.of("awarded broken-leg new learnt", "learnt broken-leg by AWARD",
                "condition weakened started at 1", "condition dazed started at 2"), events);
    }

    @Test
    void awardingAgainKeepsOneWithTheNewTime() {
        service.awardInjury(ALICE, "cut");
        events.clear();
        InjuryAward again = service.awardInjury(ALICE, "cut").orElseThrow();

        assertFalse(again.isNew());
        assertFalse(again.learnt());
        assertEquals(List.of("cut"), injuriesOf(ALICE, service));
        assertEquals(again.injury().awardedAt(), service.injuries(ALICE).get(0).awardedAt());
        assertEquals(List.of("awarded cut again"), events);
    }

    @Test
    void anUnknownInjuryIsNotAwarded() {
        assertTrue(service.awardInjury(ALICE, "missing").isEmpty());
        assertEquals(List.of(), events);
    }

    @Test
    void idsAreFoundInAnyCase() {
        assertTrue(service.awardInjury(ALICE, "CUT").isPresent());
        assertTrue(service.hasInjury(ALICE, "cut"));
        assertTrue(service.knows(ALICE, "Cut"));
    }

    @Test
    void injuriesAreListedOldestFirst() {
        service.awardInjury(ALICE, "cut");
        service.awardInjury(ALICE, "bruise");
        service.awardInjury(ALICE, "cut");
        assertEquals(List.of("bruise", "cut"), injuriesOf(ALICE, service));
    }

    @Test
    void theHighestLevelOfEverySourceCounts() {
        service.awardInjury(ALICE, "cut");
        service.awardInjury(ALICE, "deep-cut");
        assertEquals(List.of("bleeding:3"), conditionsOf(ALICE));
        ActiveCondition bleeding = service.condition(ALICE, "bleeding").orElseThrow();
        assertEquals(List.of("cut", "deep-cut"), bleeding.fromInjuries());
        assertFalse(bleeding.direct());

        events.clear();
        service.removeInjury(ALICE, "deep-cut");
        assertEquals(List.of("bleeding:2"), conditionsOf(ALICE));
        assertEquals(List.of("removed deep-cut", "condition bleeding now 2"), events);
    }

    @Test
    void removingAnInjuryRemovesItsConditionsButKeepsItKnown() {
        service.awardInjury(ALICE, "broken-leg");
        events.clear();

        assertTrue(service.removeInjury(ALICE, "broken-leg"));

        assertFalse(service.hasInjury(ALICE, "broken-leg"));
        assertTrue(service.knows(ALICE, "broken-leg"));
        assertEquals(List.of(), conditionsOf(ALICE));
        assertEquals(List.of("removed broken-leg", "condition weakened stopped", "condition dazed stopped"), events);
    }

    @Test
    void removingAnInjuryTheCharacterDoesntHaveDoesNothing() {
        assertFalse(service.removeInjury(ALICE, "cut"));
        assertFalse(service.removeInjury(ALICE, "missing"));
        assertEquals(List.of(), events);
    }

    @Test
    void aDirectConditionGivesItsConditionsToo() {
        ActiveCondition weakened = service.awardCondition(ALICE, "weakened", 4).orElseThrow();

        assertTrue(weakened.direct());
        assertEquals(4, weakened.level());
        assertEquals(List.of("weakened:4", "dazed:2"), conditionsOf(ALICE));
        assertEquals(List.of("weakened"), service.condition(ALICE, "dazed").orElseThrow().fromConditions());
    }

    @Test
    void awardingAConditionAgainChangesItsLevel() {
        service.awardCondition(ALICE, "bleeding", 1);
        events.clear();
        service.awardCondition(ALICE, "bleeding", 5);
        assertEquals(List.of("bleeding:5"), conditionsOf(ALICE));
        assertEquals(List.of("condition bleeding now 5"), events);
    }

    @Test
    void aConditionLevelIsAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> service.awardCondition(ALICE, "bleeding", 0));
        assertTrue(service.awardCondition(ALICE, "missing", 1).isEmpty());
    }

    @Test
    void onlyDirectConditionsCanBeRemoved() {
        service.awardInjury(ALICE, "cut");
        assertEquals(AfflictionService.ConditionRemoval.NOT_DIRECT, service.removeCondition(ALICE, "bleeding"));
        assertEquals(AfflictionService.ConditionRemoval.UNKNOWN_CONDITION, service.removeCondition(ALICE, "missing"));

        service.awardCondition(ALICE, "bleeding", 5);
        assertEquals(AfflictionService.ConditionRemoval.REMOVED_STILL_GIVEN, service.removeCondition(ALICE, "bleeding"));
        assertEquals(List.of("bleeding:2"), conditionsOf(ALICE));

        service.awardCondition(ALICE, "dazed", 1);
        assertEquals(AfflictionService.ConditionRemoval.REMOVED, service.removeCondition(ALICE, "dazed"));
        assertEquals(List.of("bleeding:2"), conditionsOf(ALICE));
    }

    @Test
    void discoverTeachesWithoutInjuring() {
        assertEquals(AfflictionService.DiscoverResult.LEARNT, service.discover(ALICE, "cut", LearnSource.DISCOVER));
        assertEquals(AfflictionService.DiscoverResult.ALREADY_KNOWN, service.discover(ALICE, "cut", LearnSource.API));
        assertEquals(AfflictionService.DiscoverResult.UNKNOWN_INJURY,
                service.discover(ALICE, "missing", LearnSource.API));

        assertFalse(service.hasInjury(ALICE, "cut"));
        assertEquals(List.of("learnt cut by DISCOVER"), events);
    }

    @Test
    void forgetIsRefusedWhileInjured() {
        service.awardInjury(ALICE, "cut");
        assertEquals(AfflictionService.ForgetResult.STILL_INJURED, service.forget(ALICE, "cut"));
        service.removeInjury(ALICE, "cut");
        assertEquals(AfflictionService.ForgetResult.FORGOTTEN, service.forget(ALICE, "cut"));
        assertEquals(AfflictionService.ForgetResult.NOT_KNOWN, service.forget(ALICE, "cut"));
        assertEquals(AfflictionService.ForgetResult.UNKNOWN_INJURY, service.forget(ALICE, "missing"));
        assertFalse(service.knows(ALICE, "cut"));
    }

    @Test
    void profilesAreSeparateCharacters() {
        service.awardInjury(ALICE, "cut");
        assertEquals(List.of(), service.injuries(ALICE_ALT));
        assertFalse(service.knows(ALICE_ALT, "cut"));
    }

    @Test
    void everythingIsSavedAndLoadsBack() {
        service.awardInjury(ALICE, "cut");
        service.awardCondition(ALICE, "dazed", 3);
        service.discover(ALICE, "bruise", LearnSource.DISCOVER);

        AfflictionService loaded = newService();
        loaded.load();

        assertEquals(List.of("cut"), injuriesOf(ALICE, loaded));
        assertEquals(List.of("bleeding:2", "dazed:3"), conditionsOf(ALICE, loaded));
        assertEquals(List.of("cut", "bruise"), List.copyOf(loaded.known(ALICE)));
    }

    @Test
    void givenConditionsAreNotSaved() {
        service.awardInjury(ALICE, "broken-leg");
        assertTrue(repository.loadAll().get(ALICE).conditions().isEmpty());
    }

    @Test
    void deleteCharacterRemovesEverything() {
        service.awardInjury(ALICE, "cut");
        service.awardInjury(ALICE_ALT, "bruise");
        service.deleteCharacter(ALICE);

        assertEquals(List.of(), service.injuries(ALICE));
        assertFalse(service.knows(ALICE, "cut"));
        assertNull(repository.loadAll().get(ALICE));
        assertEquals(List.of("bruise"), injuriesOf(ALICE_ALT, service));
    }

    @Test
    void aReloadReportsConditionsThatChangeButNotRewordedOnes() {
        service.awardInjury(ALICE, "cut");
        service.awardCondition(ALICE, "dazed", 1);
        events.clear();
        ConditionDefinitions reworded = conditions("""
                conditions:
                  bleeding: {name: "Blood loss", description: "Bleeds"}
                  weakened: {}
                  dazed: {name: "Confused"}
                """);
        service.setDefinitions(reworded, injuries(AfflictionFixtures.INJURIES_FILE, reworded));
        assertEquals(List.of(), events);

        ConditionDefinitions changed = conditions("conditions:\n  bleeding: {}\n");
        service.setDefinitions(changed, injuries("""
                severities: [minor]
                injuries:
                  cut: {severity: minor, effects: {conditions: ["bleeding:4"]}}
                """, changed));

        assertEquals(List.of("condition bleeding now 4", "condition dazed stopped"), events);
        assertEquals(List.of("bleeding:4"), conditionsOf(ALICE));
    }

    @Test
    void idsGoneFromTheFilesAreKeptAndComeBack() {
        service.awardInjury(ALICE, "bruise");
        ConditionDefinitions conditions = conditions();
        service.setDefinitions(conditions, injuries("severities: [minor]\ninjuries: {}\n", conditions));
        assertEquals(List.of(), service.injuries(ALICE));
        assertFalse(service.knows(ALICE, "bruise"));

        service.setDefinitions(conditions, injuries());
        assertEquals(List.of("bruise"), injuriesOf(ALICE, service));
        assertTrue(service.knows(ALICE, "bruise"));
    }
}
