package org.skysongdev.skysongstats.definition;

import java.io.File;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/stats.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them; on /ssreload the plugin then keeps the stats it already had.
public final class StatsConfigFile {
    public static final String FILE_NAME = "stats.yml";

    private StatsConfigFile() {
    }

    public static StatsConfig load(File file, String bundledText) {
        return load(file, bundledText, DefinitionFile::isItem);
    }

    static StatsConfig load(File file, String bundledText, Predicate<String> isItemMaterial) {
        return StatsConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), isItemMaterial);
    }
}
