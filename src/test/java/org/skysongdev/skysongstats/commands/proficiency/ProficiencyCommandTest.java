package org.skysongdev.skysongstats.commands.proficiency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /proficiency on the plugin's ProficiencyService, the default proficiencies.yml
 * and messages.yml, run from the console and by players. Alice and Bob have an
 * active "Default" character; neither is an op. Costs are 4, 6 and 8.
 */
class ProficiencyCommandTest {
    private ProficiencyService proficiencies;
    private PluginCommand command;
    private ConsoleCommandSenderMock console;
    private PlayerMock alice;
    private PlayerMock bob;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        // MockBukkit doesn't add plugin.yml's permissions, so their defaults
        // (everyone may view and focus) wouldn't apply.
        plugin.getPluginMeta().getPermissions().forEach(server.getPluginManager()::addPermission);
        proficiencies = plugin.getProficiencyService();
        command = plugin.getCommand("proficiency");
        console = server.getConsoleSender();
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        ProfileManager.activeProfiles = new ArrayList<>();
        for (PlayerMock player : List.of(alice, bob)) {
            ProfileManager.activeProfiles.add(new ProfileIndex(player.getUniqueId().toString(), "Default"));
            plugin.getStatService().createCharacter(character(player));
            plugin.getSkillService().createCharacter(character(player));
            proficiencies.createCharacter(character(player));
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ProfileKey character(PlayerMock player) {
        return new ProfileKey(player.getUniqueId(), "Default");
    }

    private ProficiencyDefinition proficiency(String id) {
        return proficiencies.config().proficiencies().get(id).orElseThrow();
    }

    private String tier(PlayerMock player, String id) {
        return proficiencies.tier(character(player), proficiency(id)).id();
    }

    private Optional<ProficiencyFocus> focus(PlayerMock player) {
        return proficiencies.focus(character(player));
    }

    private static String plain(Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void run(CommandSender sender, String... args) {
        command.getExecutor().onCommand(sender, command, "proficiency", args);
    }

    // The first line the console gets for this command.
    private String console(String... args) {
        run(console, args);
        return plain(console.nextComponentMessage());
    }

    private static String next(PlayerMock player) {
        return plain(player.nextComponentMessage());
    }

    private void focusAxesWithPoints(int points) {
        run(alice, "focus", "axes");
        alice.nextComponentMessage();
        if (points > 0) {
            proficiencies.setPoints(character(alice), points);
        }
    }

    @Test
    void unknownSubcommand() {
        assertEquals("[SkysongStats] Incorrect Arguments! Usage: /proficiency (view/focus/points/tier)",
                console("nope"));
    }

    @Test
    void theSheetGroupsProficienciesBySkillAndShowsTheFocus() {
        focusAxesWithPoints(2);
        proficiencies.setTier(character(alice), proficiency("medicine"), proficiencies.config().tiers().get("adept")
                .orElseThrow());
        run(alice);
        List<String> lines = new ArrayList<>();
        for (Component line = alice.nextComponentMessage(); line != null; line = alice.nextComponentMessage()) {
            lines.add(plain(line));
        }
        assertEquals("                Alice' s Proficiencies:", lines.get(1));
        assertEquals("Focus: Axes 2/4 towards Proficient", lines.get(3));
        // Skills in skills.yml order.
        List<String> skillLines = List.of(lines.get(5).split("\n"));
        assertEquals(List.of("Economical", "Entertainment", "Martial", "Medical"),
                skillLines.stream().map(line -> line.substring(0, line.indexOf(':'))).toList());
        assertEquals("Entertainment: Sleight of Hand (Untrained), Stealth (Untrained), Performance (Untrained)",
                skillLines.get(1));
        assertTrue(skillLines.get(2).startsWith("Martial: Unarmored (Untrained), Light Armor (Untrained)"));
        assertEquals("Medical: Medicine (Adept)", skillLines.get(3));
    }

    @Test
    void theSheetSaysWhenThereIsNoFocus() {
        run(alice, "view");
        alice.nextComponentMessage();
        alice.nextComponentMessage();
        alice.nextComponentMessage();
        assertEquals("Focus: none. Pick one with /proficiency focus (proficiency)", next(alice));
    }

    @Test
    void viewingSomeoneElseNeedsThePermission() {
        run(bob, "view", "Alice");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(bob));
        bob.addAttachment(command.getPlugin(), ProficiencyCommand.VIEW_OTHER, true);
        run(bob, "view", "Alice");
        next(bob);
        assertEquals("                Alice' s Proficiencies:", next(bob));
    }

    @Test
    void theConsoleCanViewAPlayer() {
        run(console, "view", "Alice");
        console.nextComponentMessage();
        assertEquals("                Alice' s Proficiencies:", plain(console.nextComponentMessage()));
        int restOfTheSheet = 0;
        while (console.nextComponentMessage() != null) {
            restOfTheSheet++;
        }
        assertTrue(restOfTheSheet > 0);
        assertEquals("[SkysongStats] You must specify a player if you are running this command on console!",
                console("view"));
    }

    @Test
    void focusingWithNoPointsSwitchesAtOnce() {
        run(alice, "focus", "Axes");
        assertEquals("[SkysongStats] You are now training Axes.", next(alice));
        run(alice, "focus", "bows");
        assertEquals("[SkysongStats] You are now training Bows.", next(alice));
        assertEquals("bows", focus(alice).orElseThrow().proficiency().id());
    }

    @Test
    void focusingWhatYouAlreadyTrainOrMasteredChangesNothing() {
        focusAxesWithPoints(1);
        run(alice, "focus", "axes");
        assertEquals("[SkysongStats] You are already training Axes.", next(alice));
        proficiencies.setTier(character(alice), proficiency("bows"), proficiencies.config().tiers().top());
        run(alice, "focus", "bows");
        assertEquals("[SkysongStats] You have already mastered Bows, pick another proficiency to train.", next(alice));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 1), focus(alice).orElseThrow());
    }

    @Test
    void switchingAwayFromPointsAsksFirstAndConfirmSwitches() {
        focusAxesWithPoints(3);
        run(alice, "focus", "bows");
        assertEquals("[SkysongStats] Training Bows instead of Axes loses your 3 points in Axes.", next(alice));
        assertEquals("[SkysongStats] Are you sure? [Yes] (within a minute)", next(alice));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 3), focus(alice).orElseThrow());

        run(alice, "focus", "confirm");
        assertEquals("[SkysongStats] You are now training Bows.", next(alice));
        assertEquals(new ProficiencyFocus(proficiency("bows"), 0), focus(alice).orElseThrow());
        run(alice, "focus", "confirm");
        assertEquals("[SkysongStats] There is no focus change to confirm, or it ran out.", next(alice));
    }

    @Test
    void focusNeedsAProficiencyThatExists() {
        run(alice, "focus");
        assertEquals("[SkysongStats] Usage: /proficiency focus (proficiency)", next(alice));
        run(alice, "focus", "juggling");
        assertEquals("[SkysongStats] There is no proficiency called juggling!", next(alice));
        assertTrue(focus(alice).isEmpty());
    }

    @Test
    void focusNeedsItsPermission() {
        alice.addAttachment(command.getPlugin(), ProficiencyCommand.FOCUS, false);
        run(alice, "focus", "axes");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(alice));
        assertTrue(focus(alice).isEmpty());
    }

    @Test
    void trainingAddsPointsAndBothAreTold() {
        focusAxesWithPoints(0);
        assertEquals("[SkysongStats] Added 3 points to Alice's Axes (3/4).",
                console("points", "add", "Alice", "3", "martial", "master"));
        assertEquals("[SkysongStats] You gained 3 points in Axes (3/4).", next(alice));
    }

    @Test
    void trainingUpATierResetsThePoints() {
        focusAxesWithPoints(3);
        assertEquals("[SkysongStats] Alice's Axes is now Proficient!",
                console("points", "add", "Alice", "5", "MARTIAL", "adept"));
        assertEquals("[SkysongStats] Your Axes is now Proficient!", next(alice));
        assertEquals("proficient", tier(alice, "axes"));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 0), focus(alice).orElseThrow());
    }

    @Test
    void reachingTheTopTierTellsThePlayerToPickANewFocus() {
        focusAxesWithPoints(0);
        proficiencies.setTier(character(alice), proficiency("axes"), proficiencies.config().tiers().get("adept")
                .orElseThrow());
        console("points", "add", "Alice", "8", "martial", "master");
        assertEquals("[SkysongStats] Your Axes is now Master!", next(alice));
        assertEquals("[SkysongStats] You have mastered Axes! Pick a new proficiency with /proficiency focus "
                + "(proficiency).", next(alice));
        assertTrue(focus(alice).isEmpty());
    }

    @Test
    void trainingInAnotherSkillAddsNothing() {
        focusAxesWithPoints(1);
        assertEquals("[SkysongStats] Alice is training Axes, which isn't a Medical proficiency. No points were added.",
                console("points", "add", "Alice", "3", "med", "master"));
        assertEquals("[SkysongStats] Your Medical training was lost: Axes isn't a Medical proficiency.", next(alice));
        assertEquals(1, focus(alice).orElseThrow().points());
    }

    @Test
    void trainingAboveTheTrainersTierAddsNothing() {
        focusAxesWithPoints(0);
        proficiencies.setTier(character(alice), proficiency("axes"), proficiencies.config().tiers().get("adept")
                .orElseThrow());
        assertEquals("[SkysongStats] Alice's Axes is already Adept, this training only goes up to Adept. "
                + "No points were added.", console("points", "add", "Alice", "3", "martial", "adept"));
        assertEquals("[SkysongStats] This training only goes up to Adept and your Axes is already Adept. "
                + "No points were added.", next(alice));
        assertEquals(0, focus(alice).orElseThrow().points());
    }

    @Test
    void trainingWithoutAFocusAddsNothing() {
        assertEquals("[SkysongStats] Alice isn't training any proficiency, no points were added.",
                console("points", "add", "Alice", "3", "martial", "master"));
        assertEquals("[SkysongStats] Your Martial training was lost: you aren't training any proficiency. "
                + "Pick one with /proficiency focus (proficiency).", next(alice));
    }

    @Test
    void trainingChecksItsArguments() {
        focusAxesWithPoints(0);
        assertEquals("[SkysongStats] Usage: /proficiency points add (player) (amount) (skill) (max-tier)",
                console("points", "add", "Alice", "3", "martial"));
        assertEquals("[SkysongStats] \"0\" must be a whole number of at least 1!",
                console("points", "add", "Alice", "0", "martial", "master"));
        assertEquals("[SkysongStats] There is no skill called juggling!",
                console("points", "add", "Alice", "3", "juggling", "master"));
        assertEquals("[SkysongStats] There is no proficiency tier called legend!",
                console("points", "add", "Alice", "3", "martial", "legend"));
        assertEquals("[SkysongStats] Player Nobody not found!",
                console("points", "add", "Nobody", "3", "martial", "master"));
        assertEquals("[SkysongStats] Usage: /proficiency points (add/remove/set) (player) (amount)",
                console("points"));
        assertEquals(0, focus(alice).orElseThrow().points());
    }

    @Test
    void staffCommandsNeedTheManagePermission() {
        focusAxesWithPoints(0);
        run(bob, "points", "add", "Alice", "3", "martial", "master");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(bob));
        run(bob, "tier", "set", "Alice", "axes", "master");
        assertEquals("[SkysongStats] You don't have permission to use this command!", next(bob));
        assertEquals(0, focus(alice).orElseThrow().points());
        assertEquals("untrained", tier(alice, "axes"));

        bob.addAttachment(command.getPlugin(), ProficiencyManagement.MANAGE, true);
        run(bob, "points", "add", "Alice", "3", "martial", "master");
        assertEquals(3, focus(alice).orElseThrow().points());
    }

    @Test
    void removingPointsStopsAtZero() {
        focusAxesWithPoints(3);
        assertEquals("[SkysongStats] Alice's Axes now has 1/4 points.", console("points", "remove", "Alice", "2"));
        console("points", "remove", "Alice", "5");
        assertEquals(0, focus(alice).orElseThrow().points());
        assertEquals("untrained", tier(alice, "axes"));
    }

    @Test
    void settingPointsStaysBelowTheNextTier() {
        focusAxesWithPoints(0);
        assertEquals("[SkysongStats] Alice's Axes now has 3/4 points.", console("points", "set", "Alice", "3"));
        assertEquals("[SkysongStats] Axes can have at most 3 points, use /proficiency tier set to change its tier.",
                console("points", "set", "Alice", "4"));
        assertEquals(3, focus(alice).orElseThrow().points());
    }

    @Test
    void changingPointsNeedsAFocus() {
        assertEquals("[SkysongStats] Alice isn't training any proficiency.", console("points", "set", "Alice", "1"));
        assertEquals("[SkysongStats] Alice isn't training any proficiency.", console("points", "remove", "Alice", "1"));
        assertEquals("[SkysongStats] Usage: /proficiency points remove (player) (amount)",
                console("points", "remove", "Alice"));
    }

    @Test
    void tierSetRaisesOrLowersATier() {
        assertEquals("[SkysongStats] Set Alice's Long Blade to Adept.",
                console("tier", "set", "Alice", "long_blade", "ADEPT"));
        assertEquals("[SkysongStats] Your Long Blade is now Adept.", next(alice));
        console("tier", "set", "Alice", "long_blade", "proficient");
        assertEquals("proficient", tier(alice, "long_blade"));
        assertEquals("[SkysongStats] Usage: /proficiency tier set (player) (proficiency) (tier)",
                console("tier", "set", "Alice", "long_blade"));
    }

    @Test
    void tierChangesToYourselfSendOneMessage() {
        alice.setOp(true);
        run(alice, "tier", "set", "Alice", "axes", "adept");
        assertEquals("[SkysongStats] Set Alice's Axes to Adept.", next(alice));
        assertNull(alice.nextComponentMessage());
    }

    @Test
    void tabCompletesWhatTheSenderMayUse() {
        assertEquals(List.of("view", "focus"), command.tabComplete(bob, "proficiency", new String[]{""}));
        assertEquals(List.of("view", "focus", "points", "tier"),
                command.tabComplete(console, "proficiency", new String[]{""}));
        assertEquals(List.of(), command.tabComplete(bob, "proficiency", new String[]{"points", ""}));
        assertEquals(List.of("confirm", "crossbows"),
                command.tabComplete(alice, "proficiency", new String[]{"focus", "c"}));
        assertEquals(List.of("Bob"), command.tabComplete(console, "proficiency", new String[]{"view", "B"}));
        // Only skills that have proficiencies.
        assertEquals(List.of("martial", "medical"),
                command.tabComplete(console, "proficiency", new String[]{"points", "add", "Alice", "3", "m"}));
        assertEquals(List.of("master"),
                command.tabComplete(console, "proficiency", new String[]{"points", "add", "Alice", "3", "martial", "m"}));
        assertEquals(List.of("light_armor", "long_blade"),
                command.tabComplete(console, "proficiency", new String[]{"tier", "set", "Alice", "l"}));
        assertEquals(List.of("adept"),
                command.tabComplete(console, "proficiency", new String[]{"tier", "set", "Alice", "axes", "a"}));
    }
}
