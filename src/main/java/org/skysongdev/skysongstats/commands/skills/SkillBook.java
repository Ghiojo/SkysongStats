package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.skysongdev.skysongstats.definition.SkillBookPage;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillLevel;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

// The /skills book: one page per book: entry of skills.yml, in order, and an
// extra last page for the skills on no page. Each page is skills.view.page of
// messages.yml with the page's title and one skills.view.skill-line per skill.
public final class SkillBook {
    private static final String VIEW = "skills.view.";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Supplier<Messages> messages;

    public SkillBook(Supplier<Messages> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // Opens the book for the viewer, with the XP xp gives for each skill.
    public void open(Player viewer, SkillsConfig config, ToIntFunction<SkillDefinition> xp) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        pages(config, xp).forEach(meta::addPages);
        meta.setTitle(plainText(VIEW + "book-title"));
        meta.setAuthor(plainText(VIEW + "book-author"));
        book.setItemMeta(meta);
        viewer.openBook(book);
    }

    List<Component> pages(SkillsConfig config, ToIntFunction<SkillDefinition> xp) {
        List<Component> pages = new ArrayList<>();
        for (SkillBookPage page : config.book()) {
            List<SkillDefinition> skills = page.skills().stream()
                    .flatMap(id -> config.skills().get(id).stream()).toList();
            pages.add(page(MINI_MESSAGE.deserialize(page.title()), skills, config, xp));
        }
        List<SkillDefinition> others = config.skillsOnNoPage();
        if (!others.isEmpty()) {
            pages.add(page(messages.get().get(VIEW + "other-title"), others, config, xp));
        }
        return pages;
    }

    private Component page(Component title, List<SkillDefinition> skills, SkillsConfig config,
                           ToIntFunction<SkillDefinition> xp) {
        List<Component> lines = skills.stream().map(skill -> line(skill, config, xp.applyAsInt(skill))).toList();
        return messages.get().get(VIEW + "page", Map.of(), Map.of("title", title,
                "skills", Component.join(JoinConfiguration.newlines(), lines)));
    }

    private Component line(SkillDefinition skill, SkillsConfig config, int xp) {
        SkillLevel level = config.levels().levelFor(xp);
        return messages.get().get(VIEW + "skill-line", Map.of("xp", Integer.toString(xp)), Map.of(
                "skill", colored(skill.color(), skill.shortName()),
                "level", colored(level.color(), level.name())));
    }

    private static Component colored(String color, String text) {
        return MINI_MESSAGE.deserialize(color + "<text>", Placeholder.unparsed("text", text));
    }

    private String plainText(String key) {
        return PlainTextComponentSerializer.plainText().serialize(messages.get().get(key));
    }
}
