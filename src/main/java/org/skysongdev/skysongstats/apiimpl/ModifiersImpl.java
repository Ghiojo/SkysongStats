package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Modifier;
import org.skysongdev.skysongstats.api.Modifiers;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredModifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Modifiers answered by the stat service, which keeps them on each character sheet.
final class ModifiersImpl implements Modifiers {
    private final StatService stats;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    ModifiersImpl(StatService stats, MainThread mainThread, ChangeCauses causes) {
        this.stats = stats;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull List<Modifier> list(@NotNull CharacterId character) {
        ProfileKey key = checked(character);
        return stats.sheet(key)
                .map(sheet -> sheet.modifiers().stream().map(ModifiersImpl::toApi).toList())
                .orElse(List.of());
    }

    @Override
    public @NotNull Optional<Modifier> add(@NotNull CharacterId character, @NotNull String target, int amount) {
        Objects.requireNonNull(target, "target");
        ProfileKey key = checked(character);
        if (stats.sheet(key).isEmpty()) {
            return Optional.empty();
        }
        return modifierTarget(target).map(id -> toApi(
                causes.during(ChangeCause.API, () -> stats.addModifier(key, id, amount))));
    }

    @Override
    public boolean remove(@NotNull CharacterId character, @NotNull String modifierId) {
        Objects.requireNonNull(modifierId, "modifierId");
        ProfileKey key = checked(character);
        return stats.sheet(key).isPresent()
                && causes.during(ChangeCause.API, () -> stats.removeModifier(key, modifierId));
    }

    // A stat id or alias gives the stat's id; max_<bar> gives the bar's max id. The
    // same names /modifier add takes.
    private Optional<String> modifierTarget(String input) {
        Optional<String> stat = stats.definitions().find(input).map(StatDefinition::id);
        if (stat.isPresent()) {
            return stat;
        }
        return stats.definitions().findBarByMaxId(input).flatMap(StatDefinition::maxId);
    }

    private ProfileKey checked(CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return ApiTypes.key(character);
    }

    private static Modifier toApi(StoredModifier modifier) {
        return new Modifier(modifier.id(), modifier.target(), modifier.amount());
    }
}
