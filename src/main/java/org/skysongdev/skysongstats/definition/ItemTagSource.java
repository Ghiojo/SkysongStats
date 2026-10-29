package org.skysongdev.skysongstats.definition;

// A Skysong item tag that adds to a stat's current value: the tag's value on each
// item in the inventory, times the stack size (how Arcrot is counted today).
// requiresCharges: only count items that still have charges left.
public record ItemTagSource(String tag, boolean requiresCharges) {
}
