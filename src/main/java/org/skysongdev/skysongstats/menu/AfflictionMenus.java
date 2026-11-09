package org.skysongdev.skysongstats.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.setup.MenuItems;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// The /injuries and /conditions menus: one item per injury or condition, with
// its icon, its name, and as lore its description and the menu lines of
// messages.yml (injury.menu, condition.menu).
public final class AfflictionMenus {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String INJURY = "injury.menu.";
    private static final String CONDITION = "condition.menu.";

    private final Supplier<Messages> messages;

    public AfflictionMenus(Supplier<Messages> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // title is a key under injury.menu, filled with placeholders.
    public ListMenu injuries(List<InjuryDefinition> injuries, String title, Map<String, String> placeholders) {
        return new ListMenu(messages.get().get(INJURY + title, placeholders),
                injuries.stream().map(this::injuryItem).toList(),
                messages.get().get(INJURY + "previous-page"), messages.get().get(INJURY + "next-page"));
    }

    // title is a key under condition.menu, filled with placeholders.
    public ListMenu conditions(List<ConditionDefinition> conditions, String title, Map<String, String> placeholders) {
        return new ListMenu(messages.get().get(CONDITION + title, placeholders),
                conditions.stream().map(this::conditionItem).toList(),
                messages.get().get(CONDITION + "previous-page"), messages.get().get(CONDITION + "next-page"));
    }

    ItemStack injuryItem(InjuryDefinition injury) {
        Map<String, String> placeholders = Map.of("id", injury.id(), "severity", injury.severity());
        List<Component> lore = List.of(MINI_MESSAGE.deserialize(injury.description()),
                messages.get().get(INJURY + "id-line", placeholders),
                messages.get().get(INJURY + "severity-line", placeholders));
        return item(MenuItems.icon(injury.icon()), injury.name(), lore);
    }

    // A condition has no level here, so its description's $ shows as condition.menu.level.
    ItemStack conditionItem(ConditionDefinition condition) {
        String level = MINI_MESSAGE.serialize(messages.get().get(CONDITION + "level"));
        List<Component> lore = List.of(MINI_MESSAGE.deserialize(condition.description().replace("$", level)),
                messages.get().get(CONDITION + "id-line", Map.of("id", condition.id())));
        return item(MenuItems.icon(condition.icon()), condition.name(), lore);
    }

    private static ItemStack item(ItemStack item, String name, List<Component> lore) {
        item.editMeta(meta -> {
            meta.displayName(MINI_MESSAGE.deserialize(name));
            meta.lore(lore);
        });
        return item;
    }

    // Whether a name, without its formatting, contains the searched text, ignoring case.
    public static boolean nameContains(String name, String search) {
        String plain = MINI_MESSAGE.stripTags(name).toLowerCase(Locale.ROOT);
        return plain.contains(search.toLowerCase(Locale.ROOT));
    }
}
