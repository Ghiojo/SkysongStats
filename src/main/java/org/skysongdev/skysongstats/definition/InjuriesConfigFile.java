package org.skysongdev.skysongstats.definition;

import java.io.File;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/injuries.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them. Effects are checked against the stats from stats.yml and the conditions
// from conditions.yml, so conditions.yml is loaded first.
public final class InjuriesConfigFile {
    public static final String FILE_NAME = InjuriesConfigLoader.FILE_NAME;

    private InjuriesConfigFile() {
    }

    public static InjuryDefinitions load(File file, String bundledText, StatDefinitions stats,
                                         ConditionDefinitions conditions) {
        return load(file, bundledText, stats, conditions, DefinitionFile::isItem);
    }

    static InjuryDefinitions load(File file, String bundledText, StatDefinitions stats,
                                  ConditionDefinitions conditions, Predicate<String> isItemMaterial) {
        return InjuriesConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), stats, conditions,
                isItemMaterial);
    }
}
