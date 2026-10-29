package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link Icon}.
 */
class IconTest {

    @Test
    void materialNamesAreUpperCased() {
        Icon icon = Icon.parse(" iron_sword ");
        assertEquals("IRON_SWORD", icon.material());
        assertNull(icon.model());
        assertFalse(icon.isModel());
    }

    @Test
    void modelsUseTheModelPrefix() {
        Icon icon = Icon.parse("MODEL:Skysong:items/ruby_ring");
        assertEquals("skysong:items/ruby_ring", icon.model());
        assertNull(icon.material());
        assertTrue(icon.isModel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "model:", "model:ruby", "model:sky song:ruby", "model:a:b:c"})
    void rejectsEmptyIconsAndBadModels(String text) {
        assertThrows(IllegalArgumentException.class, () -> Icon.parse(text));
    }

    @Test
    void isEitherAMaterialOrAModel() {
        assertThrows(IllegalArgumentException.class, () -> new Icon(null, null));
        assertThrows(IllegalArgumentException.class, () -> new Icon("APPLE", "a:b"));
    }
}
