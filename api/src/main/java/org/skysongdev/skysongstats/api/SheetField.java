package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/** A text field of a character sheet, the ones {@code /character set} changes. */
public enum SheetField {
    NAME(100),
    AGE(20),
    GENDER(20),
    ANCESTRY(50),
    PRONOUNS(20),
    DESCRIPTION(500);

    private final int maxLength;

    SheetField(int maxLength) {
        this.maxLength = maxLength;
    }

    /** The longest value the field takes, in characters: what the database column holds. */
    public int maxLength() {
        return maxLength;
    }

    /**
     * The field with this name, ignoring case, as typed in {@code /character set <field>}.
     *
     * @return the field, empty when no field has that name
     */
    public static @NotNull Optional<SheetField> find(@NotNull String name) {
        for (SheetField field : values()) {
            if (field.name().equals(name.trim().toUpperCase(Locale.ROOT))) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }
}
