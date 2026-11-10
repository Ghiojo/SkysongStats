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
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listener puts the inventory's item tag counts on the player's active
 * character, using the default stats.yml.
 */
class ItemTagListenerTest {
    // Where SkysongItems stores a tag on an item: skysong_items:<tag id>.
    private static final String SKYSONG_ITEMS = "skysong_items";

    // Stands in for SkysongItems' sumInt: value times stack size (charges are tested there).
    private static final ItemTagReader READER = (List<ItemStack> items, ItemTagSource source) -> items.stream()
            .filter(item -> item != null && !item.getType().isAir())
            .mapToInt(item -> item.getPersistentDataContainer().getOrDefault(
                    new NamespacedKey(SKYSONG_ITEMS, source.tag()), PersistentDataType.INTEGER, 0) * item.getAmount())
            .sum();

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
    // What the other plugins' item sources hold for the player.
    private final List<ItemStack> sourceItems = new ArrayList<>();

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
        listener = new ItemTagListener(stats, p -> Optional.of(new ProfileKey(p.getUniqueId(), "main")), READER,
                p -> sourceItems);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack tagged(int amount, String tag, int value) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(SKYSONG_ITEMS, tag), PersistentDataType.INTEGER, value);
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
        new ItemTagListener(stats, p -> Optional.empty(), READER, p -> List.of()).recount(stranger);
        listener.recount(stranger);
        assertEquals(0, value("arcrot"), "the other character is untouched");
    }

    @Test
    void itemSourcesCountWithTheInventory() {
        player.getInventory().addItem(tagged(1, "arcrot", 2));
        sourceItems.add(tagged(2, "arcrot", 3));
        sourceItems.add(null);
        listener.recount(player);
        assertEquals(8, value("arcrot"));
    }

    @Test
    void itemsLeavingASourceStopCountingOnTheNextRecount() {
        sourceItems.add(tagged(1, "fictis", 4));
        listener.recount(player);
        sourceItems.clear();
        listener.recount(player);
        assertEquals(0, value("fictis"));
    }

    @Test
    void sourcesAreNotAskedForPlayersWithoutACharacter() {
        Player stranger = server.addPlayer();
        List<Player> asked = new ArrayList<>();
        new ItemTagListener(stats, p -> Optional.empty(), READER, p -> {
            asked.add(p);
            return List.of();
        }).recount(stranger);
        assertEquals(List.of(), asked);
    }

    @Test
    void countPutsTheGivenItemsOnACharacterWithoutAPlayer() {
        ProfileKey guard = new ProfileKey(UUID.randomUUID(), "npc");
        stats.createCharacter(guard);
        List<ItemStack> items = new ArrayList<>(List.of(tagged(3, "arcrot", 2)));
        items.add(null);

        listener.count(guard, items);

        assertEquals(6, stats.value(guard, "arcrot").orElseThrow());
        assertEquals(0, value("arcrot"));
    }

    @Test
    void countReplacesTheLastCount() {
        ProfileKey guard = new ProfileKey(UUID.randomUUID(), "npc");
        stats.createCharacter(guard);
        listener.count(guard, List.of(tagged(3, "arcrot", 2)));

        listener.count(guard, List.of(tagged(1, "arcrot", 2)));

        assertEquals(2, stats.value(guard, "arcrot").orElseThrow());
    }

    @Test
    void countDoesNothingForACharacterWithoutStats() {
        ProfileKey nobody = new ProfileKey(UUID.randomUUID(), "npc");
        List<ItemStack> items = List.of(tagged(3, "arcrot", 2));

        assertDoesNotThrow(() -> listener.count(nobody, items));
        assertTrue(stats.sheet(nobody).isEmpty());
    }
}

