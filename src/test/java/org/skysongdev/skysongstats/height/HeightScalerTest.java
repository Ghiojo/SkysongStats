package org.skysongdev.skysongstats.height;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.attribute.AttributeInstanceMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.character.StatChange;
import org.skysongdev.skysongstats.character.StatChangeListener;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The player's Minecraft scale follows their active character's height.
 */
class HeightScalerTest {
    private static final StatRepository NO_STORAGE = new StatRepository() {
        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, StoredStats> loadAll() {
            return Map.of();
        }

        @Override
        public void saveValue(ProfileKey character, String statId, int value) {
        }

        @Override
        public void saveModifier(ProfileKey character, StoredModifier modifier) {
        }

        @Override
        public void deleteModifier(String modifierId) {
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
        }
    };

    private ServerMock server;
    private StatService stats;
    private HeightScaler scaler;
    private PlayerMock alice;
    private AttributeInstance scale;
    private ProfileKey main;
    // Each player's active character; a player missing here has none.
    private final Map<Player, ProfileKey> active = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        stats = new StatService(defaultStats(), NO_STORAGE, new StatChangeListener() {
            @Override
            public void onChange(StatChange change) {
            }

            @Override
            public void onTotalsChanged(ProfileKey character) {
                scaler.refresh(character);
            }
        });
        alice = server.addPlayer("Alice");
        // MockBukkit's players have no scale attribute.
        scale = new AttributeInstanceMock(Attribute.SCALE, 1.0);
        scaler = new HeightScaler(stats, player -> Optional.ofNullable(active.get(player)), server::getPlayer,
                player -> player.equals(alice) ? scale : null);
        main = new ProfileKey(alice.getUniqueId(), "main");
        stats.createCharacter(main);
        active.put(alice, main);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static StatDefinitions defaultStats() throws Exception {
        try (InputStream in = HeightScalerTest.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true).stats();
        }
    }

    private static StatDefinitions stats(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return StatsConfigLoader.load(config, name -> true).stats();
    }

    private AttributeInstance scale() {
        return scale;
    }

    private void setHeight(ProfileKey character, int centimetres) {
        stats.apply(character, stats.definitions().height().orElseThrow(), StatAction.SET, centimetres);
    }

    @Test
    void theNormalHeightIsScale1() {
        scaler.refresh(alice);
        assertEquals(1.0, scale().getValue(), 1e-9);
    }

    @Test
    void settingTheHeightChangesTheScaleStraightAway() {
        setHeight(main, 360);
        assertEquals(2.0, scale().getValue(), 1e-9);
        setHeight(main, 300);
        // 3 m tall is 3 blocks: scale 1 is 1.8 blocks.
        assertEquals(3.0, scale().getValue() * 1.8, 1e-9);
    }

    @Test
    void modifiersOnTheHeightChangeTheScaleWithinTheLimits() {
        StoredModifier tall = stats.addModifier(main, "height", 90);
        assertEquals(1.5, scale().getValue(), 1e-9);
        stats.addModifier(main, "height", 500);
        assertEquals(2.0, scale().getValue(), 1e-9);
        stats.removeModifier(main, tall.id());
        assertEquals(2.0, scale().getValue(), 1e-9);
    }

    @Test
    void stacksWithOtherScaleChanges() {
        scale().setBaseValue(2.0);
        setHeight(main, 90);
        assertEquals(1.0, scale().getValue(), 1e-9);
    }

    @Test
    void onlyTheActiveCharacterSetsTheScale() {
        ProfileKey alt = new ProfileKey(alice.getUniqueId(), "alt");
        stats.createCharacter(alt);
        setHeight(alt, 360);
        assertNull(scale().getModifier(HeightScaler.MODIFIER));
        active.put(alice, alt);
        scaler.refresh(alice);
        assertEquals(2.0, scale().getValue(), 1e-9);
    }

    @Test
    void aPlayerWithoutACharacterHasNoHeightModifier() {
        setHeight(main, 360);
        active.remove(alice);
        scaler.refresh(alice);
        assertNull(scale().getModifier(HeightScaler.MODIFIER));
        assertEquals(1.0, scale().getValue(), 1e-9);
    }

    @Test
    void removingTheScaleBlockRemovesTheModifier() throws Exception {
        setHeight(main, 360);
        stats.setDefinitions(stats("stats:\n  height:\n    baseline: 180\n"));
        scaler.refresh(alice);
        assertNull(scale().getModifier(HeightScaler.MODIFIER));
    }

    @Test
    void theModifierIsTransientAndRemovedWhenThePluginStops() {
        setHeight(main, 270);
        AttributeModifier modifier = scale().getModifier(HeightScaler.MODIFIER);
        assertNotNull(modifier);
        assertEquals(0.5, modifier.getAmount(), 1e-9);
        assertEquals(new NamespacedKey("skysongstats", "height"), modifier.getKey());
        scaler.remove(alice);
        assertNull(scale().getModifier(HeightScaler.MODIFIER));
    }
}
