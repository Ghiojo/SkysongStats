package org.skysongdev.skysongstats.definition;

import java.util.List;

// One page of the /skills book: its title and the ids of its skills, in order.
public record SkillBookPage(String id, String title, List<String> skills) {
    public SkillBookPage {
        skills = List.copyOf(skills);
    }
}
