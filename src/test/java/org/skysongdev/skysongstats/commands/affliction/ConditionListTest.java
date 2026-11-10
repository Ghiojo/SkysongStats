package org.skysongdev.skysongstats.commands.affliction;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** /condition list and /condition search on the shared affliction fixtures. */
class ConditionListTest extends AfflictionCommandFixture {
    @Override
    protected String commandName() {
        return "condition";
    }

    private ItemStack item(int slot) {
        return alice.getOpenInventory().getTopInventory().getItem(slot);
    }

    @Test
    void showsEveryConditionWithItsLevelAsValue() {
        run(alice, "list");
        assertEquals("Conditions", plain(alice.getOpenInventory().title()));
        assertEquals("Bleeding", plain(item(9).getItemMeta().displayName()));
        assertEquals(List.of("Loses [value] blood", "ID: bleeding"),
                item(9).getItemMeta().lore().stream().map(AfflictionCommandFixture::plain).toList());
        assertEquals("Dazed", plain(item(11).getItemMeta().displayName()));
        assertNull(item(12));
    }

    @Test
    void searchShowsMatchingConditions() {
        run(alice, "search", "weak");
        assertEquals("Conditions matching weak", plain(alice.getOpenInventory().title()));
        assertEquals("Weakened", plain(item(9).getItemMeta().displayName()));
        assertNull(item(10));
    }

    @Test
    void aSearchWithNoMatchSaysSo() {
        run(alice, "search", "zzz");
        assertEquals("[SkysongStats] There is no condition whose name contains \"zzz\".", next(alice));
    }

    @Test
    void aSearchNeedsText() {
        run(alice, "search");
        assertEquals("[SkysongStats] Usage: /condition search (text)", next(alice));
    }

    @Test
    void noArgumentsOpenTheMenuToo() {
        run(alice);
        assertEquals("Conditions", plain(alice.getOpenInventory().title()));
    }

    @Test
    void theMenuNeedsItsPermission() {
        alice.addAttachment(plugin, ConditionMenu.PERMISSION, false);
        run(alice, "list");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(alice));
        run(alice, "search", "weak");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(alice));
        assertNull(alice.nextComponentMessage(), "one message each");
    }

    @Test
    void theConsoleCantOpenIt() {
        assertEquals("[SkysongStats] Only players can use this command!", console("list"));
    }
}
