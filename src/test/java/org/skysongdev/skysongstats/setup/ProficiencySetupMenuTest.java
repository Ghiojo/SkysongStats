package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficienciesConfigLoader;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The /setup proficiency screen on the default skills.yml and a small
 * proficiencies.yml, for a player who picked Martial on the skill screen: Unarmored
 * is trained, Martial has 1 pick from its skill and Medical 1 free pick. Texts are
 * shown as their key and placeholders, so the tests can read them.
 */
class ProficiencySetupMenuTest {
    private static final MenuTexts KEYS = new MenuTexts() {
        @Override
        public Component line(String key, Map<String, String> placeholders) {
            return Component.text(key + new TreeMap<>(placeholders));
        }

        @Override
        public List<Component> lines(String key, Map<String, String> placeholders) {
            return List.of(line(key, placeholders));
        }
    };
    private static final String PROFICIENCIES = """
            cost: 4
            tiers:
              untrained: {}
              proficient: {}
              adept: {extra: 2}
              master: {extra: 2}
            setup:
              trained: [unarmored]
              picks: {medical: 1}
              skill-picks: {martial: 1, entertainment: 1}
            proficiencies:
              unarmored: {name: Unarmored, skill: martial}
              perception: {name: Perception, skill: economical}
              axes: {name: Axes, skill: martial, icon: IRON_AXE, lore: ["<gray>Chop"]}
              bows: {name: Bows, skill: martial}
              medicine: {name: Medicine, skill: medical}
              stealth: {name: Stealth, skill: entertainment}
            """;
    private static final int UNARMORED = 9;
    private static final int AXES = 10;
    private static final int BOWS = 11;
    private static final int MEDICINE = 12;

    private PlayerMock player;
    private SkillsConfig skills;
    private ProficienciesConfig config;
    private ProficiencySetupMenu menu;
    private final List<List<ProficiencyDefinition>> confirmed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        server.getPluginManager().registerEvents(new SetupScreenListener(), MockBukkit.createMockPlugin());
        player = server.addPlayer("Alice");
        skills = SetupTestData.defaultSkills();
        config = proficiencies(PROFICIENCIES);
        menu = menuFor(config, List.of("martial"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ProficienciesConfig proficiencies(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return ProficienciesConfigLoader.load(file, skills.skills(), name -> true);
    }

    private ProficiencySetupMenu menuFor(ProficienciesConfig proficiencies, List<String> pickedSkills) {
        return ProficiencySetupMenu.forPickedSkills(proficiencies, skills.skills().all(), pickedSkills, KEYS,
                (who, raised) -> confirmed.add(raised)).orElseThrow();
    }

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    private ItemStack item(int slot) {
        return menu.getInventory().getItem(slot);
    }

    private String name(int slot) {
        return plain(item(slot).getItemMeta().displayName());
    }

    // No lore at all reads as no lines.
    private List<String> lore(int slot) {
        List<Component> lore = item(slot).getItemMeta().lore();
        return lore == null ? List.of() : lore.stream().map(ProficiencySetupMenuTest::plain).toList();
    }

    private boolean glows(int slot) {
        var meta = item(slot).getItemMeta();
        return meta.hasEnchantmentGlintOverride() && meta.getEnchantmentGlintOverride();
    }

    private ProficiencyDefinition proficiency(String id) {
        return config.proficiencies().get(id).orElseThrow();
    }

    @Test
    void showsTheTrainedOnesAndTheSkillsWithPicksInFileOrder() {
        assertEquals("proficiency.choice.proficiency-name{name=Unarmored}", name(UNARMORED));
        assertEquals("proficiency.choice.proficiency-name{name=Axes}", name(AXES));
        assertEquals("proficiency.choice.proficiency-name{name=Bows}", name(BOWS));
        assertEquals("proficiency.choice.proficiency-name{name=Medicine}", name(MEDICINE));
        // Perception's skill has no picks and Stealth's skill wasn't picked.
        assertNull(item(MEDICINE + 1));
    }

    @Test
    void usesTheIconAndLoreFromTheFileOrABook() {
        assertEquals(Material.IRON_AXE, item(AXES).getType());
        assertEquals(List.of("Chop"), lore(AXES));
        assertEquals(Material.BOOK, item(BOWS).getType());
        assertEquals(List.of(), lore(BOWS));
    }

    @Test
    void trainedOnesGlowAndCantBePickedOrRemoved() {
        assertTrue(glows(UNARMORED));
        assertEquals(List.of("proficiency.choice.trained{}"), lore(UNARMORED));
        menu.click(player, UNARMORED, ClickType.LEFT);
        menu.click(player, UNARMORED, ClickType.RIGHT);
        assertTrue(menu.picks().picked().isEmpty());
        assertEquals(1, menu.picks().left("martial"));
        assertTrue(glows(UNARMORED));
    }

    @Test
    void theHeaderListsThePicksLeftPerSkillInSkillsOrder() {
        assertEquals(Material.NETHER_STAR, item(ProficiencySetupMenu.HEADER_SLOT).getType());
        assertEquals("proficiency.choice.title{}", name(ProficiencySetupMenu.HEADER_SLOT));
        assertEquals(List.of("proficiency.choice.lore{}", "proficiency.choice.skill-picks{picks=1, skill=Martial}",
                "proficiency.choice.skill-picks{picks=1, skill=Medical}"), lore(ProficiencySetupMenu.HEADER_SLOT));
        menu.click(player, AXES, ClickType.LEFT);
        assertEquals("proficiency.choice.skill-picks{picks=0, skill=Martial}",
                lore(ProficiencySetupMenu.HEADER_SLOT).get(1));
    }

    @Test
    void aPickOnlySpendsPicksOfItsOwnSkill() {
        menu.click(player, AXES, ClickType.LEFT);
        menu.click(player, BOWS, ClickType.LEFT);
        menu.click(player, MEDICINE, ClickType.LEFT);
        assertEquals(List.of(proficiency("axes"), proficiency("medicine")), menu.picks().picked());
        assertTrue(glows(AXES));
        assertFalse(glows(BOWS));
    }

    @Test
    void rightClickUnpicks() {
        menu.click(player, AXES, ClickType.LEFT);
        menu.click(player, AXES, ClickType.RIGHT);
        menu.click(player, BOWS, ClickType.LEFT);
        assertEquals(List.of(proficiency("bows")), menu.picks().picked());
        assertFalse(glows(AXES));
    }

    @Test
    void confirmingWithPicksLeftNamesTheSkill() {
        player.openInventory(menu.getInventory());
        menu.click(player, MEDICINE, ClickType.LEFT);
        menu.click(player, ProficiencySetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals("proficiency.choice.pick-more{picks=1, skill=Martial}", plain(player.nextComponentMessage()));
        assertTrue(confirmed.isEmpty());
        assertEquals(menu, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void confirmingPassesTheTrainedAndPickedOnes() {
        player.openInventory(menu.getInventory());
        menu.click(player, MEDICINE, ClickType.LEFT);
        menu.click(player, BOWS, ClickType.LEFT);
        menu.click(player, ProficiencySetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(List.of(List.of(proficiency("unarmored"), proficiency("medicine"), proficiency("bows"))),
                confirmed);
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void picksOfASkillWithNothingLeftToPickDontHoldTheConfirmBack() {
        menu = menuFor(proficiencies(PROFICIENCIES.replace("skill-picks: {martial: 1,", "skill-picks: {martial: 5,")),
                List.of("martial"));
        player.openInventory(menu.getInventory());
        menu.click(player, AXES, ClickType.LEFT);
        menu.click(player, BOWS, ClickType.LEFT);
        menu.click(player, MEDICINE, ClickType.LEFT);
        menu.click(player, ProficiencySetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(1, confirmed.size());
    }

    @Test
    void closingClearsThePicks() {
        player.openInventory(menu.getInventory());
        menu.click(player, AXES, ClickType.LEFT);
        player.closeInventory();
        assertEquals(1, menu.picks().left("martial"));
        assertFalse(glows(AXES));
        assertTrue(glows(UNARMORED));
    }

    @Test
    void picksForTheSkillsPickedOnTheSkillScreen() {
        menu = menuFor(config, List.of("entertainment"));
        assertEquals("proficiency.choice.proficiency-name{name=Medicine}", name(10));
        assertEquals("proficiency.choice.proficiency-name{name=Stealth}", name(11));
        assertEquals(0, menu.picks().left("martial"));
    }

    @Test
    void theScreenIsSkippedWhenItWouldShowNothing() {
        ProficienciesConfig none = proficiencies(PROFICIENCIES.replace("""
                setup:
                  trained: [unarmored]
                  picks: {medical: 1}
                  skill-picks: {martial: 1, entertainment: 1}
                """, ""));
        assertTrue(ProficiencySetupMenu.forPickedSkills(none, skills.skills().all(), List.of("martial"), KEYS,
                (who, raised) -> confirmed.add(raised)).isEmpty());
    }
}
