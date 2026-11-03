package org.skysongdev.skysongstats.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import org.skysongdev.skysongstats.messages.ExternalPlaceholders;

// The sheets' link to PlaceholderAPI. Kept in its own class so the plugin only
// loads PlaceholderAPI's classes when PlaceholderAPI is installed.
public final class PlaceholderApiPlaceholders {
    private PlaceholderApiPlaceholders() {
    }

    public static ExternalPlaceholders create() {
        return new ExternalPlaceholders(PlaceholderAPI::setPlaceholders);
    }
}
