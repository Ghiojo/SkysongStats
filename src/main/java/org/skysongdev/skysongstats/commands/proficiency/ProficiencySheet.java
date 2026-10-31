package org.skysongdev.skysongstats.commands.proficiency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// What /proficiency view shows: proficiency.sheet of messages.yml, with {focus}
// and {proficiencies} filled in. {proficiencies} has one sheet-skill line per
// skill that has proficiencies, in skills.yml order, each listing that skill's
// proficiencies with their tiers in proficiencies.yml order.
public final class ProficiencySheet {
    private static final String SHEET = "proficiency.sheet";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Supplier<Messages> messages;

    public ProficiencySheet(Supplier<Messages> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void send(CommandSender to, String player, ProficiencyService proficiencies, SkillsConfig skills,
                     ProfileKey character) {
        lines(player, proficiencies, skills, character).forEach(to::sendMessage);
    }

    List<Component> lines(String player, ProficiencyService proficiencies, SkillsConfig skills, ProfileKey character) {
        List<Component> skillLines = skills.skills().all().stream()
                .filter(skill -> !proficiencies.config().proficiencies().ofSkill(skill.id()).isEmpty())
                .map(skill -> skillLine(skill, proficiencies, character))
                .toList();
        return messages.get().getLines(SHEET, Map.of("player", player), Map.of(
                "focus", focus(proficiencies, character),
                "proficiencies", Component.join(JoinConfiguration.newlines(), skillLines)));
    }

    private Component skillLine(SkillDefinition skill, ProficiencyService proficiencies, ProfileKey character) {
        List<Component> entries = proficiencies.config().proficiencies().ofSkill(skill.id()).stream()
                .map(proficiency -> entry(proficiency, proficiencies.tier(character, proficiency)))
                .toList();
        Component separator = messages.get().get(SHEET + "-separator");
        return messages.get().get(SHEET + "-skill", Map.of(), Map.of(
                "skill", colored(skill.color(), skill.name()),
                "proficiencies", Component.join(JoinConfiguration.separator(separator), entries)));
    }

    private Component entry(ProficiencyDefinition proficiency, ProficiencyTier tier) {
        return messages.get().get(SHEET + "-entry", Map.of(), Map.of(
                "proficiency", colored(proficiency.color(), proficiency.name()),
                "tier", colored(tier.color(), tier.name())));
    }

    private Component focus(ProficiencyService proficiencies, ProfileKey character) {
        return proficiencies.focus(character)
                .map(focus -> focusLine(proficiencies, character, focus))
                .orElseGet(() -> messages.get().get(SHEET + "-no-focus"));
    }

    private Component focusLine(ProficiencyService proficiencies, ProfileKey character, ProficiencyFocus focus) {
        ProficiencyTier tier = proficiencies.tier(character, focus.proficiency());
        ProficiencyTier next = proficiencies.config().tiers().next(tier).orElse(tier);
        return messages.get().get(SHEET + "-focus", Map.of(
                "points", Integer.toString(focus.points()),
                "cost", Integer.toString(proficiencies.pointsNeeded(character, focus))), Map.of(
                "proficiency", colored(focus.proficiency().color(), focus.proficiency().name()),
                "next", colored(next.color(), next.name())));
    }

    private static Component colored(String color, String text) {
        return MINI_MESSAGE.deserialize(color + "<text>", Placeholder.unparsed("text", text));
    }
}
