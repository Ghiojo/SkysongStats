package org.skysongdev.skysongstats.definition;

import java.io.File;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/ancestries.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them. Ancestries adjust the /setup stats of stats.yml, so stats.yml loads first.
public final class AncestriesConfigFile {
    public static final String FILE_NAME = AncestriesConfigLoader.FILE_NAME;

    private AncestriesConfigFile() {
    }

    public static AncestryDefinitions load(File file, String bundledText, StatDefinitions stats) {
        return load(file, bundledText, stats, DefinitionFile::isItem);
    }

    static AncestryDefinitions load(File file, String bundledText, StatDefinitions stats,
                                    Predicate<String> isItemMaterial) {
        return AncestriesConfigLoader.load(DefinitionFile.read(file, bundledText, FILE_NAME), stats, isItemMaterial);
    }
}
