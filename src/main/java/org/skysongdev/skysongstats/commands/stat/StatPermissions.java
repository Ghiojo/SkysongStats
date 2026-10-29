package org.skysongdev.skysongstats.commands.stat;

import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;

// The permission nodes generated for every stat in stats.yml:
//   skysongstats.stat.<id>.<action>        use the action on yourself
//   skysongstats.stat.<id>.<action>.other  use it on someone else
//   skysongstats.stat.<id>.view.other      see someone else's value
public final class StatPermissions {
    private static final String PREFIX = "skysongstats.stat.";
    private static final String OTHER = ".other";

    private StatPermissions() {
    }

    public static String self(StatDefinition stat, StatAction action) {
        return PREFIX + stat.id() + "." + action.configName();
    }

    public static String other(StatDefinition stat, StatAction action) {
        return self(stat, action) + OTHER;
    }

    public static String viewOther(StatDefinition stat) {
        return PREFIX + stat.id() + ".view" + OTHER;
    }
}
