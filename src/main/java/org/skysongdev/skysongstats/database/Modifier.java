package org.skysongdev.skysongstats.database;

import org.skysongdev.skysongstats.Utils.Utils;

public class Modifier {
    private Utils.StaticStats stat;
    private int modifier;
    private PlayerStats parent;

    public Modifier(Utils.StaticStats stat, int modifier){
        this.stat = stat;
        this.modifier = modifier;
    }

    public int getModifier() {
        return modifier;
    }

    public Utils.StaticStats getStat() {
        return stat;
    }
}
