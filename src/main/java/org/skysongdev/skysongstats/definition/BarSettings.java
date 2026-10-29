package org.skysongdev.skysongstats.definition;

import org.skysongdev.skysongstats.formula.Formula;

// The settings only bars have. max is the formula for the bar's max; a new
// character starts full when startAtMax, otherwise at start. shield is the id of
// the stat that absorbs DAMAGE first (hp uses temphp), or null. canExceedMax lets
// the current value go above the max (Arcrot over a character's tolerance);
// otherwise changes stop at the max, as HP does.
public record BarSettings(Formula max, boolean startAtMax, int start, String shield, boolean canExceedMax) {
}
