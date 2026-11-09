package org.skysongdev.skysongstats.definition;

import java.io.File;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/conditions.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them. Effect modifiers are checked against the stats loaded from stats.yml.
public final class ConditionsConfigFile {
    public static final String FILE_NAME = ConditionsConfigLoader.FILE_NAME;

    private ConditionsConfigFile() {
    }

    public static ConditionDefinitions load(File file, String bundledText, StatDefinitions stats) {
        return load(file, bundledText, stats, DefinitionFile::isItem);
    }

    static ConditionDefinitions load(File file, String bundledText, StatDefinitions stats,
                                     Predicate<String> isItemMaterial) {
        return ConditionsConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), stats, isItemMaterial);
    }
}
