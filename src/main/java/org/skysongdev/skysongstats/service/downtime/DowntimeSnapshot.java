package org.skysongdev.skysongstats.service.downtime;

import java.util.Map;
import java.util.UUID;

/** Everything a {@link DowntimeRepository} holds, read once when the plugin starts. */
public record DowntimeSnapshot(
        Map<UUID, Integer> globals,
        Map<CharacterKey, Integer> characters,
        Map<UUID, Boolean> spendsGlobal) {

    public DowntimeSnapshot {
        globals = Map.copyOf(globals);
        characters = Map.copyOf(characters);
        spendsGlobal = Map.copyOf(spendsGlobal);
    }
}
