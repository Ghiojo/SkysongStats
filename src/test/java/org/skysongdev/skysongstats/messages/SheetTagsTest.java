package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SheetTagsTest {
    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "Default");

    private static SheetTags giving(String name, String text) {
        return new SheetTags() {
            @Override
            public Map<String, Component> tags(OfflinePlayer player, String line) {
                return Map.of(name, Component.text(text));
            }

            @Override
            public Map<String, Component> tags(ProfileKey character, String line) {
                return Map.of(name, Component.text(text));
            }
        };
    }

    @Test
    void allJoinsEveryTagAndTheFirstWins() {
        SheetTags all = SheetTags.all(giving("a", "first"), giving("b", "other"), giving("a", "second"));

        assertEquals(Map.of("a", Component.text("first"), "b", Component.text("other")), all.tags(CHARACTER, "line"));
        assertEquals(Map.of("a", Component.text("first"), "b", Component.text("other")), all.tags((OfflinePlayer) null, "line"));
    }
}
