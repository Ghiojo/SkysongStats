package org.skysongdev.skysongstats.commands.proficiency;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.MessageSink;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillDefinition;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

// Reads the proficiencies, tiers, skills and amounts typed in /proficiency. Each
// method tells the sender what was wrong and returns empty when the input isn't
// valid.
final class ProficiencyArguments {
    static final String PROFICIENCY = "proficiency.";
    private static final String NO_CHARACTER = "stats.errors.no-character";

    private final ProficiencyService proficiencies;
    private final SkillService skills;
    private final CommandTargets targets;
    private final MessageSink messages;

    ProficiencyArguments(ProficiencyService proficiencies, SkillService skills, CommandTargets targets,
                         MessageSink messages) {
        this.proficiencies = Objects.requireNonNull(proficiencies, "proficiencies");
        this.skills = Objects.requireNonNull(skills, "skills");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // The online player named, or the sender when name is null, with their active
    // character. The permissions work as in CommandTargets.
    Optional<CommandTargets.Target> target(CommandSender sender, String name, String selfPermission,
                                           String otherPermission) {
        return targets.resolve(sender, name, selfPermission, otherPermission)
                .filter(target -> hasProficiencies(sender, target));
    }

    private boolean hasProficiencies(CommandSender sender, CommandTargets.Target target) {
        if (proficiencies.hasCharacter(target.character())) {
            return true;
        }
        messages.send(sender, NO_CHARACTER, Map.of("player", target.player().getName()));
        return false;
    }

    // A proficiency by its id or one of its aliases, in any case.
    Optional<ProficiencyDefinition> proficiency(CommandSender sender, String input) {
        return found(sender, proficiencies.config().proficiencies().find(input), "unknown-proficiency", input);
    }

    Optional<ProficiencyTier> tier(CommandSender sender, String input) {
        return found(sender, proficiencies.config().tiers().get(input.toLowerCase(Locale.ROOT)), "unknown-tier", input);
    }

    // A skill from skills.yml by its id or one of its aliases, in any case.
    Optional<SkillDefinition> skill(CommandSender sender, String input) {
        return found(sender, skills.config().skills().find(input), "unknown-skill", input);
    }

    // A whole number of at least min.
    OptionalInt amount(CommandSender sender, String input, int min) {
        OptionalInt amount = parse(input, min);
        if (amount.isEmpty()) {
            messages.send(sender, PROFICIENCY + "invalid-amount", Map.of("input", input, "min", Integer.toString(min)));
        }
        return amount;
    }

    private <T> Optional<T> found(CommandSender sender, Optional<T> value, String error, String input) {
        if (value.isEmpty()) {
            messages.send(sender, PROFICIENCY + error, Map.of("input", input));
        }
        return value;
    }

    private static OptionalInt parse(String input, int min) {
        try {
            int amount = Integer.parseInt(input);
            return amount < min ? OptionalInt.empty() : OptionalInt.of(amount);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
