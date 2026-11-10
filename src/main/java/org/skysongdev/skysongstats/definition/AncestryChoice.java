package org.skysongdev.skysongstats.definition;

// Something the player picks on the ancestry choices page: either stats to adjust
// (StatPickChoice) or one package out of several (PackageChoice). Without stack,
// a choice can't land on a stat the same ancestry already moved the same way.
public sealed interface AncestryChoice permits StatPickChoice, PackageChoice {
    String name();

    boolean stack();
}
