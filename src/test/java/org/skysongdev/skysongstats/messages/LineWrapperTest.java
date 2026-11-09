package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineWrapperTest {
    private static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    @Test
    void noWidthKeepsOneLine() {
        assertEquals("Broken Leg, Bruised Ribs", LineWrapper.wrap("Broken Leg, Bruised Ribs", 0));
    }

    @Test
    void breaksBetweenWordsOnly() {
        assertEquals("Broken Leg,<br>Bruised Ribs", LineWrapper.wrap("Broken Leg, Bruised Ribs", 12));
        assertEquals("Broken<br>Leg,<br>Bruised<br>Ribs", LineWrapper.wrap("Broken Leg, Bruised Ribs", 7));
    }

    @Test
    void aWordLongerThanTheLimitGetsItsOwnLine() {
        assertEquals("a<br>Supercalifragilistic<br>b", LineWrapper.wrap("a Supercalifragilistic b", 5));
    }

    @Test
    void tagsDontCountTowardsTheLength() {
        String text = "<red>Broken Leg</red>, <#f8e886>Bruised Ribs";
        assertEquals("<red>Broken Leg</red>,<br><#f8e886>Bruised Ribs", LineWrapper.wrap(text, 12));
    }

    @Test
    void spacesInsideATagDontBreakIt() {
        String text = "<hover:show_text:'It hurts a lot'>Broken Leg</hover>";
        assertEquals("<hover:show_text:'It hurts a lot'>Broken<br>Leg</hover>", LineWrapper.wrap(text, 8));
    }

    @Test
    void aWrappedNameKeepsItsHoverOnBothLines() {
        Component wrapped = MiniMessage.miniMessage().deserialize(
                LineWrapper.wrap("<hover:show_text:'It hurts'>Broken Leg</hover>", 8));
        assertEquals("Broken\nLeg", PlainTextComponentSerializer.plainText().serialize(wrapped));
        assertNotNull(wrapped.children().isEmpty() ? wrapped.hoverEvent() : wrapped.children().get(0).hoverEvent());
        wrapped.children().forEach(child -> assertNotNull(child.hoverEvent(), "no hover on " + child));
    }

    @Test
    void wordsKeepQuotedArgumentsAndEscapesWhole() {
        assertEquals(List.of("<hover:show_text:\"a b\">x", "\\<y"), LineWrapper.words("<hover:show_text:\"a b\">x \\<y"));
        assertEquals("x <br>", plain("x \\<br>"));
    }
}
