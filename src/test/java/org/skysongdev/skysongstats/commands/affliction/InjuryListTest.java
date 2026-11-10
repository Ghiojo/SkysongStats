package org.skysongdev.skysongstats.commands.affliction;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.menu.ListMenu;
import org.skysongdev.skysongstats.storage.LearnSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** /injury list and /injury search on the shared affliction fixtures. */
class InjuryListTest extends AfflictionCommandFixture {
    @Override
    protected String commandName() {
        return "injury";
    }

    private void know(String... ids) {
        for (String id : ids) {
            afflictions.discover(character(alice), id, LearnSource.DISCOVER);
        }
    }

    // The names of the items between the borders.
    private List<String> shown() {
        Inventory inventory = alice.getOpenInventory().getTopInventory();
        assertInstanceOf(ListMenu.class, inventory.getHolder());
        List<String> names = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                names.add(plain(item.getItemMeta().displayName()));
            }
        }
        return names;
    }

    private String title() {
        return plain(alice.getOpenInventory().title());
    }

    @Test
    void showsTheKnownInjuriesInFileOrder() {
        know("cut", "broken-leg");
        run(alice, "list");
        assertEquals("Known Injuries", title());
        assertEquals(List.of("Broken Leg", "Cut"), shown());
    }

    @Test
    void theSeeAllPermissionShowsEveryInjury() {
        know("cut");
        alice.addAttachment(plugin, InjuryMenu.SEE_ALL_PERMISSION, true);
        run(alice, "list");
        assertEquals(List.of("Broken Leg", "Cut", "Deep Cut", "Bruise"), shown());
        assertEquals(Set.of("cut"), afflictions.known(character(alice)), "seeing teaches nothing");
    }

    @Test
    void theSeeAllPermissionKeepsTheFilters() {
        alice.addAttachment(plugin, InjuryMenu.SEE_ALL_PERMISSION, true);
        run(alice, "list", "minor");
        assertEquals(List.of("Cut", "Bruise"), shown());
        run(alice, "search", "cut");
        assertEquals(List.of("Cut", "Deep Cut"), shown());
    }

    @Test
    void theSeeAllPermissionIsForOpsByDefault() {
        assertFalse(alice.hasPermission(InjuryMenu.SEE_ALL_PERMISSION));
        alice.setOp(true);
        assertTrue(alice.hasPermission(InjuryMenu.SEE_ALL_PERMISSION));
    }

    @Test
    void eachItemShowsTheDescriptionIdAndSeverity() {
        know("broken-leg");
        run(alice, "list");
        List<Component> lore = alice.getOpenInventory().getTopInventory().getItem(9).getItemMeta().lore();
        assertEquals(List.of("Snapped", "ID: broken-leg", "Severity: severe"), lore.stream().map(AfflictionCommandFixture::plain).toList());
    }

    @Test
    void viewFiltersBySeverity() {
        know("cut", "broken-leg", "bruise");
        run(alice, "list", "MINOR");
        assertEquals("Known Injuries: minor", title());
        assertEquals(List.of("Cut", "Bruise"), shown());
    }

    @Test
    void anUnknownSeverityIsRefused() {
        run(alice, "list", "deadly");
        assertEquals("[SkysongStats] There is no injury severity called deadly!", next(alice));
    }

    @Test
    void searchMatchesNamesIgnoringCase() {
        know("cut", "deep-cut", "bruise");
        run(alice, "search", "CUT");
        assertEquals("Injuries matching CUT", title());
        assertEquals(List.of("Cut", "Deep Cut"), shown());
    }

    @Test
    void aSearchOnlyFindsKnownInjuries() {
        know("cut");
        run(alice, "search", "leg");
        assertEquals("[SkysongStats] You know no injury whose name contains \"leg\".", next(alice));
    }

    @Test
    void theConsoleCantOpenIt() {
        assertEquals("[SkysongStats] Only players can use this command!", console("list"));
    }

    @Test
    void noArgumentsOpenTheMenuToo() {
        know("cut");
        run(alice);
        assertEquals(List.of("Cut"), shown());
    }

    @Test
    void aSearchNeedsText() {
        run(alice, "search");
        assertEquals("[SkysongStats] Usage: /injury search (text)", next(alice));
    }

    @Test
    void theMenuNeedsItsPermission() {
        alice.addAttachment(plugin, InjuryMenu.PERMISSION, false);
        run(alice, "list");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(alice));
    }

    @Test
    void tabCompletesOnlyUsedSeverities() {
        assertEquals(List.of("minor", "severe"), complete(alice, "list", ""));
        assertEquals(List.of(), complete(alice, "search", ""));
    }
}
