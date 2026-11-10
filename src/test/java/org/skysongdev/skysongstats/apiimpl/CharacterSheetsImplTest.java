package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongstats.api.SheetLayout;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.CharacterSheet;
import org.skysongdev.skysongstats.api.SheetField;
import org.skysongdev.skysongstats.database.CharacterProfile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSheetsImplTest {
    private static final UUID PLAYER = UUID.randomUUID();
    private static final CharacterId MAGE = new CharacterId(PLAYER, "Mage");
    private static final CharacterId NOBODY = new CharacterId(UUID.randomUUID(), "Mage");

    // These tests are about the sheet fields; rendering is tested in SheetRenderingTest.
    private static final SheetRenderer NO_RENDERING = new SheetRenderer() {
        @Override
        public Optional<List<Component>> render(ProfileKey character, SheetLayout layout, String name) {
            return Optional.empty();
        }

        @Override
        public Optional<List<Component>> render(ProfileKey character, List<String> lines, String name) {
            return Optional.empty();
        }
    };

    private final CharacterProfile profile = new CharacterProfile(PLAYER.toString(), "Mage", "<red>Aria", "30",
            "Female", "Elf", "she/her", "A quiet mage");
    private final List<CharacterProfile> saved = new ArrayList<>();
    private final Set<ProfileKey> withAncestry = new HashSet<>();
    private final CharacterSheetsImpl sheets = sheets(saved::add, () -> true);

    private CharacterSheetsImpl sheets(CharacterSheetsImpl.SheetStore store, java.util.function.BooleanSupplier main) {
        return new CharacterSheetsImpl((uuid, name) -> uuid.equals(profile.getUuid())
                && name.equals(profile.getProfile()) ? profile : null, store, NO_RENDERING, new MainThread(main),
                withAncestry::contains);
    }

    @Test
    void theAncestryFieldOfACharacterWithAnAncestryIsLocked() {
        withAncestry.add(ApiTypes.key(MAGE));

        assertThrows(IllegalStateException.class, () -> sheets.set(MAGE, SheetField.ANCESTRY, "Orc"));
        assertEquals("Elf", profile.getAncestry());
        assertTrue(saved.isEmpty());
        assertTrue(sheets.set(MAGE, SheetField.NAME, "Bram").isPresent(), "the other fields stay free");
    }

    @Test
    void theAncestryFieldIsFreeWithoutAnAncestry() {
        assertTrue(sheets.set(MAGE, SheetField.ANCESTRY, "Orc").isPresent());
        assertEquals("Orc", profile.getAncestry());
    }

    @Test
    void getReturnsTheSavedFields() {
        assertEquals(Optional.of(new CharacterSheet(MAGE, "<red>Aria", "30", "Female", "Elf", "she/her",
                "A quiet mage")), sheets.get(MAGE));
    }

    @Test
    void anUnknownCharacterHasNoSheet() {
        assertTrue(sheets.get(NOBODY).isEmpty());
        assertTrue(sheets.set(NOBODY, SheetField.NAME, "Bram").isEmpty());
        assertTrue(saved.isEmpty());
    }

    @Test
    void everyFieldCanBeSetAndIsSaved() {
        for (SheetField field : SheetField.values()) {
            sheets.set(MAGE, field, "new " + field);
        }

        CharacterSheet sheet = sheets.get(MAGE).orElseThrow();
        assertEquals(List.of("new NAME", "new AGE", "new GENDER", "new ANCESTRY", "new PRONOUNS", "new DESCRIPTION"),
                List.of(sheet.name(), sheet.age(), sheet.gender(), sheet.ancestry(), sheet.pronouns(),
                        sheet.description()));
        assertEquals(SheetField.values().length, saved.size());
    }

    @Test
    void setReturnsTheSheetAfterTheChange() {
        CharacterSheet sheet = sheets.set(MAGE, SheetField.AGE, "31").orElseThrow();

        assertEquals("31", sheet.age());
        assertEquals("<red>Aria", sheet.name());
    }

    @Test
    void aValueAsLongAsTheFieldTakesIsAccepted() {
        String longest = "a".repeat(SheetField.ANCESTRY.maxLength());

        assertEquals(longest, sheets.set(MAGE, SheetField.ANCESTRY, longest).orElseThrow().ancestry());
    }

    @Test
    void aTooLongOrBlankValueIsRefusedAndNothingChanges() {
        String tooLong = "a".repeat(SheetField.NAME.maxLength() + 1);

        assertThrows(IllegalArgumentException.class, () -> sheets.set(MAGE, SheetField.NAME, tooLong));
        assertThrows(IllegalArgumentException.class, () -> sheets.set(MAGE, SheetField.NAME, "  "));
        assertEquals("<red>Aria", profile.getName());
        assertTrue(saved.isEmpty());
    }

    @Test
    void theFieldLimitsMatchTheDatabaseColumns() {
        assertEquals(List.of(100, 20, 20, 50, 20, 500),
                List.of(SheetField.values()).stream().map(SheetField::maxLength).toList());
    }

    @Test
    void aRefusedSaveKeepsTheOldValue() {
        CharacterSheetsImpl failing = sheets(changed -> {
            throw new SQLException("database down");
        }, () -> true);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> failing.set(MAGE, SheetField.NAME, "Bram"));

        assertTrue(error.getMessage().contains("database down"), error.getMessage());
        assertEquals("<red>Aria", profile.getName());
    }

    @Test
    void anEmptyColumnReadsAsEmptyText() {
        profile.setDescription(null);

        assertEquals("", sheets.get(MAGE).orElseThrow().description());
    }

    @Test
    void callsOffTheMainThreadAreRefused() {
        CharacterSheetsImpl offThread = sheets(saved::add, () -> false);

        assertThrows(IllegalStateException.class, () -> offThread.get(MAGE));
        assertThrows(IllegalStateException.class, () -> offThread.set(MAGE, SheetField.NAME, "Bram"));
    }

    @Test
    void nullsAreRefused() {
        assertThrows(NullPointerException.class, () -> sheets.get(null));
        assertThrows(NullPointerException.class, () -> sheets.set(MAGE, null, "Bram"));
        assertThrows(NullPointerException.class, () -> sheets.set(MAGE, SheetField.NAME, null));
    }

    @Test
    void fieldsAreFoundByNameIgnoringCase() {
        assertEquals(Optional.of(SheetField.PRONOUNS), SheetField.find("Pronouns"));
        assertTrue(SheetField.find("height").isEmpty());
    }
}
