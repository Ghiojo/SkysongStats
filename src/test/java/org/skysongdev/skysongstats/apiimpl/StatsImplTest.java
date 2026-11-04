package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.StatType;
import org.skysongdev.skysongstats.api.StatValues;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedStatRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsImplTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Default");
    private static final CharacterId NOBODY = new CharacterId(UUID.randomUUID(), "Default");
    private static final String STATS = """
            stats:
              con:
                name: "Constitution"
                aliases: [constitution]
                color: "<light_purple>"
                baseline: 2
              hp:
                name: "HP"
                type: bar
                max: "60 + {con} * 10"
                start: max
                shield: temphp
              temphp:
                name: "Temp HP"
                min: 0
            """;

    private final List<ChangeCause> causesSeen = new ArrayList<>();
    private final ChangeCauses causes = new ChangeCauses();
    private StatService service;
    private StatsImpl stats;

    private static StatDefinitions definitions(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(file, name -> true).stats();
    }

    @BeforeEach
    void setUp() {
        service = new StatService(definitions(STATS), new UnsavedStatRepository(line -> { }),
                change -> causesSeen.add(causes.current()));
        service.createCharacter(new ProfileKey(ALICE.player(), ALICE.profile()));
        stats = new StatsImpl(service, new MainThread(() -> true), causes);
    }

    private Stat stat(String name) {
        return stats.find(name).orElseThrow();
    }

    private StatValues values(String stat) {
        return stats.get(ALICE, stat).orElseThrow();
    }

    @Test
    void allListsEveryStatInFileOrder() {
        assertEquals(List.of("con", "hp", "temphp"), stats.all().stream().map(Stat::id).toList());
    }

    @Test
    void aStatCarriesItsSettings() {
        Stat con = stat("con");
        assertEquals("Constitution", con.name());
        assertEquals("<light_purple>", con.color());
        assertEquals(List.of("constitution"), con.aliases());
        assertEquals(StatType.VALUE, con.type());
        assertEquals(Optional.empty(), con.maxId());
        assertEquals(OptionalInt.empty(), con.minimum());

        Stat hp = stat("hp");
        assertTrue(hp.isBar());
        assertEquals(Optional.of("max_hp"), hp.maxId());
        assertEquals(Optional.of("temphp"), hp.shield());
        assertEquals(OptionalInt.of(0), stat("temphp").minimum());
    }

    @Test
    void findMatchesIdsAndAliasesIgnoringCase() {
        assertEquals("con", stat("CONSTITUTION").id());
        assertEquals(Optional.empty(), stats.find("max_hp"));
        assertEquals(Optional.empty(), stats.find("Health"));
    }

    @Test
    void aValueStatHasNoMax() {
        assertEquals(new StatValues(2, 0, 0, null, null), values("con"));
        assertEquals(2, values("con").total());
    }

    @Test
    void aBarHasItsCurrentValueAndMax() {
        StatValues hp = values("hp");

        assertEquals(80, hp.total());
        assertEquals(OptionalInt.of(80), hp.max());
        assertEquals(OptionalInt.of(80), hp.maxBase());
    }

    @Test
    void modifiersShowSeparatelyFromTheBase() {
        ProfileKey alice = new ProfileKey(ALICE.player(), ALICE.profile());
        service.addModifier(alice, "con", 3);
        service.addModifier(alice, "max_hp", 5);

        assertEquals(new StatValues(80, 0, 0, 115, 110), values("hp"), "a saved bar keeps its value when its max grows");
        assertEquals(5, values("con").total());
        assertEquals(3, values("con").modifiers());
    }

    @Test
    void readsAreEmptyForACharacterOrStatThatDoesNotExist() {
        assertEquals(Optional.empty(), stats.get(NOBODY, stat("con")));
        assertEquals(Optional.empty(), stats.get(ALICE, "luck"));
    }

    @Test
    void applyWorksLikeTheStatCommandsAndSaysItIsTheApi() {
        Optional<StatChange> change = stats.apply(ALICE, "con", StatAction.ADD, 3);

        assertEquals(Optional.of(new StatChange(ALICE, stat("con"), 2, 5)), change);
        assertEquals(5, values("con").total());
        assertEquals(List.of(ChangeCause.API), causesSeen);
    }

    @Test
    void setMayBeNegativeAndTheMinimumStillHolds() {
        assertEquals(-4, stats.apply(ALICE, "con", StatAction.SET, -4).orElseThrow().after());
        assertEquals(0, stats.apply(ALICE, "temphp", StatAction.SET, -4).orElseThrow().after());
    }

    @Test
    void aCappedBarStaysAtItsMax() {
        assertEquals(80, stats.apply(ALICE, "hp", StatAction.ADD, 50).orElseThrow().after());
    }

    @Test
    void damageGoesThroughTheShieldFirst() {
        stats.apply(ALICE, "temphp", StatAction.SET, 5);

        StatChange hp = stats.apply(ALICE, "hp", StatAction.DAMAGE, 8).orElseThrow();

        assertEquals(77, hp.after());
        assertEquals(0, values("temphp").total());
    }

    @Test
    void damageOnAStatWithoutAShieldIsRefused() {
        Stat con = stat("con");

        assertThrows(IllegalArgumentException.class, () -> stats.apply(ALICE, con, StatAction.DAMAGE, 1));
    }

    @Test
    void negativeAmountsAreRefusedExceptForSet() {
        Stat con = stat("con");

        assertThrows(IllegalArgumentException.class, () -> stats.apply(ALICE, con, StatAction.ADD, -1));
        assertThrows(IllegalArgumentException.class, () -> stats.apply(ALICE, con, StatAction.REDUCE, -1));
        assertEquals(2, values("con").total());
    }

    @Test
    void writesAreEmptyForACharacterOrStatThatDoesNotExist() {
        assertEquals(Optional.empty(), stats.apply(NOBODY, stat("con"), StatAction.ADD, 1));
        assertEquals(Optional.empty(), stats.apply(ALICE, "luck", StatAction.ADD, 1));
        assertTrue(causesSeen.isEmpty());
    }

    @Test
    void aStatRemovedByAReloadReadsEmpty() {
        Stat hp = stat("hp");
        service.setDefinitions(definitions("stats:\n  con: {}\n"));

        assertEquals(Optional.empty(), stats.get(ALICE, hp));
        assertEquals(Optional.empty(), stats.apply(ALICE, hp, StatAction.ADD, 1));
    }

    @Test
    void characterMethodsRefuseCallsOffTheMainThread() {
        StatsImpl offThread = new StatsImpl(service, new MainThread(() -> false), causes);
        Stat con = stat("con");

        assertThrows(IllegalStateException.class, () -> offThread.get(ALICE, con));
        assertThrows(IllegalStateException.class, () -> offThread.apply(ALICE, con, StatAction.ADD, 1));
        assertEquals(3, offThread.all().size());
        assertTrue(offThread.find("hp").isPresent());
    }

    @Test
    void nullArgumentsAreRefused() {
        Stat con = stat("con");

        assertThrows(NullPointerException.class, () -> stats.find(null));
        assertThrows(NullPointerException.class, () -> stats.get(null, con));
        assertThrows(NullPointerException.class, () -> stats.apply(ALICE, con, null, 1));
    }

    @Test
    void statsAndValuesCompareByContent() {
        assertEquals(stat("hp"), stats.find("hp").orElseThrow());
        assertEquals(stat("hp").hashCode(), stats.find("hp").orElseThrow().hashCode());
        assertNotEquals(stat("hp"), stat("con"));
        assertEquals(values("hp").hashCode(), values("hp").hashCode());
    }
}
