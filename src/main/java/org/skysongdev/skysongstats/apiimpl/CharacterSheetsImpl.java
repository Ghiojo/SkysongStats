package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.CharacterSheet;
import org.skysongdev.skysongstats.api.CharacterSheets;
import org.skysongdev.skysongstats.api.SheetField;
import org.skysongdev.skysongstats.api.SheetLayout;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

// The sheet fields live in CharacterManager's loaded profiles and the
// skysong_characters table, the same ones /character set changes.
final class CharacterSheetsImpl implements CharacterSheets {
    private static final String CHARACTER = "character";
    private static final String NAME = "name";
    private static final Map<SheetField, Function<CharacterProfile, String>> GETTERS = new EnumMap<>(Map.of(
            SheetField.NAME, CharacterProfile::getName,
            SheetField.AGE, CharacterProfile::getAge,
            SheetField.GENDER, CharacterProfile::getGender,
            SheetField.ANCESTRY, CharacterProfile::getAncestry,
            SheetField.PRONOUNS, CharacterProfile::getPronouns,
            SheetField.DESCRIPTION, CharacterProfile::getDescription));
    private static final Map<SheetField, BiConsumer<CharacterProfile, String>> SETTERS = new EnumMap<>(Map.of(
            SheetField.NAME, CharacterProfile::setName,
            SheetField.AGE, CharacterProfile::setAge,
            SheetField.GENDER, CharacterProfile::setGender,
            SheetField.ANCESTRY, CharacterProfile::setAncestry,
            SheetField.PRONOUNS, CharacterProfile::setPronouns,
            SheetField.DESCRIPTION, CharacterProfile::setDescription));

    // Saves a changed profile, like Database.updateCharacterData.
    @FunctionalInterface
    interface SheetStore {
        void save(CharacterProfile profile) throws SQLException;
    }

    // (player UUID as text, profile name) -> the loaded profile, or null.
    private final BiFunction<String, String, CharacterProfile> profiles;
    private final SheetStore store;
    private final SheetRenderer rendering;
    private final MainThread mainThread;
    private final Predicate<ProfileKey> hasAncestry;

    // hasAncestry: whether the character has an ancestry, whose name then is its
    // ancestry field.
    CharacterSheetsImpl(BiFunction<String, String, CharacterProfile> profiles, SheetStore store,
                        SheetRenderer rendering, MainThread mainThread, Predicate<ProfileKey> hasAncestry) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.store = Objects.requireNonNull(store, "store");
        this.rendering = Objects.requireNonNull(rendering, "rendering");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.hasAncestry = Objects.requireNonNull(hasAncestry, "hasAncestry");
    }

    @Override
    public @NotNull Optional<CharacterSheet> get(@NotNull CharacterId character) {
        Objects.requireNonNull(character, CHARACTER);
        mainThread.check();
        return profile(character).map(profile -> sheet(character, profile));
    }

    @Override
    public @NotNull Optional<CharacterSheet> set(@NotNull CharacterId character, @NotNull SheetField field,
                                                 @NotNull String value) {
        Objects.requireNonNull(character, CHARACTER);
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        checkValue(field, value);
        mainThread.check();
        if (field == SheetField.ANCESTRY && hasAncestry.test(ApiTypes.key(character))) {
            throw new IllegalStateException("The ancestry field of " + character + " follows its ancestry;"
                    + " change it with ancestries()");
        }
        return profile(character).map(profile -> {
            String before = GETTERS.get(field).apply(profile);
            SETTERS.get(field).accept(profile, value);
            save(profile, field, before);
            return sheet(character, profile);
        });
    }

    @Override
    public @NotNull Optional<List<Component>> render(@NotNull CharacterId character, @NotNull SheetLayout layout,
                                                     @NotNull String name) {
        Objects.requireNonNull(character, CHARACTER);
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(name, NAME);
        mainThread.check();
        return rendering.render(ApiTypes.key(character), layout, name);
    }

    @Override
    public @NotNull Optional<List<Component>> render(@NotNull CharacterId character, @NotNull List<String> lines,
                                                     @NotNull String name) {
        Objects.requireNonNull(character, CHARACTER);
        Objects.requireNonNull(name, NAME);
        List<String> copy = List.copyOf(lines);
        mainThread.check();
        return rendering.render(ApiTypes.key(character), copy, name);
    }

    private static void checkValue(SheetField field, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > field.maxLength()) {
            throw new IllegalArgumentException(field + " takes at most " + field.maxLength()
                    + " characters, got " + value.length());
        }
    }

    // A refused save puts the old value back, so the loaded sheet matches the database.
    private void save(CharacterProfile profile, SheetField field, String before) {
        try {
            store.save(profile);
        } catch (SQLException e) {
            SETTERS.get(field).accept(profile, before);
            throw new IllegalStateException("Could not save the character sheet: " + e.getMessage(), e);
        }
    }

    private Optional<CharacterProfile> profile(CharacterId character) {
        return Optional.ofNullable(profiles.apply(character.player().toString(), character.profile()));
    }

    private static CharacterSheet sheet(CharacterId character, CharacterProfile profile) {
        return new CharacterSheet(character, text(profile.getName()), text(profile.getAge()),
                text(profile.getGender()), text(profile.getAncestry()), text(profile.getPronouns()),
                text(profile.getDescription()));
    }

    // A column the database left empty reads as "".
    private static String text(String value) {
        return value == null ? "" : value;
    }
}
