package org.skysongdev.skysongstats.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkillChange;
import org.skysongdev.skysongstats.api.SkillLevel;
import org.skysongdev.skysongstats.api.Skills;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;

// Skills answered by the skill service. Each call reads the current skills.yml, so a
// Skill kept across /ssreload is looked up again by its id.
final class SkillsImpl implements Skills {
    private final SkillService skills;
    private final MainThread mainThread;

    SkillsImpl(SkillService skills, MainThread mainThread) {
        this.skills = skills;
        this.mainThread = mainThread;
    }

    @Override
    public @NotNull List<Skill> all() {
        return skills.config().skills().all().stream().map(SkillsImpl::toApi).toList();
    }

    @Override
    public @NotNull Optional<Skill> find(@NotNull String idOrAlias) {
        Objects.requireNonNull(idOrAlias, "idOrAlias");
        return skills.config().skills().find(idOrAlias).map(SkillsImpl::toApi);
    }

    @Override
    public @NotNull List<SkillLevel> levels() {
        return skills.config().levels().all().stream().map(SkillsImpl::toApi).toList();
    }

    @Override
    public @NotNull SkillLevel levelFor(int xp) {
        return toApi(skills.config().levels().levelFor(xp));
    }

    @Override
    public @NotNull OptionalInt xp(@NotNull CharacterId character, @NotNull Skill skill) {
        ProfileKey key = checked(character, skill);
        return definition(skill).map(definition -> skills.xp(key, definition)).orElse(OptionalInt.empty());
    }

    @Override
    public @NotNull Optional<SkillLevel> level(@NotNull CharacterId character, @NotNull Skill skill) {
        ProfileKey key = checked(character, skill);
        if (!skills.hasCharacter(key)) {
            return Optional.empty();
        }
        return definition(skill).map(definition -> toApi(skills.level(key, definition)));
    }

    @Override
    public @NotNull Optional<SkillChange> setXp(@NotNull CharacterId character, @NotNull Skill skill, int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP can't be negative: " + xp);
        }
        return write(character, skill, (key, definition) -> skills.setXp(key, definition, xp));
    }

    @Override
    public @NotNull Optional<SkillChange> addXp(@NotNull CharacterId character, @NotNull Skill skill, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount can't be negative: " + amount);
        }
        return write(character, skill, (key, definition) -> skills.addXp(key, definition, amount));
    }

    private Optional<SkillChange> write(CharacterId character, Skill skill,
            BiFunction<ProfileKey, SkillDefinition, org.skysongdev.skysongstats.character.SkillChange> change) {
        ProfileKey key = checked(character, skill);
        if (!skills.hasCharacter(key)) {
            return Optional.empty();
        }
        return definition(skill).map(definition -> toApi(change.apply(key, definition)));
    }

    // Checks the arguments and the thread, and gives the internal key.
    private ProfileKey checked(CharacterId character, Skill skill) {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(skill, "skill");
        mainThread.check();
        return Keys.toInternal(character);
    }

    // The skill as skills.yml has it now, matched by id only: an alias can move to
    // another skill on reload, an id can't.
    private Optional<SkillDefinition> definition(Skill skill) {
        return skills.config().skills().get(skill.id());
    }

    static Skill toApi(SkillDefinition skill) {
        return new Skill(skill.id(), skill.name(), skill.shortName(), skill.color(), skill.aliases());
    }

    static SkillLevel toApi(org.skysongdev.skysongstats.definition.SkillLevel level) {
        return new SkillLevel(level.id(), level.name(), level.color(), level.xp());
    }

    static SkillChange toApi(org.skysongdev.skysongstats.character.SkillChange change) {
        return new SkillChange(Keys.toApi(change.character()), toApi(change.skill()), change.before(),
                change.after());
    }
}
