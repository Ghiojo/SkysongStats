package org.skysongdev.skysongstats.definition;

import java.util.Locale;
import java.util.Optional;

// What a stat command does. The names are the keys of a stat's "commands:" block.
public enum StatAction {
    SET,
    ADD,
    REDUCE,
    // Like REDUCE, but the bar's shield stat absorbs the amount first.
    DAMAGE;

    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<StatAction> fromConfigName(String name) {
        for (StatAction action : values()) {
            if (action.configName().equalsIgnoreCase(name)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
