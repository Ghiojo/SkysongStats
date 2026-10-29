package org.skysongdev.skysongstats.definition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Everything skills.yml defines: the levels, the /setup rules, the pages of the
// /skills book and the skills.
public record SkillsConfig(SkillLevels levels, SkillSetupRules setup, List<SkillBookPage> book,
                           SkillDefinitions skills) {
    public SkillsConfig {
        book = List.copyOf(book);
    }

    // The skills that no book page lists, in file order. The book shows them on an
    // extra last page so none is hidden.
    public List<SkillDefinition> skillsOnNoPage() {
        Set<String> listed = new HashSet<>();
        book.forEach(page -> listed.addAll(page.skills()));
        return skills.all().stream().filter(skill -> !listed.contains(skill.id())).toList();
    }
}
