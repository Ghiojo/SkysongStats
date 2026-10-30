package org.skysongdev.skysongstats.definition;

import java.io.File;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/skills.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them.
public final class SkillsConfigFile {
    public static final String FILE_NAME = "skills.yml";

    private SkillsConfigFile() {
    }

    public static SkillsConfig load(File file, String bundledText) {
        return load(file, bundledText, DefinitionFile::isItem);
    }

    static SkillsConfig load(File file, String bundledText, Predicate<String> isItemMaterial) {
        return SkillsConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), isItemMaterial);
    }
}
