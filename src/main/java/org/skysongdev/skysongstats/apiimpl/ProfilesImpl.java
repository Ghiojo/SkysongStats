package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Profiles;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

// Profiles read from the profile manager (active and setup rows) and the stat
// service, which holds one sheet for every profile that exists.
final class ProfilesImpl implements Profiles {
    private final ProfileManager profileManager;
    private final Supplier<Set<ProfileKey>> characters;
    private final MainThread mainThread;

    ProfilesImpl(ProfileManager profileManager, Supplier<Set<ProfileKey>> characters, MainThread mainThread) {
        this.profileManager = profileManager;
        this.characters = characters;
        this.mainThread = mainThread;
    }

    @Override
    public @NotNull Optional<CharacterId> active(@NotNull UUID player) {
        Objects.requireNonNull(player, "player");
        mainThread.check();
        return profileManager.activeProfile(player).map(profile -> new CharacterId(player, profile));
    }

    @Override
    public @NotNull List<CharacterId> all(@NotNull UUID player) {
        Objects.requireNonNull(player, "player");
        mainThread.check();
        return characters.get().stream()
                .filter(character -> character.player().equals(player))
                .map(ApiTypes::character)
                .sorted(Comparator.comparing(CharacterId::profile, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean exists(@NotNull CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return characters.get().contains(ApiTypes.key(character));
    }

    @Override
    public boolean isSetUp(@NotNull CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return profileManager.isSetUp(ApiTypes.key(character));
    }
}
