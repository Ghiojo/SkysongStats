package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.definition.InjuryDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AfflictionTexts} with the bundled messages.yml and the shared fixtures.
 * The character's name is "<b>Aria</b>", to check its format is kept.
 */
class AfflictionTextsTest {
    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");
    private static final ConditionDefinitions CONDITIONS = AfflictionFixtures.conditions();
    private static final InjuryDefinitions INJURIES = AfflictionFixtures.injuries();

    private final AfflictionTexts texts = new AfflictionTexts(AfflictionTextsTest::bundledMessages,
            character -> "<b>Aria</b>");

    private static Messages bundledMessages() {
        try (InputStream in = AfflictionTextsTest.class.getResourceAsStream("/messages.yml")) {
            YamlConfiguration bundled = new YamlConfiguration();
            bundled.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return new Messages(bundled, bundled, warning -> fail(warning));
        } catch (IOException | InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // The text shown when hovering over the first part of a component that has one.
    private static String hover(Component component) {
        HoverEvent<?> hover = findHover(component);
        assertNotNull(hover, "no hover in " + component);
        return plain((Component) hover.value());
    }

    private static HoverEvent<?> findHover(Component component) {
        if (component.hoverEvent() != null) {
            return component.hoverEvent();
        }
        return component.children().stream().map(AfflictionTextsTest::findHover)
                .filter(found -> found != null).findFirst().orElse(null);
    }

    private static InjuryDefinition injury(String id) {
        return INJURIES.get(id).orElseThrow();
    }

    private static ConditionDefinition condition(String id) {
        return CONDITIONS.get(id).orElseThrow();
    }

    @Test
    void anInjuryTagShowsItsNameWithItsDescriptionOnHover() {
        Component tag = texts.injuryTag(injury("broken-leg"));
        assertEquals("[Broken Leg]", plain(tag));
        assertEquals("Snapped", hover(tag));
    }

    @Test
    void aConditionTagShowsItsLevelAboveOne() {
        assertEquals("[Bleeding]", plain(texts.conditionTag(condition("bleeding"), 1)));
        Component levelTwo = texts.conditionTag(condition("bleeding"), 2);
        assertEquals("[Bleeding 2]", plain(levelTwo));
        assertEquals("Loses 2 blood", hover(levelTwo));
    }

    @Test
    void theAnnouncementFillsInTheCharacterAndTheInjuryTag() {
        Component announcement = texts.announcement(CHARACTER, injury("broken-leg")).orElseThrow();
        assertEquals("Aria now has a [Broken Leg]", plain(announcement));
        assertEquals("Snapped", hover(announcement));
        assertTrue(AfflictionTexts.miniMessage(announcement).contains("<b>Aria</b>")
                || AfflictionTexts.miniMessage(announcement).contains("<bold>Aria</bold>"));
    }

    @Test
    void anInjuryWithoutAMessageHasNoAnnouncement() {
        assertEquals(Optional.empty(), texts.announcement(CHARACTER, injury("cut")));
    }
}
