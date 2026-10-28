package org.skysongdev.skysongstats.Utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkysongItemsKeysTest {
    // These are the keys Skysong_Items stores on items; a mismatch here means
    // arcrot and fictis silently stop counting.
    @Test
    void keysMatchTheOnesSkysongItemsWrites() {
        assertEquals("skysong_items:arcrot", SkysongItemsKeys.ARCROT.toString());
        assertEquals("skysong_items:charged_arcrot", SkysongItemsKeys.CHARGED_ARCROT.toString());
        assertEquals("skysong_items:fictis", SkysongItemsKeys.FICTIS.toString());
        assertEquals("skysong_items:charges", SkysongItemsKeys.CHARGES.toString());
    }
}
