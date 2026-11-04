package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Downtime;
import org.skysongdev.skysongstats.api.DowntimeChange;
import org.skysongdev.skysongstats.api.DowntimeLimits;
import org.skysongdev.skysongstats.api.DowntimeTarget;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

// Downtime answered by the downtime service. The service creates a pool the first
// time it is read, so only players and characters the plugin knows are let through.
final class DowntimeImpl implements Downtime {
    private final DowntimeService downtime;
    private final Supplier<Set<ProfileKey>> characters;
    private final Function<UUID, Optional<String>> activeProfile;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    DowntimeImpl(DowntimeService downtime, Supplier<Set<ProfileKey>> characters,
                 Function<UUID, Optional<String>> activeProfile, MainThread mainThread, ChangeCauses causes) {
        this.downtime = downtime;
        this.characters = characters;
        this.activeProfile = activeProfile;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull DowntimeLimits limits() {
        DowntimeSettings settings = downtime.getSettings();
        return new DowntimeLimits(settings.characterMax(), settings.globalStart(), settings.globalRecovery(),
                settings.globalMax());
    }

    @Override
    public @NotNull OptionalInt get(@NotNull DowntimeTarget target) {
        if (!known(target)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(byPool(target, downtime::getGlobal, downtime::getCharacter));
    }

    @Override
    public @NotNull Optional<Boolean> spendsGlobal(@NotNull UUID player) {
        return known(DowntimeTarget.global(player)) ? Optional.of(downtime.spendsGlobal(player)) : Optional.empty();
    }

    @Override
    public boolean setSpendsGlobal(@NotNull UUID player, boolean spendGlobal) {
        if (!known(DowntimeTarget.global(player))) {
            return false;
        }
        downtime.setPreference(player, spendGlobal);
        return true;
    }

    @Override
    public @NotNull Optional<DowntimeChange> add(@NotNull DowntimeTarget target, int amount) {
        requireNotNegative(amount);
        if (!known(target)) {
            return Optional.empty();
        }
        return Optional.of(asApi(() -> byPool(target, player -> downtime.addGlobal(player, amount),
                (player, profile) -> downtime.addCharacter(player, profile, amount))));
    }

    @Override
    public @NotNull Optional<DowntimeChange> remove(@NotNull DowntimeTarget target, int amount) {
        requireNotNegative(amount);
        if (!known(target)) {
            return Optional.empty();
        }
        return Optional.of(asApi(() -> byPool(target, player -> downtime.removeGlobal(player, amount),
                (player, profile) -> downtime.removeCharacter(player, profile, amount))));
    }

    @Override
    public @NotNull Optional<DowntimeChange> spend(@NotNull UUID player, int amount) {
        requireNotNegative(amount);
        if (!known(DowntimeTarget.global(player))) {
            return Optional.empty();
        }
        if (downtime.spendsGlobal(player)) {
            return remove(DowntimeTarget.global(player), amount);
        }
        return activeProfile.apply(player)
                .flatMap(profile -> remove(DowntimeTarget.character(new CharacterId(player, profile)), amount));
    }

    private DowntimeChange asApi(Supplier<org.skysongdev.skysongstats.service.downtime.DowntimeChange> change) {
        return ApiTypes.downtimeChange(causes.during(ChangeCause.API, change));
    }

    // Checks the argument and the thread, and whether the plugin knows the pool's
    // owner: a player with a profile, or a character that exists.
    private boolean known(DowntimeTarget target) {
        Objects.requireNonNull(target, "target");
        mainThread.check();
        Set<ProfileKey> all = characters.get();
        return byPool(target, player -> all.stream().anyMatch(key -> key.player().equals(player)),
                (player, profile) -> all.contains(new ProfileKey(player, profile)));
    }

    // Runs global for a player's global pool, character for one character's pool.
    private static <T> T byPool(DowntimeTarget target, Function<UUID, T> global,
                                BiFunction<UUID, String, T> character) {
        if (target instanceof DowntimeTarget.Character pool) {
            return character.apply(pool.player(), pool.character().profile());
        }
        return global.apply(target.player());
    }

    private static void requireNotNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative: " + amount);
        }
    }
}
