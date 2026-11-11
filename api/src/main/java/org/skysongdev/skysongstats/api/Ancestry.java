package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * An ancestry of ancestries.yml, as the file has it after the last {@code /ssreload}.
 *
 * @param id          its id, lowercase
 * @param name        its name, MiniMessage
 * @param icon        its icon in the /setup list: a Material name or {@code model:<namespace:id>}
 * @param lore        its own lore lines, MiniMessage
 * @param permission  the node a player needs to pick it in /setup; empty when everyone may.
 *                    See {@link Ancestries#mayPick(org.bukkit.entity.Player, Ancestry)}
 * @param adjustments its fixed adjustments ({@code stats:}), in file order
 * @param choices     its choices, in file order; {@link AncestryPicks} names them by
 *                    their position in this list, from 0
 * @since 1.4.0
 */
public record Ancestry(@NotNull String id, @NotNull String name, @NotNull String icon, @NotNull List<String> lore,
                       @NotNull String permission, @NotNull List<AncestryAdjustment> adjustments,
                       @NotNull List<AncestryChoice> choices) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Ancestry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(permission, "permission");
        lore = List.copyOf(lore);
        adjustments = List.copyOf(adjustments);
        choices = List.copyOf(choices);
    }

    /** Whether it has choices to make. */
    public boolean hasChoices() {
        return !choices.isEmpty();
    }
}
