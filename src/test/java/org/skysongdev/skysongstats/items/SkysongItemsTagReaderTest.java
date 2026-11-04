package org.skysongdev.skysongstats.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongitems.api.ItemTags;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongstats.definition.ItemTagSource;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader hands each stats.yml tag to SkysongItems' sumInt, which adds up the tag's
 * value times the stack size (only on items with charges left when asked). SkysongItems
 * tests that sum itself in its SkysongStatsReadsTest.
 */
class SkysongItemsTagReaderTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // One sumInt call SkysongItems received.
    private record SumCall(Iterable<?> items, String tagId, boolean onlyWithChargesLeft) {
    }

    // An ItemTags that answers sumInt with the given total and remembers the calls.
    private static ItemTags tagsAnswering(int total, List<SumCall> calls) {
        return (ItemTags) Proxy.newProxyInstance(ItemTags.class.getClassLoader(), new Class<?>[]{ItemTags.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("sumInt")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    calls.add(new SumCall((Iterable<?>) args[0], (String) args[1], (Boolean) args[2]));
                    return total;
                });
    }

    @Test
    void aTagIsSummedBySkysongItems() {
        List<SumCall> calls = new ArrayList<>();
        List<ItemStack> items = List.of(new ItemStack(Material.STICK, 3));

        int total = new SkysongItemsTagReader(tagsAnswering(6, calls)).sum(items, new ItemTagSource("arcrot", false));

        assertEquals(6, total);
        assertEquals(List.of(new SumCall(items, "arcrot", false)), calls);
    }

    @Test
    void requiresChargesOnlyCountsItemsWithChargesLeft() {
        List<SumCall> calls = new ArrayList<>();

        new SkysongItemsTagReader(tagsAnswering(0, calls)).sum(List.of(), new ItemTagSource("charged_arcrot", true));

        assertEquals("charged_arcrot", calls.get(0).tagId());
        assertEquals(true, calls.get(0).onlyWithChargesLeft());
    }

    @Test
    void fromServerUsesTheRegisteredSkysongItemsApi() {
        ItemTags tags = tagsAnswering(9, new ArrayList<>());
        SkysongItemsApi api = (SkysongItemsApi) Proxy.newProxyInstance(SkysongItemsApi.class.getClassLoader(),
                new Class<?>[]{SkysongItemsApi.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("tags")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return tags;
                });
        server.getServicesManager().register(SkysongItemsApi.class, api, MockBukkit.createMockPlugin(), ServicePriority.Normal);

        assertEquals(9, SkysongItemsTagReader.fromServer().sum(List.of(), new ItemTagSource("fictis", false)));
    }

    @Test
    void fromServerFailsWhenSkysongItemsIsNotEnabled() {
        IllegalStateException error = assertThrows(IllegalStateException.class, SkysongItemsTagReader::fromServer);
        assertTrue(error.getMessage().contains("SkysongItems is not enabled"), error.getMessage());
    }
}
