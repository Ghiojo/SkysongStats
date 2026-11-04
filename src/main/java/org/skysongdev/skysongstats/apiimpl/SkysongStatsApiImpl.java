package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.api.Profiles;
import org.skysongdev.skysongstats.api.Skills;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

/**
 * SkysongStats' implementation of its public API, offered to other plugins through
 * Bukkit's {@link ServicesManager}.
 */
public final class SkysongStatsApiImpl implements SkysongStatsApi {
    /** The API version this implementation provides. See {@link SkysongStatsApi#apiVersion()}. */
    public static final String API_VERSION = "1.0.0";

    private final Profiles profiles;
    private final Skills skills;

    /** An API reading through the plugin's services, guarded to the server's main thread. */
    public SkysongStatsApiImpl(SkysongStats plugin) {
        MainThread mainThread = new MainThread(Bukkit::isPrimaryThread);
        this.profiles = new ProfilesImpl(plugin.getUtils().getProfileManager(),
                () -> plugin.getStatService().characters(), mainThread);
        this.skills = new SkillsImpl(plugin.getSkillService(), mainThread);
    }

    @Override
    public @NotNull String apiVersion() {
        return API_VERSION;
    }

    @Override
    public @NotNull Profiles profiles() {
        return profiles;
    }

    @Override
    public @NotNull Skills skills() {
        return skills;
    }

    /** Offers this API to other plugins, owned by {@code owner}. */
    public void register(ServicesManager services, Plugin owner) {
        services.register(SkysongStatsApi.class, this, owner, ServicePriority.Normal);
    }

    /** Stops offering this API, so {@link SkysongStatsApi#get()} fails until it is registered again. */
    public void unregister(ServicesManager services) {
        services.unregister(SkysongStatsApi.class, this);
    }
}
