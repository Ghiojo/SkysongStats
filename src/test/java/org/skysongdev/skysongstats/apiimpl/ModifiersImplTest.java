package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Modifier;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifiersImplTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Default");
    private static final CharacterId NOBODY = new CharacterId(UUID.randomUUID(), "Default");
    private static final ProfileKey ALICE_KEY = new ProfileKey(ALICE.player(), ALICE.profile());
    private static final String STATS = """
            stats:
              con: {aliases: [constitution]}
              hp: {type: bar, max: "60 + {con} * 10", start: max}
            """;

    private final List<ChangeCause> causesSeen = new ArrayList<>();
    private final ChangeCauses causes = new ChangeCauses();
    private StatService service;
    private ModifiersImpl modifiers;

    private static StatDefinitions definitions() {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(STATS);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(file, name -> true).stats();
    }

    @BeforeEach
    void setUp() {
        service = new StatService(definitions(), new UnsavedStatRepository(line -> { }),
                change -> causesSeen.add(causes.current()));
        service.createCharacter(ALICE_KEY);
        modifiers = new ModifiersImpl(service, new MainThread(() -> true), causes);
    }

    @Test
    void aNewCharacterHasNoModifiers() {
        assertEquals(List.of(), modifiers.list(ALICE));
    }

    @Test
    void addSavesAModifierOnAStatByIdOrAlias() {
        Modifier added = modifiers.add(ALICE, "Constitution", 2).orElseThrow();

        assertEquals("con", added.target());
        assertEquals(2, added.amount());
        assertEquals(List.of(added), modifiers.list(ALICE));
        assertEquals(2, service.value(ALICE_KEY, "con").orElseThrow());
    }

    @Test
    void addSavesAModifierOnABarsMax() {
        Modifier added = modifiers.add(ALICE, "max_hp", -10).orElseThrow();

        assertEquals("max_hp", added.target());
        assertEquals(50, service.value(ALICE_KEY, "max_hp").orElseThrow());
    }

    @Test
    void loweringAMaxBringsACappedBarDownAndSaysItIsTheApi() {
        modifiers.add(ALICE, "max_hp", -10);

        assertEquals(50, service.value(ALICE_KEY, "hp").orElseThrow());
        assertEquals(List.of(ChangeCause.API), causesSeen);
    }

    @Test
    void addIsEmptyForAnUnknownTargetOrCharacter() {
        assertEquals(Optional.empty(), modifiers.add(ALICE, "luck", 1));
        assertEquals(Optional.empty(), modifiers.add(ALICE, "max_con", 1));
        assertEquals(Optional.empty(), modifiers.add(NOBODY, "con", 1));
        assertEquals(List.of(), modifiers.list(ALICE));
    }

    @Test
    void removeTakesOffOnlyThatModifier() {
        Modifier first = modifiers.add(ALICE, "con", 1).orElseThrow();
        Modifier second = modifiers.add(ALICE, "con", 2).orElseThrow();

        assertTrue(modifiers.remove(ALICE, first.id()));
        assertEquals(List.of(second), modifiers.list(ALICE));
        assertFalse(modifiers.remove(ALICE, first.id()));
    }

    @Test
    void removeIsFalseForACharacterThatDoesNotExist() {
        assertFalse(modifiers.remove(NOBODY, "anything"));
    }

    @Test
    void listIsEmptyForACharacterThatDoesNotExistAndCannotBeChanged() {
        modifiers.add(ALICE, "con", 1);
        List<Modifier> list = modifiers.list(ALICE);

        assertEquals(List.of(), modifiers.list(NOBODY));
        assertThrows(UnsupportedOperationException.class, () -> list.add(new Modifier("x", "con", 1)));
    }

    @Test
    void everyMethodRefusesCallsOffTheMainThread() {
        ModifiersImpl offThread = new ModifiersImpl(service, new MainThread(() -> false), causes);

        assertThrows(IllegalStateException.class, () -> offThread.list(ALICE));
        assertThrows(IllegalStateException.class, () -> offThread.add(ALICE, "con", 1));
        assertThrows(IllegalStateException.class, () -> offThread.remove(ALICE, "x"));
    }

    @Test
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> modifiers.list(null));
        assertThrows(NullPointerException.class, () -> modifiers.add(ALICE, null, 1));
        assertThrows(NullPointerException.class, () -> modifiers.remove(ALICE, null));
    }
}
