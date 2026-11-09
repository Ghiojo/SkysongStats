package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.AnnouncerRegistration;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Injuries;
import org.skysongdev.skysongstats.api.Injury;
import org.skysongdev.skysongstats.api.InjuryType;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.messages.AfflictionTexts;
import org.skysongdev.skysongstats.messages.AwardAnnouncers;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

// Injuries answered by the affliction service. A character "exists" when it has
// stats, like everywhere in the API. A plugin's announcer registrations go when it
// disables (the API offers this as a listener).
final class InjuriesImpl implements Injuries, Listener {
    private final Supplier<AfflictionService> afflictions;
    private final StatService stats;
    private final AfflictionTexts texts;
    private final AwardAnnouncers announcers;
    private final MainThread mainThread;
    private final ChangeCauses causes;

    InjuriesImpl(Supplier<AfflictionService> afflictions, StatService stats, AfflictionTexts texts,
                 AwardAnnouncers announcers, MainThread mainThread, ChangeCauses causes) {
        this.afflictions = afflictions;
        this.stats = stats;
        this.texts = texts;
        this.announcers = announcers;
        this.mainThread = mainThread;
        this.causes = causes;
    }

    @Override
    public @NotNull List<InjuryType> types() {
        return afflictions.get().injuryDefinitions().all().stream().map(AfflictionApiTypes::injuryType).toList();
    }

    @Override
    public @NotNull Optional<InjuryType> type(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return afflictions.get().injuryDefinitions().get(id).map(AfflictionApiTypes::injuryType);
    }

    @Override
    public @NotNull List<String> severities() {
        return afflictions.get().injuryDefinitions().severities();
    }

    @Override
    public @NotNull List<Injury> current(@NotNull CharacterId character) {
        return afflictions.get().injuries(checked(character)).stream().map(AfflictionApiTypes::injury).toList();
    }

    @Override
    public boolean has(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        return afflictions.get().hasInjury(checked(character), injuryId);
    }

    @Override
    public @NotNull Optional<Injury> award(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        ProfileKey key = checked(character);
        if (stats.sheet(key).isEmpty()) {
            return Optional.empty();
        }
        return causes.during(ChangeCause.API, () -> afflictions.get().awardInjury(key, injuryId))
                .map(award -> AfflictionApiTypes.injury(award.injury()));
    }

    @Override
    public boolean remove(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        ProfileKey key = checked(character);
        return causes.during(ChangeCause.API, () -> afflictions.get().removeInjury(key, injuryId));
    }

    @Override
    public @NotNull Set<String> known(@NotNull CharacterId character) {
        return Set.copyOf(afflictions.get().known(checked(character)));
    }

    @Override
    public boolean knows(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        return afflictions.get().knows(checked(character), injuryId);
    }

    @Override
    public boolean discover(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        ProfileKey key = checked(character);
        if (stats.sheet(key).isEmpty()) {
            return false;
        }
        return causes.during(ChangeCause.API, () -> afflictions.get().discover(key, injuryId, LearnSource.API))
                == AfflictionService.DiscoverResult.LEARNT;
    }

    @Override
    public boolean forget(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        return afflictions.get().forget(checked(character), injuryId) == AfflictionService.ForgetResult.FORGOTTEN;
    }

    @Override
    public @NotNull Optional<String> tag(@NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        return afflictions.get().injuryDefinitions().get(injuryId)
                .map(injury -> AfflictionTexts.miniMessage(texts.injuryTag(injury)));
    }

    @Override
    public @NotNull Optional<String> message(@NotNull CharacterId character, @NotNull String injuryId) {
        Objects.requireNonNull(injuryId, "injuryId");
        ProfileKey key = checked(character);
        return afflictions.get().injuryDefinitions().get(injuryId)
                .flatMap(injury -> texts.announcement(key, injury))
                .map(AfflictionTexts::miniMessage);
    }

    @Override
    public @NotNull AnnouncerRegistration announceAwards(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        mainThread.check();
        if (!plugin.isEnabled()) {
            throw new IllegalStateException(plugin.getName() + " is not enabled: call announceAwards in onEnable");
        }
        return new Registration(announcers.add(plugin));
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        announcers.removeOwner(event.getPlugin());
    }

    private ProfileKey checked(CharacterId character) {
        Objects.requireNonNull(character, "character");
        mainThread.check();
        return ApiTypes.key(character);
    }

    private final class Registration implements AnnouncerRegistration {
        private final AwardAnnouncers.Entry entry;

        private Registration(AwardAnnouncers.Entry entry) {
            this.entry = entry;
        }

        @Override
        public @NotNull Plugin owner() {
            return entry.owner();
        }

        @Override
        public boolean isRegistered() {
            return entry.isRegistered();
        }

        @Override
        public void unregister() {
            mainThread.check();
            announcers.remove(entry);
        }
    }
}
