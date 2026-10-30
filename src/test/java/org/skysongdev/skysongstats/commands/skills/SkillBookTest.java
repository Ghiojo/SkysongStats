package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The /skills book pages come from the book: section of skills.yml and messages.yml. */
class SkillBookTest {
    private SkysongStats plugin;
    private SkillBook book;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(SkysongStats.class);
        HandlerList.unregisterAll(plugin);
        book = new SkillBook(plugin::getMessages);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static SkillsConfig load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return SkillsConfigLoader.load(file, name -> true);
    }

    @Test
    void theDefaultBookHasTheThreePagesWithEachSkillsLevel() {
        SkillsConfig config = plugin.getSkillService().config();
        List<Component> pages = book.pages(config, skill -> "animalhandling".equals(skill.id()) ? 20 : 0);

        assertEquals(3, pages.size());
        String gathering = plain(pages.get(0));
        assertTrue(gathering.startsWith("Gathering Skills:\n\nAnimal Handling: Novice(20)\nFarming: Unskilled(0)\n"),
                gathering);
        assertEquals(8, gathering.split("\n", -1).length);
        assertTrue(plain(pages.get(1)).startsWith("Crafting Skills:\n\nAlchemy: Unskilled(0)\n"));
        assertTrue(plain(pages.get(2)).endsWith("\nT&E: Unskilled(0)"), plain(pages.get(2)));
    }

    @Test
    void skillsOnNoPageGetAnExtraLastPage() throws InvalidConfigurationException {
        SkillsConfig config = load("""
                levels:
                  unskilled: {name: Unskilled, xp: 0}
                  expert: {name: Expert, xp: 100}
                book:
                  main: {title: "Main", skills: [farming]}
                skills:
                  farming: {name: Farming}
                  juggling: {name: Juggling}
                """);
        List<Component> pages = book.pages(config, skill -> 100);

        assertEquals(List.of("Main\n\nFarming: Expert(100)", "Other Skills:\n\nJuggling: Expert(100)"),
                pages.stream().map(SkillBookTest::plain).toList());
    }

    @Test
    void aBookWithEverySkillOnAPageHasNoExtraPage() throws InvalidConfigurationException {
        SkillsConfig config = load("""
                levels:
                  unskilled: {name: Unskilled, xp: 0}
                book:
                  main: {title: "Main", skills: [farming]}
                skills:
                  farming: {name: Farming}
                """);
        assertEquals(1, book.pages(config, skill -> 0).size());
    }
}
