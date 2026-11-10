package org.skysongdev.skysongstats.apiimpl;

import org.skysongdev.skysongstats.api.Ancestry;
import org.skysongdev.skysongstats.api.AncestryAdjustment;
import org.skysongdev.skysongstats.api.AncestryChange;
import org.skysongdev.skysongstats.api.AncestryChoice;
import org.skysongdev.skysongstats.api.AncestryPackage;
import org.skysongdev.skysongstats.api.CharacterAncestry;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.character.AncestryService;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.definition.PackageChoice;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatPickChoice;
import org.skysongdev.skysongstats.storage.StoredAncestry;

import java.util.List;
import java.util.Locale;

// Turns the plugin's ancestry types into the API's and back.
public final class AncestryApiTypes {

    private AncestryApiTypes() {
    }

    static Ancestry ancestry(AncestryDefinition ancestry) {
        return new Ancestry(ancestry.id(), ancestry.name(), AfflictionApiTypes.icon(ancestry.icon()), ancestry.lore(),
                ancestry.permission().orElse(""), adjustments(ancestry.adjustments()),
                ancestry.choices().stream().map(AncestryApiTypes::choice).toList());
    }

    private static AncestryChoice choice(org.skysongdev.skysongstats.definition.AncestryChoice choice) {
        if (choice instanceof StatPickChoice stats) {
            return new AncestryChoice.Stats(stats.name(), stats.amount(), stats.picks(), stats.from(), stats.anyStat(),
                    stats.stack());
        }
        PackageChoice packages = (PackageChoice) choice;
        return new AncestryChoice.Packages(packages.name(), packages.options().stream()
                .map(option -> new AncestryPackage(option.name(), AfflictionApiTypes.icon(option.icon()),
                        adjustments(option.adjustments())))
                .toList(), packages.stack());
    }

    private static List<AncestryAdjustment> adjustments(
            List<org.skysongdev.skysongstats.definition.AncestryAdjustment> adjustments) {
        return adjustments.stream().map(adjustment -> new AncestryAdjustment(adjustment.stat(), adjustment.amount()))
                .toList();
    }

    // Named as ancestries.yml has it now, or by its id when it has left the file.
    public static CharacterAncestry characterAncestry(StoredAncestry saved, AncestryDefinitions definitions) {
        String name = definitions.get(saved.ancestry()).map(AncestryDefinition::name).orElse(saved.ancestry());
        return new CharacterAncestry(saved.ancestry(), name, saved.choicesPending(), saved.adjustments().stream()
                .map(given -> new CharacterAncestry.Given(given.stat(), given.amount(), given.choiceIndex()))
                .toList());
    }

    static AncestryChange change(AncestryService.AncestryChange change, AncestryDefinitions definitions) {
        return new AncestryChange(ApiTypes.character(change.character()),
                change.before().map(saved -> characterAncestry(saved, definitions)).orElse(null),
                change.after().map(saved -> characterAncestry(saved, definitions)).orElse(null));
    }

    // Stats named by alias become their id; unknown names stay, for the rules to report.
    static AncestryPicks picks(org.skysongdev.skysongstats.api.AncestryPicks picks, StatDefinitions stats) {
        AncestryPicks converted = AncestryPicks.none();
        for (int choice : picks.choices()) {
            int position = choice;
            converted = picks.option(position).isPresent()
                    ? converted.withPackage(position, picks.option(position).getAsInt())
                    : converted.withStats(position, picks.stats(position).orElse(List.of()).stream()
                            .map(stat -> stats.find(stat).map(StatDefinition::id).orElse(stat.toLowerCase(Locale.ROOT)))
                            .toList());
        }
        return converted;
    }
}
