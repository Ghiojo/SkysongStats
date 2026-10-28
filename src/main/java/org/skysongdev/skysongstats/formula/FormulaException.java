package org.skysongdev.skysongstats.formula;

// Thrown when a formula's text cannot be parsed. The position is the 0-based
// character index where the problem was found, so config errors can point at it.
public class FormulaException extends IllegalArgumentException {
    private final String source;
    private final int position;

    public FormulaException(String message, String source, int position) {
        super(message + " at position " + (position + 1) + " in formula \"" + source + "\"");
        this.source = source;
        this.position = position;
    }

    public String getSource() {
        return source;
    }

    public int getPosition() {
        return position;
    }
}
