package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A condition of conditions.yml, as the file has it after the last {@code /ssreload}.
 *
 * @param id          its id, lowercase
 * @param name        its name, MiniMessage
 * @param description its description, MiniMessage, where {@code $} stands for the level;
 *                    empty when it has none
 * @param icon        its menu icon: a Material name ({@code PAPER}) or {@code model:<namespace:id>}
 * @param modifiers   the stat modifiers it gives while a character has it
 * @param conditions  the other conditions it gives while a character has it, by id, with
 *                    their level, in file order
 * @since 1.2.0
 */
public record ConditionType(@NotNull String id, @NotNull String name, @NotNull String description,
                            @NotNull String icon, @NotNull List<EffectModifier> modifiers,
                            @NotNull Map<String, Integer> conditions) {

    /**
     * @throws NullPointerException when any part is null
     */
    public ConditionType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(icon, "icon");
        modifiers = List.copyOf(modifiers);
        conditions = copy(conditions);
    }

    // An unmodifiable copy that keeps the order.
    static Map<String, Integer> copy(Map<String, Integer> conditions) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(conditions, "conditions")));
    }
}
