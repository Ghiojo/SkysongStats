package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.AfflictionChangeListener;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedAfflictionRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AfflictionLists} and {@link AfflictionSheetTags} with the bundled
 * messages.yml and the shared fixtures.
 */
class AfflictionListsTest {
    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");

    private AfflictionService service;
    private AfflictionLists lists;
    private Messages messages;

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @BeforeEach
    void setUp() {
        messages = bundledMessages(SheetTags.NONE);
        service = new AfflictionService(AfflictionFixtures.conditions(), AfflictionFixtures.injuries(),
                new UnsavedAfflictionRepository(line -> { }), AfflictionChangeListener.NONE, () -> 1L);
        lists = new AfflictionLists(() -> service, new AfflictionTexts(() -> messages, character -> "Aria"),
                () -> messages);
    }

    private static Messages bundledMessages(SheetTags tags) {
        try (InputStream in = AfflictionListsTest.class.getResourceAsStream("/messages.yml")) {
            YamlConfiguration bundled = new YamlConfiguration();
            bundled.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return new Messages(bundled, bundled, warning -> fail(warning), ExternalPlaceholders.NONE, tags);
        } catch (IOException | InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private String shown(String name) {
        return plain(MiniMessage.miniMessage().deserialize(lists.resolve(CHARACTER, name).orElseThrow()));
    }

    @Test
    void otherNamesAreNotLists() {
        assertEquals(Optional.empty(), lists.resolve(CHARACTER, "str"));
        assertEquals(Optional.empty(), lists.resolve(CHARACTER, "injuries_nope"));
        assertFalse(AfflictionLists.isListName("injury"));
        assertTrue(AfflictionLists.isListName("Conditions_Hover_40"));
    }

    @Test
    void noneWhenEmpty() {
        assertEquals("None", shown("injuries"));
        assertEquals("None", shown("conditions_hover_50"));
        assertEquals("0", shown("injuries_count"));
    }

    @Test
    void injuriesInTheOrderTheyCame() {
        service.awardInjury(CHARACTER, "broken-leg");
        service.awardInjury(CHARACTER, "bruise");
        assertEquals("Broken Leg, Bruise", shown("injuries"));
        assertEquals("[Broken Leg], [Bruise]", shown("injuries_hover"));
        assertEquals("2", shown("injuries_count"));
    }

    @Test
    void conditionsShowTheirLevelAboveOne() {
        service.awardInjury(CHARACTER, "broken-leg");
        assertEquals("Weakened, Dazed 2", shown("conditions"));
        assertEquals("[Weakened], [Dazed 2]", shown("conditions_hover"));
    }

    @Test
    void aLineLengthBreaksTheList() {
        service.awardInjury(CHARACTER, "broken-leg");
        service.awardInjury(CHARACTER, "deep-cut");
        service.awardInjury(CHARACTER, "bruise");
        assertEquals("Broken Leg,\nDeep Cut,\nBruise", shown("injuries_12"));
        assertEquals("[Broken Leg],\n[Deep Cut],\n[Bruise]", shown("injuries_hover_13"));
    }

    @Test
    void hoverNamesShowTheirDescription() {
        service.awardInjury(CHARACTER, "broken-leg");
        Component list = MiniMessage.miniMessage().deserialize(lists.resolve(CHARACTER, "injuries_hover").orElseThrow());
        assertTrue(MiniMessage.miniMessage().serialize(list).contains("show_text:'Snapped'"));
    }

    @Test
    void sheetsCanUseTheListsAsTags() {
        service.awardInjury(CHARACTER, "cut");
        OfflinePlayer player = MockBukkit.mock().addPlayer();
        YamlConfiguration sheet = new YamlConfiguration();
        sheet.set("test.sheet", "Injuries: {injuries_hover_50} / {conditions} / {injuries_count} / {other}");
        Messages withSheet = new Messages(sheet, sheet, warning -> { }, ExternalPlaceholders.NONE,
                new AfflictionSheetTags(lists, about -> Optional.of(CHARACTER)));

        assertEquals("Injuries: [Cut] / Bleeding 2 / 1 / {other}",
                plain(withSheet.get("test.sheet", Map.of(), Map.of(), player)));
        // Only lines about a player get them.
        assertEquals("Injuries: {injuries_hover_50} / {conditions} / {injuries_count} / {other}",
                plain(withSheet.get("test.sheet")));
    }

    @Test
    void aPlayerWithoutACharacterGetsNoTags() {
        AfflictionSheetTags tags = new AfflictionSheetTags(lists, about -> Optional.empty());
        assertEquals(Map.of(), tags.tags(MockBukkit.mock().addPlayer(), "{injuries}"));
    }
}
