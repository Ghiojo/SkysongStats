package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.api.ActiveCondition;
import org.skysongdev.skysongstats.api.AnnouncerRegistration;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Conditions;
import org.skysongdev.skysongstats.api.Effect;
import org.skysongdev.skysongstats.api.EffectModifier;
import org.skysongdev.skysongstats.api.EffectSource;
import org.skysongdev.skysongstats.api.Injuries;
import org.skysongdev.skysongstats.api.InjuryType;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.ConditionAwardEvent;
import org.skysongdev.skysongstats.api.event.ConditionRemoveEvent;
import org.skysongdev.skysongstats.api.event.InjuryAwardEvent;
import org.skysongdev.skysongstats.api.event.InjuryDiscoverEvent;
import org.skysongdev.skysongstats.api.event.InjuryRemoveEvent;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * injuries(), conditions() and stats().effects() of the running plugin, with the shared
 * affliction fixtures. Alice is online and playing "Default".
 */
class AfflictionApiTest {
    private ServerMock server;
    private PlayerMock player;
    private CharacterId alice;
    private Injuries injuries;
    private Conditions conditions;
    private final List<String> events = new ArrayList<>();
    private InjuryAwardEvent lastAward;

    public final class Recorder implements Listener {
        @EventHandler
        public void onAward(InjuryAwardEvent event) {
            lastAward = event;
            events.add("award " + event.getInjury().typeId() + (event.isNew() ? " new" : "") + " " + event.getCause());
        }

        @EventHandler
        public void onRemove(InjuryRemoveEvent event) {
            events.add("remove " + event.getInjuryId());
        }

        @EventHandler
        public void onDiscover(InjuryDiscoverEvent event) {
            events.add("discover " + event.getInjuryId());
        }

        @EventHandler
        public void onCondition(ConditionAwardEvent event) {
            events.add("condition " + event.getCondition().typeId() + " " + event.getPreviousLevel() + " -> "
                    + event.getCondition().level());
        }

        @EventHandler
        public void onConditionRemove(ConditionRemoveEvent event) {
            events.add("condition removed " + event.getConditionId());
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        // The API's own listener (announcers of disabled plugins) is needed.
        server.getPluginManager().registerEvents((Listener) SkysongStatsApi.get().injuries(), plugin);
        Files.writeString(new File(plugin.getDataFolder(), "conditions.yml").toPath(), AfflictionFixtures.CONDITIONS_FILE);
        Files.writeString(new File(plugin.getDataFolder(), "injuries.yml").toPath(), AfflictionFixtures.INJURIES_FILE);
        assertEquals(List.of(), plugin.reloadSettings());
        player = server.addPlayer("Alice");
        alice = new CharacterId(player.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(new ProfileKey(alice.player(), alice.profile()));
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(player.getUniqueId().toString(), "Default")));
        injuries = SkysongStatsApi.get().injuries();
        conditions = SkysongStatsApi.get().conditions();
        server.getPluginManager().registerEvents(new Recorder(), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void typesAreTheFilesDefinitions() {
        assertEquals(List.of("broken-leg", "cut", "deep-cut", "bruise"), injuries.types().stream().map(InjuryType::id).toList());
        InjuryType leg = injuries.type("BROKEN-LEG").orElseThrow();
        assertEquals("severe", leg.severity());
        assertEquals("PAPER", leg.icon());
        assertEquals("{character} now has a {injury}", leg.message());
        assertEquals(List.of(new EffectModifier("dex", -2, false)), leg.modifiers());
        assertEquals(Map.of("weakened", 1), leg.conditions());
        assertEquals(List.of("minor", "severe"), injuries.severities());
        assertEquals(List.of(new EffectModifier("str", -1, true)), conditions.type("bleeding").orElseThrow().modifiers());
        assertTrue(injuries.type("nope").isEmpty());
    }

    @Test
    void anAwardIsCurrentKnownAndFiresItsEvents() {
        assertTrue(injuries.award(alice, "broken-leg").isPresent());

        assertTrue(injuries.has(alice, "broken-leg"));
        assertEquals("broken-leg", injuries.current(alice).getFirst().typeId());
        assertEquals(Set.of("broken-leg"), injuries.known(alice));
        assertEquals(List.of("award broken-leg new API", "discover broken-leg",
                "condition weakened OptionalInt.empty -> 1", "condition dazed OptionalInt.empty -> 2"), events);
        assertEquals(Optional.of(injuries.message(alice, "broken-leg").orElseThrow()), lastAward.getMessage());
    }

    @Test
    void theAwardMessageGoesToThePlayerUnlessAPluginAnnounces() {
        injuries.award(alice, "broken-leg");
        assertEquals("Alice now has a [Broken Leg]", plain(player));

        Plugin chat = MockBukkit.createMockPlugin("Chat");
        AnnouncerRegistration registration = injuries.announceAwards(chat);
        injuries.award(alice, "broken-leg");
        assertNull(player.nextComponentMessage());

        registration.unregister();
        assertFalse(registration.isRegistered());
        injuries.award(alice, "broken-leg");
        assertEquals("Alice now has a [Broken Leg]", plain(player));
    }

    @Test
    void aDisabledPluginStopsAnnouncing() {
        Plugin chat = MockBukkit.createMockPlugin("Chat");
        AnnouncerRegistration registration = injuries.announceAwards(chat);
        server.getPluginManager().disablePlugin(chat);

        assertFalse(registration.isRegistered());
        injuries.award(alice, "broken-leg");
        assertEquals("Alice now has a [Broken Leg]", plain(player));
    }

    @Test
    void aDisabledPluginCantRegister() {
        Plugin chat = MockBukkit.createMockPlugin("Chat");
        server.getPluginManager().disablePlugin(chat);
        assertThrows(IllegalStateException.class, () -> injuries.announceAwards(chat));
    }

    @Test
    void tagsAndMessagesAreResolvedMiniMessage() {
        String tag = injuries.tag("broken-leg").orElseThrow();
        assertTrue(tag.contains("<hover:show_text:'Snapped'>"), tag);
        String message = injuries.message(alice, "broken-leg").orElseThrow();
        assertTrue(message.startsWith("Alice now has a "), message);
        assertTrue(message.contains(tag), message);
        assertTrue(injuries.message(alice, "cut").isEmpty());
        assertTrue(conditions.tag("bleeding", 2).orElseThrow().contains("Loses 2 blood"));
        assertThrows(IllegalArgumentException.class, () -> conditions.tag("bleeding", 0));
    }

    @Test
    void removeDiscoverAndForget() {
        injuries.award(alice, "cut");
        events.clear();
        assertTrue(injuries.remove(alice, "cut"));
        assertFalse(injuries.remove(alice, "cut"));
        assertEquals(List.of("remove cut", "condition removed bleeding"), events);

        assertTrue(injuries.discover(alice, "bruise"));
        assertFalse(injuries.discover(alice, "bruise"));
        assertTrue(injuries.knows(alice, "bruise"));
        assertTrue(injuries.forget(alice, "bruise"));
        assertFalse(injuries.knows(alice, "bruise"));
    }

    @Test
    void anUnknownCharacterGetsNothing() {
        CharacterId nobody = new CharacterId(UUID.randomUUID(), "Default");
        assertTrue(injuries.award(nobody, "cut").isEmpty());
        assertFalse(injuries.discover(nobody, "cut"));
        assertTrue(conditions.award(nobody, "bleeding", 1).isEmpty());
        assertEquals(List.of(), injuries.current(nobody));
        assertEquals(List.of(), SkysongStatsApi.get().stats().effects(nobody));
    }

    @Test
    void conditionsAwardedDirectlyAndGiven() {
        injuries.award(alice, "cut");
        ActiveCondition bleeding = conditions.award(alice, "bleeding", 5).orElseThrow();
        assertEquals(new ActiveCondition("bleeding", 5, true, List.of("cut"), List.of()), bleeding);
        assertEquals(List.of(bleeding), conditions.current(alice));

        assertTrue(conditions.remove(alice, "bleeding"));
        assertEquals(2, conditions.current(alice).getFirst().level());
        assertFalse(conditions.remove(alice, "bleeding"));
        assertThrows(IllegalArgumentException.class, () -> conditions.award(alice, "bleeding", 0));
    }

    @Test
    void statsListTheEffects() {
        injuries.award(alice, "broken-leg");
        conditions.award(alice, "bleeding", 3);
        assertEquals(List.of(new Effect("dex", -2, EffectSource.INJURY, "broken-leg"),
                        new Effect("str", -3, EffectSource.CONDITION, "bleeding")),
                SkysongStatsApi.get().stats().effects(alice));
        assertEquals(-3, SkysongStatsApi.get().stats().get(alice, "str").orElseThrow().total());
    }

    @Test
    void theConditionEventCarriesThePreviousLevel() {
        conditions.award(alice, "bleeding", 1);
        conditions.award(alice, "bleeding", 4);
        assertEquals(List.of("condition bleeding OptionalInt.empty -> 1", "condition bleeding OptionalInt[1] -> 4"),
                events);
    }

    @Test
    void anImportedInjuryFiresNoDiscoverEvent() {
        SkysongStats plugin = (SkysongStats) server.getPluginManager().getPlugin("SkysongStats");
        plugin.getAfflictionService().discover(new ProfileKey(alice.player(), alice.profile()), "cut",
                org.skysongdev.skysongstats.storage.LearnSource.IMPORT);
        assertEquals(List.of(), events);
    }

    private static String plain(PlayerMock player) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(player.nextComponentMessage());
    }
}
