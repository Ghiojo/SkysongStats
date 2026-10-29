package org.skysongdev.skysongstats.service.downtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every downtime rule lives here. Commands, placeholders and listeners only
 * call these methods.
 *
 * - A player or profile with no downtime yet gets its starting value the first
 *   time it is read, and that value is saved.
 * - Every change is saved through the {@link DowntimeRepository} before the
 *   in-memory value changes, then reported to the {@link DowntimeChangeListener}.
 * - Add stops at the pool's maximum, remove stops at 0.
 * - A player with no spending preference spends from their character.
 *
 * This class has no Bukkit calls, so it can be unit tested on its own.
 */
public class DowntimeService {

    private final DowntimeRepository repository;
    private volatile DowntimeSettings settings;
    private final DowntimeChangeListener listener;

    private final Map<UUID, Integer> globals = new ConcurrentHashMap<>();
    private final Map<CharacterKey, Integer> characters = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> spendsGlobal = new ConcurrentHashMap<>();

    public DowntimeService(DowntimeRepository repository, DowntimeSettings settings, DowntimeChangeListener listener) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /** Replaces the in-memory values with what the repository holds. */
    public synchronized void load() {
        DowntimeSnapshot snapshot = repository.loadAll();
        globals.clear();
        globals.putAll(snapshot.globals());
        characters.clear();
        characters.putAll(snapshot.characters());
        spendsGlobal.clear();
        spendsGlobal.putAll(snapshot.spendsGlobal());
    }

    public DowntimeSettings getSettings() {
        return settings;
    }

    /**
     * Switches to new limits, for {@code /ssreload}. Stored values are kept as
     * they are; the new limits apply from the next read, add, remove or reset.
     */
    public synchronized void setSettings(DowntimeSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    // --- Reading ---

    /** The player's global downtime, created at {@code global-start} if they have none. */
    public int getGlobal(UUID player) {
        Integer value = globals.get(player);
        return value != null ? value : createGlobal(player);
    }

    /** The profile's character downtime, created at {@code character-max} if it has none. */
    public int getCharacter(UUID player, String profile) {
        CharacterKey key = new CharacterKey(player, profile);
        Integer value = characters.get(key);
        return value != null ? value : createCharacter(key);
    }

    /** {@code true} if the player spends from global by default; players with no preference spend from their character. */
    public boolean spendsGlobal(UUID player) {
        return spendsGlobal.getOrDefault(player, false);
    }

    private synchronized int createGlobal(UUID player) {
        Integer existing = globals.get(player);
        if (existing != null) {
            return existing;
        }
        repository.saveGlobal(player, settings.globalStart());
        globals.put(player, settings.globalStart());
        return settings.globalStart();
    }

    private synchronized int createCharacter(CharacterKey key) {
        Integer existing = characters.get(key);
        if (existing != null) {
            return existing;
        }
        repository.saveCharacter(key, settings.characterMax());
        characters.put(key, settings.characterMax());
        return settings.characterMax();
    }

    // --- Changing ---

    /** Adds to the player's global downtime, stopping at {@code global-max}. */
    public synchronized DowntimeChange addGlobal(UUID player, int amount) {
        requireNotNegative(amount);
        int old = getGlobal(player);
        return setGlobal(player, old, capped(old, amount, settings.globalMax()), DowntimeChange.Cause.ADD);
    }

    /** Removes from the player's global downtime, stopping at 0. */
    public synchronized DowntimeChange removeGlobal(UUID player, int amount) {
        requireNotNegative(amount);
        int old = getGlobal(player);
        return setGlobal(player, old, Math.max(0, old - amount), DowntimeChange.Cause.REMOVE);
    }

    /** Adds to one profile's character downtime, stopping at {@code character-max}. */
    public synchronized DowntimeChange addCharacter(UUID player, String profile, int amount) {
        requireNotNegative(amount);
        int old = getCharacter(player, profile);
        return setCharacter(new CharacterKey(player, profile), old, capped(old, amount, settings.characterMax()), DowntimeChange.Cause.ADD);
    }

    /** Removes from one profile's character downtime, stopping at 0. */
    public synchronized DowntimeChange removeCharacter(UUID player, String profile, int amount) {
        requireNotNegative(amount);
        int old = getCharacter(player, profile);
        return setCharacter(new CharacterKey(player, profile), old, Math.max(0, old - amount), DowntimeChange.Cause.REMOVE);
    }

    /** Removes from the pool the player's preference points to: global, or their active profile. */
    public synchronized DowntimeChange removeFromPreferred(UUID player, String activeProfile, int amount) {
        return spendsGlobal(player)
                ? removeGlobal(player, amount)
                : removeCharacter(player, activeProfile, amount);
    }

    /** Switches the player's spending preference, saves it, and returns the new value ({@code true} = global). */
    public synchronized boolean togglePreference(UUID player) {
        boolean next = !spendsGlobal(player);
        setPreference(player, next);
        return next;
    }

    public synchronized void setPreference(UUID player, boolean spendGlobal) {
        repository.savePreference(player, spendGlobal);
        spendsGlobal.put(player, spendGlobal);
    }

    /** Removes a deleted profile's character downtime. */
    public synchronized void deleteCharacter(UUID player, String profile) {
        CharacterKey key = new CharacterKey(player, profile);
        repository.deleteCharacter(key);
        characters.remove(key);
    }

    /**
     * Starts a new downtime period: every profile goes to {@code character-max},
     * and every player gains {@code global-recovery} up to {@code global-max}
     * (a player already above the maximum keeps their value).
     *
     * Players and profiles that have no downtime yet are created first, so
     * they get the refill too.
     *
     * @param knownProfiles every player the plugin knows, with all their profile names
     * @return the values that changed
     */
    public synchronized List<DowntimeChange> resetAll(Map<UUID, ? extends Collection<String>> knownProfiles) {
        Map<UUID, Integer> oldGlobals = new HashMap<>(globals);
        Map<CharacterKey, Integer> oldCharacters = new HashMap<>(characters);
        knownProfiles.forEach((player, profiles) -> {
            oldGlobals.putIfAbsent(player, settings.globalStart());
            for (String profile : profiles) {
                oldCharacters.putIfAbsent(new CharacterKey(player, profile), settings.characterMax());
            }
        });

        Map<UUID, Integer> newGlobals = new HashMap<>();
        oldGlobals.forEach((player, old) -> newGlobals.put(player, capped(old, settings.globalRecovery(), settings.globalMax())));
        Map<CharacterKey, Integer> newCharacters = new HashMap<>();
        oldCharacters.forEach((key, old) -> newCharacters.put(key, settings.characterMax()));

        repository.saveAll(newGlobals, newCharacters);
        globals.putAll(newGlobals);
        characters.putAll(newCharacters);

        List<DowntimeChange> changes = new ArrayList<>();
        newGlobals.forEach((player, value) -> {
            int old = oldGlobals.get(player);
            if (old != value) {
                changes.add(new DowntimeChange(player, DowntimePool.GLOBAL, null, old, value, DowntimeChange.Cause.RESET));
            }
        });
        newCharacters.forEach((key, value) -> {
            int old = oldCharacters.get(key);
            if (old != value) {
                changes.add(new DowntimeChange(key.player(), DowntimePool.CHARACTER, key.profile(), old, value, DowntimeChange.Cause.RESET));
            }
        });
        changes.forEach(listener::onChange);
        return changes;
    }

    // --- Helpers ---

    private DowntimeChange setGlobal(UUID player, int old, int value, DowntimeChange.Cause cause) {
        DowntimeChange change = new DowntimeChange(player, DowntimePool.GLOBAL, null, old, value, cause);
        if (old != value) {
            repository.saveGlobal(player, value);
            globals.put(player, value);
            listener.onChange(change);
        }
        return change;
    }

    private DowntimeChange setCharacter(CharacterKey key, int old, int value, DowntimeChange.Cause cause) {
        DowntimeChange change = new DowntimeChange(key.player(), DowntimePool.CHARACTER, key.profile(), old, value, cause);
        if (old != value) {
            repository.saveCharacter(key, value);
            characters.put(key, value);
            listener.onChange(change);
        }
        return change;
    }

    /** {@code old + amount}, but never above {@code max}, and never below {@code old} (a value already over the max is kept). */
    private static int capped(int old, int amount, int max) {
        return Math.max(old, Math.min(old + amount, max));
    }

    private static void requireNotNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Downtime amount must not be negative: " + amount);
        }
    }
}
