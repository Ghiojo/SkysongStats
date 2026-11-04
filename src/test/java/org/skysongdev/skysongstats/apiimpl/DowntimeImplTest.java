package org.skysongdev.skysongstats.apiimpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.DowntimeChange;
import org.skysongdev.skysongstats.api.DowntimeLimits;
import org.skysongdev.skysongstats.api.DowntimePool;
import org.skysongdev.skysongstats.api.DowntimeTarget;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.InMemoryDowntimeRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DowntimeImplTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final CharacterId MAGE = new CharacterId(ALICE, "Mage");
    private static final DowntimeTarget GLOBAL = DowntimeTarget.global(ALICE);

    private final List<ChangeCause> causesSeen = new ArrayList<>();
    private final ChangeCauses causes = new ChangeCauses();
    private final Map<UUID, String> active = new HashMap<>();
    private DowntimeService service;
    private DowntimeImpl downtime;

    @BeforeEach
    void setUp() {
        service = new DowntimeService(new InMemoryDowntimeRepository(), new DowntimeSettings(3, 2, 3, 9),
                change -> causesSeen.add(causes.current()));
        Set<ProfileKey> characters = Set.of(new ProfileKey(ALICE, "Default"), new ProfileKey(ALICE, "Mage"));
        active.put(ALICE, "Mage");
        downtime = new DowntimeImpl(service, () -> characters, player -> Optional.ofNullable(active.get(player)),
                new MainThread(() -> true), causes);
    }

    @Test
    void limitsComeFromConfig() {
        assertEquals(new DowntimeLimits(3, 2, 3, 9), downtime.limits());
    }

    @Test
    void newPoolsStartAtTheirConfiguredValues() {
        assertEquals(OptionalInt.of(2), downtime.get(GLOBAL));
        assertEquals(OptionalInt.of(3), downtime.get(DowntimeTarget.character(MAGE)));
    }

    @Test
    void addStopsAtTheMaxAndSaysItIsTheApi() {
        DowntimeChange change = downtime.add(GLOBAL, 20).orElseThrow();

        assertEquals(new DowntimeChange(GLOBAL, DowntimeChange.Kind.ADD, 2, 9), change);
        assertEquals(7, change.delta());
        assertEquals(List.of(ChangeCause.API), causesSeen);
        assertEquals(3, downtime.add(DowntimeTarget.character(MAGE), 5).orElseThrow().after());
    }

    @Test
    void removeStopsAtZero() {
        DowntimeChange change = downtime.remove(DowntimeTarget.character(MAGE), 5).orElseThrow();

        assertEquals(DowntimeChange.Kind.REMOVE, change.kind());
        assertEquals(0, change.after());
        assertEquals(DowntimePool.CHARACTER, change.target().pool());
    }

    @Test
    void spendTakesFromThePreferredPool() {
        assertEquals(Optional.of(false), downtime.spendsGlobal(ALICE));
        assertEquals(DowntimeTarget.character(MAGE), downtime.spend(ALICE, 1).orElseThrow().target());

        assertTrue(downtime.setSpendsGlobal(ALICE, true));
        assertEquals(GLOBAL, downtime.spend(ALICE, 1).orElseThrow().target());
        assertEquals(OptionalInt.of(1), downtime.get(GLOBAL));
    }

    @Test
    void spendingCharacterDowntimeNeedsAnActiveProfile() {
        active.clear();

        assertEquals(Optional.empty(), downtime.spend(ALICE, 1));
    }

    @Test
    void unknownPlayersAndCharactersAreNotCreated() {
        DowntimeTarget stranger = DowntimeTarget.global(STRANGER);
        DowntimeTarget rogue = DowntimeTarget.character(new CharacterId(ALICE, "Rogue"));

        assertEquals(OptionalInt.empty(), downtime.get(stranger));
        assertEquals(OptionalInt.empty(), downtime.get(rogue));
        assertEquals(Optional.empty(), downtime.add(stranger, 1));
        assertEquals(Optional.empty(), downtime.remove(rogue, 1));
        assertEquals(Optional.empty(), downtime.spend(STRANGER, 1));
        assertEquals(Optional.empty(), downtime.spendsGlobal(STRANGER));
        assertFalse(downtime.setSpendsGlobal(STRANGER, true));
        assertTrue(causesSeen.isEmpty());
    }

    @Test
    void negativeAmountsAreRefused() {
        DowntimeTarget mage = DowntimeTarget.character(MAGE);

        assertThrows(IllegalArgumentException.class, () -> downtime.add(GLOBAL, -1));
        assertThrows(IllegalArgumentException.class, () -> downtime.remove(mage, -1));
        assertThrows(IllegalArgumentException.class, () -> downtime.spend(ALICE, -1));
    }

    @Test
    void everyCharacterMethodRefusesCallsOffTheMainThread() {
        DowntimeImpl offThread = new DowntimeImpl(service, Set::of, player -> Optional.empty(),
                new MainThread(() -> false), causes);

        assertThrows(IllegalStateException.class, () -> offThread.get(GLOBAL));
        assertThrows(IllegalStateException.class, () -> offThread.add(GLOBAL, 1));
        assertThrows(IllegalStateException.class, () -> offThread.spend(ALICE, 1));
        assertEquals(new DowntimeLimits(3, 2, 3, 9), offThread.limits());
    }

    @Test
    void targetsNeedTheirOwner() {
        assertThrows(NullPointerException.class, () -> DowntimeTarget.global(null));
        assertThrows(NullPointerException.class, () -> DowntimeTarget.character(null));
        assertThrows(NullPointerException.class, () -> downtime.get(null));
    }
}
