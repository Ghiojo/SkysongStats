package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

// Breaks a MiniMessage text into lines of at most a number of visible
// characters, with <br>. Only the visible text counts, never tags, so colours
// and hovers don't use up the length. Lines break only at spaces outside tags,
// never inside a word; a word longer than the limit gets a line of its own. A
// tag that is open at a break (a colour, a hover) stays open on the next line.
public final class LineWrapper {
    static final String BREAK = "<br>";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private LineWrapper() {
    }

    // width 0 or less leaves the text on one line.
    public static String wrap(String text, int width) {
        if (width <= 0) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int lineLength = 0;
        boolean lineEmpty = true;
        for (String word : words(text)) {
            int length = visibleLength(word);
            if (!lineEmpty && lineLength + 1 + length > width) {
                result.append(BREAK);
                lineLength = 0;
                lineEmpty = true;
            } else if (!lineEmpty) {
                result.append(' ');
                lineLength++;
            }
            result.append(word);
            lineLength += length;
            lineEmpty = lineEmpty && length == 0;
        }
        return result.toString();
    }

    // The text split at every space outside a tag. Quoted tag arguments
    // (<hover:show_text:'a b'>) and escaped characters (\<) are kept whole.
    static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        TagScanner scanner = new TagScanner();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == ' ' && scanner.outsideTag()) {
                words.add(word.toString());
                word.setLength(0);
            } else {
                word.append(character);
                scanner.read(character);
            }
        }
        words.add(word.toString());
        return words;
    }

    static int visibleLength(String word) {
        return MINI_MESSAGE.stripTags(word).replace("\\", "").length();
    }

    // Follows whether the characters read so far are inside a <tag>, and inside a
    // quoted argument of one.
    private static final class TagScanner {
        private boolean inTag;
        private char quote;
        private boolean escaped;

        boolean outsideTag() {
            return !inTag;
        }

        void read(char character) {
            if (escaped) {
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (!inTag) {
                inTag = character == '<';
            } else if (quote != 0) {
                quote = character == quote ? 0 : quote;
            } else if (character == '\'' || character == '"') {
                quote = character;
            } else {
                inTag = character != '>';
            }
        }
    }
}
