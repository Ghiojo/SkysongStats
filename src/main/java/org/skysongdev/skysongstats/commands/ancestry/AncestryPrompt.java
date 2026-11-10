package org.skysongdev.skysongstats.commands.ancestry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.character.AncestryService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.setup.AncestryChoicesMenu;
import org.skysongdev.skysongstats.setup.MenuTexts;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// The ancestry choices a character still has to make (staff gave it an ancestry,
// or opened its choices again). While they are pending its player is told so,
// with a button for /ancestry choose: when they join, when they switch to the
// character, and right when the choices become pending if they are playing it.
// openChoices() is the page /ancestry choose opens.
public final class AncestryPrompt implements Listener {
    static final String KEYS = "ancestry.";

    // What openChoices() did.
    public enum Opened { OPENED, NO_CHARACTER, NOTHING_PENDING, ANCESTRY_GONE }

    // Where the prompt finds players and their characters.
    public record Players(Function<Player, Optional<ProfileKey>> activeCharacter, Function<UUID, Player> online) {
        public Players {
            Objects.requireNonNull(activeCharacter, "activeCharacter");
            Objects.requireNonNull(online, "online");
        }
    }

    private final AncestryService ancestries;
    private final Supplier<StatDefinitions> stats;
    private final MenuTexts texts;
    private final Supplier<Messages> messages;
    private final Players players;
    private final Consumer<Runnable> nextTick;
    private final ChangeCauses causes;

    // nextTick runs a task on the next server tick, so a staff command's own
    // messages come before the prompt.
    public AncestryPrompt(AncestryService ancestries, Supplier<StatDefinitions> stats, MenuTexts texts,
                          Supplier<Messages> messages, Players players, Consumer<Runnable> nextTick,
                          ChangeCauses causes) {
        this.ancestries = Objects.requireNonNull(ancestries, "ancestries");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.texts = Objects.requireNonNull(texts, "texts");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.players = Objects.requireNonNull(players, "players");
        this.nextTick = Objects.requireNonNull(nextTick, "nextTick");
        this.causes = Objects.requireNonNull(causes, "causes");
    }

    // After the listener that gives a new player their first character.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        players.activeCharacter().apply(event.getPlayer())
                .ifPresent(character -> promptIfPending(event.getPlayer(), character));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileSwitch(ProfileUpdateEvent event) {
        ProfileKey character = new ProfileKey(event.getCharacter().player(), event.getCharacter().profile());
        promptIfPending(event.getPlayer(), character);
    }

    // The plugin calls this for every ancestry change.
    public void onAncestryChanged(AncestryService.AncestryChange change) {
        if (change.after().filter(StoredAncestry::choicesPending).isPresent()) {
            nextTick.accept(() -> prompt(change.character()));
        }
    }

    // Tells the character's player, when they are online and playing it.
    public void prompt(ProfileKey character) {
        Player player = players.online().apply(character.player());
        if (player != null && players.activeCharacter().apply(player).filter(character::equals).isPresent()) {
            promptIfPending(player, character);
        }
    }

    private void promptIfPending(Player player, ProfileKey character) {
        if (ancestries.of(character).filter(StoredAncestry::choicesPending).isEmpty()) {
            return;
        }
        // With its ancestry gone from ancestries.yml there is nothing to choose.
        ancestries.definitionOf(character).ifPresent(ancestry ->
                messages.get().send(player, KEYS + "choices-pending", Map.of("ancestry", ancestry.name())));
    }

    // Opens the choices page of the player's character, when it has choices
    // pending; it has no Back button. Confirming makes the choices, as cause.
    public Opened openChoices(Player player, ChangeCause cause) {
        Optional<ProfileKey> character = players.activeCharacter().apply(player);
        if (character.isEmpty()) {
            return Opened.NO_CHARACTER;
        }
        if (ancestries.of(character.get()).filter(StoredAncestry::choicesPending).isEmpty()) {
            return Opened.NOTHING_PENDING;
        }
        Optional<AncestryDefinition> ancestry = ancestries.definitionOf(character.get());
        if (ancestry.isEmpty()) {
            return Opened.ANCESTRY_GONE;
        }
        new AncestryChoicesMenu(ancestry.get(), stats.get(), texts, Optional.empty(),
                (chooser, picks) -> choose(chooser, character.get(), ancestry.get(), picks, cause)).open(player);
        return Opened.OPENED;
    }

    private void choose(Player player, ProfileKey character, AncestryDefinition ancestry, AncestryPicks picks,
                        ChangeCause cause) {
        String key;
        try {
            // Staff may have changed the ancestry while the page was open.
            boolean made = ancestries.definitionOf(character).filter(ancestry::equals).isPresent()
                    && causes.during(cause, () -> ancestries.choose(character, picks)).isPresent();
            key = made ? "done" : "changed";
        } catch (IllegalArgumentException e) {
            key = "changed";
        }
        messages.get().send(player, AncestryCommand.KEYS + "choose." + key, Map.of("ancestry", ancestry.name()));
    }
}
