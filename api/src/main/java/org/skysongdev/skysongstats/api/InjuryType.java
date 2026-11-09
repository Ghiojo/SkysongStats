package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An injury of injuries.yml, as the file has it after the last {@code /ssreload}.
 *
 * @param id          its id, lowercase
 * @param name        its name, MiniMessage
 * @param description its description, MiniMessage; empty when it has none
 * @param icon        its menu icon: a Material name ({@code BONE}) or {@code model:<namespace:id>}
 * @param severity    its severity, one of {@link Injuries#severities()}
 * @param message     its award message as written, with {@code {character}} and
 *                    {@code {injury}} not filled in; empty when it has none.
 *                    {@link Injuries#message(CharacterId, String)} gives it filled in
 * @param modifiers   the stat modifiers it gives while a character has it
 * @param conditions  the conditions it gives while a character has it, by id, with their
 *                    level, in file order
 * @since 1.2.0
 */
public record InjuryType(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String icon,
                         @NotNull String severity, @NotNull String message, @NotNull List<EffectModifier> modifiers,
                         @NotNull Map<String, Integer> conditions) {

    /**
     * @throws NullPointerException when any part is null
     */
    public InjuryType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        modifiers = List.copyOf(modifiers);
        conditions = ConditionType.copy(conditions);
    }
}
