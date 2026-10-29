package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

// The messages.yml texts the /setup screen shows, with {name} placeholders filled
// in. The plugin passes its messages.yml loader; tests pass plain text.
public interface MenuTexts {
    Component line(String key, Map<String, String> placeholders);

    List<Component> lines(String key, Map<String, String> placeholders);
}
