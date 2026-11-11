package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.Ancestries;
import org.skysongdev.skysongstats.api.Ancestry;
import org.skysongdev.skysongstats.api.AncestryChange;
import org.skysongdev.skysongstats.api.AncestryPicks;
import org.skysongdev.skysongstats.api.CharacterAncestry;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AncestryRules;
import org.skysongdev.skysongstats.character.AncestryService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.commands.ancestry.AncestryPrompt;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

// Ancestries answered by the ancestry service. A character "exists" when it has
// stats, like everywhere in the API.
final class AncestriesImpl implements Ancestries {
    private static final String CHARACTER = "character";
    private static final String ANCESTRY = "ancestry";
    private static final String PICKS = "picks";

    private final Supplier<AncestryService> ancestries;
    private final StatService stats;
    private final Supplier<AncestryPrompt> prompt;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    AncestriesImpl(Supplier<AncestryService> ancestries, StatService stats, Supplier<AncestryPrompt> prompt,
                   MainThread mainThread, ChangeCauses causes) {
        this.ancestries = Objects.requireNonNull(ancestries, "ancestries");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.causes = Objects.requireNonNull(causes, "causes");
    }

    private AncestryDefinitions definitions() {
        return ancestries.get().definitions();
    }

    @Override
    public @NotNull List<Ancestry> all() {
        return definitions().all().stream().map(AncestryApiTypes::ancestry).toList();
    }

    @Override
    public @NotNull Optional<Ancestry> ancestry(@NotNull String idOrName) {
        Objects.requireNonNull(idOrName, "idOrName");
        AncestryDefinitions definitions = definitions();
        String wanted = idOrName.trim();
        return definitions.get(wanted)
                .or(() -> definitions.all().stream().filter(ancestry -> plain(ancestry.name()).equalsIgnoreCase(wanted))
                        .findFirst())
                .map(AncestryApiTypes::ancestry);
    }

    private static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(miniMessage))
                .trim();
    }

    @Override
    public boolean mayPick(@NotNull Player player, @NotNull Ancestry ancestry) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(ancestry, ANCESTRY);
        return ancestry.permission().isEmpty() || player.hasPermission(ancestry.permission());
    }

    @Override
    public @NotNull List<String> problems(@NotNull Ancestry ancestry, @NotNull AncestryPicks picks) {
        Objects.requireNonNull(ancestry, ANCESTRY);
        Objects.requireNonNull(picks, PICKS);
        return definitions().get(ancestry.id())
                .map(definition -> AncestryRules.problems(definition, AncestryApiTypes.picks(picks, stats.definitions())))
                .orElseGet(() -> List.of("ancestry '" + ancestry.id() + "' is no longer in ancestries.yml"));
    }

    @Override
    public @NotNull Optional<CharacterAncestry> of(@NotNull CharacterId character) {
        ProfileKey key = checked(character);
        return ancestries.get().of(key).map(saved -> AncestryApiTypes.characterAncestry(saved, definitions()));
    }

    @Override
    public @NotNull Optional<AncestryChange> set(@NotNull CharacterId character, @NotNull Ancestry ancestry,
                                                 @NotNull AncestryPicks picks) {
        Objects.requireNonNull(picks, PICKS);
        return set(character, ancestry, Optional.of(picks));
    }

    @Override
    public @NotNull Optional<AncestryChange> set(@NotNull CharacterId character, @NotNull Ancestry ancestry) {
        return set(character, ancestry, Optional.empty());
    }

    private Optional<AncestryChange> set(CharacterId character, Ancestry ancestry, Optional<AncestryPicks> picks) {
        Objects.requireNonNull(ancestry, ANCESTRY);
        ProfileKey key = checked(character);
        Optional<AncestryDefinition> definition = definitions().get(ancestry.id());
        if (definition.isEmpty() || stats.sheet(key).isEmpty()) {
            return Optional.empty();
        }
        Optional<org.skysongdev.skysongstats.character.AncestryPicks> converted =
                picks.map(chosen -> AncestryApiTypes.picks(chosen, stats.definitions()));
        return Optional.of(change(causes.during(ChangeCause.API,
                () -> ancestries.get().set(key, definition.get(), converted))));
    }

    @Override
    public @NotNull Optional<AncestryChange> choose(@NotNull CharacterId character, @NotNull AncestryPicks picks) {
        Objects.requireNonNull(picks, PICKS);
        ProfileKey key = checked(character);
        return causes.during(ChangeCause.API, () -> ancestries.get().choose(key,
                AncestryApiTypes.picks(picks, stats.definitions()))).map(this::change);
    }

    @Override
    public @NotNull Optional<AncestryChange> rechoose(@NotNull CharacterId character) {
        ProfileKey key = checked(character);
        return causes.during(ChangeCause.API, () -> ancestries.get().rechoose(key)).map(this::change);
    }

    @Override
    public @NotNull Optional<AncestryChange> remove(@NotNull CharacterId character) {
        ProfileKey key = checked(character);
        return causes.during(ChangeCause.API, () -> ancestries.get().remove(key)).map(this::change);
    }

    @Override
    public boolean openChoices(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        mainThread.check();
        return prompt.get().openChoices(player, ChangeCause.API) == AncestryPrompt.Opened.OPENED;
    }

    private AncestryChange change(AncestryService.AncestryChange change) {
        return AncestryApiTypes.change(change, definitions());
    }

    private ProfileKey checked(CharacterId character) {
        Objects.requireNonNull(character, CHARACTER);
        mainThread.check();
        return ApiTypes.key(character);
    }
}
