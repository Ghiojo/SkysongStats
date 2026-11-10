package org.skysongdev.skysongstats.character;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.definition.AncestriesConfigLoader;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

/** The stats and ancestries the ancestry rule and service tests share. */
final class AncestryFixtures {
    static final StatDefinitions STATS = StatsConfigLoader.load(yaml("""
            stats:
              str: {setup: {icon: IRON_SWORD, slot: 9, min: -3, max: 4}}
              dex: {setup: {icon: BOW, slot: 11, min: -3, max: 4}}
              con: {setup: {icon: APPLE, slot: 13, min: -3, max: 4}}
              spd: {baseline: 6, setup: {icon: FEATHER, slot: 15, min: -3, max: 4}}
            """), name -> true).stats();

    // dwarf: fixed only. human: two boosts to any stat and a flaw. orc: packages.
    // giant: a choice that may stack on its fixed str.
    static final AncestryDefinitions ANCESTRIES = AncestriesConfigLoader.load(yaml("""
            ancestries:
              dwarf: {icon: STONE, slot: 10, stats: {con: 1, spd: -1}}
              human:
                icon: STONE
                slot: 11
                stats: {dex: 1}
                choices:
                  - {amount: 1, picks: 2, from: any}
                  - {amount: -1, from: [str, con]}
              orc:
                icon: STONE
                slot: 12
                stats: {str: 1}
                choices:
                  - options:
                      - {name: Brawler, stats: {con: 2}}
                      - {name: Runner, stats: {dex: 1, spd: 1}}
                  - {amount: 1, from: [dex, con]}
              giant:
                icon: STONE
                slot: 13
                stats: {str: 1}
                choices: [{amount: 1, from: [str, con], stack: true}]
            """), STATS, name -> true);

    private AncestryFixtures() {
    }

    static AncestryDefinition ancestry(String id) {
        return ANCESTRIES.get(id).orElseThrow();
    }

    static YamlConfiguration yaml(String text) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return config;
    }
}
