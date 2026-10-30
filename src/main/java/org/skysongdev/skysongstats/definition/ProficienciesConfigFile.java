package org.skysongdev.skysongstats.definition;

import java.io.File;

// Reads plugins/SkysongStats/proficiencies.yml. The first start writes the
// plugin's default file. A file with mistakes throws a DefinitionException listing
// all of them. Every proficiency names a skill, so it is checked against the
// skills loaded from skills.yml.
public final class ProficienciesConfigFile {
    public static final String FILE_NAME = "proficiencies.yml";

    private ProficienciesConfigFile() {
    }

    public static ProficienciesConfig load(File file, String bundledText, SkillDefinitions skills) {
        return ProficienciesConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), skills);
    }
}
