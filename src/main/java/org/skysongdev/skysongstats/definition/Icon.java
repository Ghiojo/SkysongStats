package org.skysongdev.skysongstats.definition;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

// An item shown in a GUI: either a Material ("IRON_SWORD") or an item model
// ("model:<namespace:id>"). Exactly one of material and model is set.
public record Icon(String material, String model) {
    public static final String MODEL_PREFIX = "model:";
    private static final Pattern MODEL_KEY = Pattern.compile("[a-z0-9._-]+:[a-z0-9/._-]+");

    public Icon {
        if ((material == null) == (model == null)) {
            throw new IllegalArgumentException("An icon is either a material or a model");
        }
    }

    // Parses an icon from config. Material names are upper-cased; whether the
    // material exists is checked by the loader, which needs the server for it.
    public static Icon parse(String text) {
        Objects.requireNonNull(text, "text");
        String trimmed = text.trim();
        if (trimmed.regionMatches(true, 0, MODEL_PREFIX, 0, MODEL_PREFIX.length())) {
            String key = trimmed.substring(MODEL_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
            if (!MODEL_KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("'" + text + "' is not a valid item model, use model:<namespace:id>");
            }
            return new Icon(null, key);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("The icon is empty");
        }
        return new Icon(trimmed.toUpperCase(Locale.ROOT), null);
    }

    public boolean isModel() {
        return model != null;
    }
}
