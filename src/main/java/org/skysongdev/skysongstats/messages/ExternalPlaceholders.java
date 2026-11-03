package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills in other plugins' {@code %placeholders%} (PlaceholderAPI) in a line of
 * messages.yml, for the player the line is about. It runs on the line as the
 * server owner wrote it, before the plugin's own {@code {name}} values go in, so
 * text a player typed (a character name...) is never read as a placeholder.
 * <p>
 * A value may come back in MiniMessage, which is kept, or with legacy colour
 * codes ({@code §a}, {@code &a}), which are turned into MiniMessage so they show
 * as colours. A placeholder nobody knows stays as written.
 */
public final class ExternalPlaceholders {
    /** For when PlaceholderAPI isn't installed: lines are left as they are. */
    public static final ExternalPlaceholders NONE = new ExternalPlaceholders((player, text) -> text);

    private static final Pattern TOKEN = Pattern.compile("%[^%\\s]+%");
    private static final Pattern LEGACY_CODE = Pattern.compile("[§&]([0-9a-fk-orx]|#[0-9a-f]{6})", Pattern.CASE_INSENSITIVE);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    // §x§r§r§g§g§b§b is how Bukkit writes a hex colour; &#rrggbb is the usual & form.
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR).hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.AMPERSAND_CHAR).hexColors().build();

    private final BiFunction<OfflinePlayer, String, String> lookup;

    /**
     * @param lookup gives one placeholder's value for a player, such as
     *               {@code PlaceholderAPI::setPlaceholders}; it gets the
     *               placeholder with its % signs and returns it unchanged when
     *               no plugin knows it
     */
    public ExternalPlaceholders(BiFunction<OfflinePlayer, String, String> lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /** The line with every known {@code %placeholder%} replaced by its value for the player. */
    public String fill(OfflinePlayer player, String line) {
        Matcher matcher = TOKEN.matcher(line);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group();
            String value = lookup.apply(player, token);
            String replacement = value == null || value.equals(token) ? token : asMiniMessage(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String asMiniMessage(String value) {
        if (!LEGACY_CODE.matcher(value).find()) {
            return value;
        }
        LegacyComponentSerializer legacy = value.indexOf(LegacyComponentSerializer.SECTION_CHAR) >= 0 ? SECTION : AMPERSAND;
        return MINI_MESSAGE.serialize(legacy.deserialize(value));
    }
}
