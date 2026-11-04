package org.skysongdev.skysongstats.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point of the SkysongStats API, the supported way for other plugins to read and
 * change SkysongStats characters. Classes outside the {@code org.skysongdev.skysongstats.api}
 * package are internal and may change without notice.
 *
 * <p>SkysongStats registers this API with Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * at the end of its {@code onEnable} and removes it in {@code onDisable}. Plugins that use it
 * add {@code depend: [SkysongStats]} (or {@code softdepend}) to their plugin.yml, so they
 * enable after SkysongStats, and get it with {@link #get()}:
 *
 * <pre>{@code
 * SkysongStatsApi stats = SkysongStatsApi.get();
 * Optional<CharacterId> playing = stats.profiles().active(player.getUniqueId());
 * }</pre>
 *
 * <p>Every character is a {@link CharacterId}: a player and one of their profiles. Reading or
 * changing a character's data must happen on the server's main thread; such methods throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface SkysongStatsApi {

    /**
     * The API registered by SkysongStats.
     *
     * @return the API
     * @throws IllegalStateException when SkysongStats is not installed or not enabled yet,
     *                               for example because its database is down
     */
    static @NotNull SkysongStatsApi get() {
        RegisteredServiceProvider<SkysongStatsApi> registration =
                Bukkit.getServicesManager().getRegistration(SkysongStatsApi.class);
        if (registration == null) {
            throw new IllegalStateException("The SkysongStats API is not available: SkysongStats is not enabled."
                    + " Add 'depend: [SkysongStats]' to your plugin.yml and check that SkysongStats is installed"
                    + " and started without errors.");
        }
        return registration.getProvider();
    }

    /**
     * The version of the API that the installed SkysongStats implements, as
     * {@code major.minor.patch}. A plugin can compare it with the version it was built
     * against and log a clear message instead of failing with {@link NoSuchMethodError}.
     * Adding methods raises the minor version; removing or changing one raises the major.
     *
     * @return the API version, for example {@code "1.0.0"}
     */
    @NotNull String apiVersion();

    /** Which profiles players have, and which one each player is playing. */
    @NotNull Profiles profiles();
}
