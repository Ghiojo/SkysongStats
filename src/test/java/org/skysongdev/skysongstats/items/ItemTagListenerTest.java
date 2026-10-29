package org.skysongdev.skysongstats.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.Utils.SkysongItemsKeys;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The listener puts the inventory's item tag counts on the player's active
 * character, using the default stats.yml.
 */
class ItemTagListenerTest {

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
    private Player player;
    private ProfileKey character;
    private ItemTagListener listener;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            stats = new StatService(StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true).stats(),
                    NO_STORAGE, change -> { });
        }
        player = server.addPlayer();
        character = new ProfileKey(player.getUniqueId(), "main");
        stats.createCharacter(character);
        listener = new ItemTagListener(stats, p -> Optional.of(new ProfileKey(p.getUniqueId(), "main")));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack tagged(int amount, String tag, int value) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(SkysongItemsKeys.NAMESPACE, tag), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }

    private int value(String stat) {
        return stats.value(character, stat).orElseThrow();
    }

    @Test
    void inventoryTagsAddToTheirStats() {
        player.getInventory().addItem(tagged(3, "arcrot", 2), tagged(2, "fictis", 4));
        listener.recount(player);
        assertEquals(6, value("arcrot"));
        assertEquals(8, value("fictis"));
    }

    @Test
    void removingItemsTakesTheirCountAway() {
        player.getInventory().addItem(tagged(1, "arcrot", 5));
        listener.recount(player);
        player.getInventory().clear();
        listener.recount(player);
        assertEquals(0, value("arcrot"));
    }

    @Test
    void playersWithoutACharacterAreIgnored() {
        Player stranger = server.addPlayer();
        stranger.getInventory().addItem(tagged(1, "arcrot", 5));
        new ItemTagListener(stats, p -> Optional.empty()).recount(stranger);
        listener.recount(stranger);
        assertEquals(0, value("arcrot"), "the other character is untouched");
    }
}
