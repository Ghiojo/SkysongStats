package org.skysongdev.skysongstats.storage;

import java.util.Locale;
import java.util.Optional;

// How a character learnt an injury, saved in ss_known_injuries.learnt_by so the
// server keeps its own record of who knows what and why.
public enum LearnSource {
    // The character got the injury.
    AWARD,
    // Staff ran /injury discover.
    DISCOVER,
    // Another plugin, through the API (SkysongChatTweaks when someone hears of it).
    API,
    // /ssimport chattweaks copied it from SkysongChatTweaks.
    IMPORT;

    public String column() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<LearnSource> fromColumn(String text) {
        for (LearnSource source : values()) {
            if (source.column().equalsIgnoreCase(text)) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }
}
