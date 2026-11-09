package org.skysongdev.skysongstats.height;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads and writes heights, which are stored in whole centimetres.
// Read: metres ("2", "1.8", "1.75", "1,75", "1.75m") or feet and inches ("5'10\"",
// "5'10''", "5'10", "6'"), with any of the apostrophes and quote marks phones and
// keyboards type. Inches go from 0 to 11, and feet are rounded to the nearest cm.
// Written: metres with two decimals ("1.75") or feet and inches ("5'9\"").
public final class HeightText {
    private static final double CM_PER_INCH = 2.54;
    private static final int INCHES_PER_FOOT = 12;
    // Feet marks: ' ’ ‘ ‛ ′ ´ `
    private static final Pattern FEET_MARKS = Pattern.compile("[\u2019\u2018\u201B\u2032\u00B4`]");
    // Inch marks: ” “ „ ‟ ″ ˝ (and '' once the feet marks are plain ')
    private static final Pattern INCH_MARKS = Pattern.compile("[\u201D\u201C\u201E\u201F\u2033\u02DD]|''");
    private static final Pattern METRES = Pattern.compile("(\\d{1,3})(?:[.,](\\d{1,2}))?m?");
    private static final Pattern FEET = Pattern.compile("(\\d{1,2})'(?:(\\d{1,2})\"?)?");
    // What the part after "5'" looks like when a space split "5' 10\"" in two.
    private static final Pattern LONE_INCHES = Pattern.compile("\\d{1,2}\"");

    private HeightText() {
    }

    // The height in centimetres, or empty when the text is not a height.
    public static OptionalInt parse(String text) {
        String height = normalize(text);
        Matcher metres = METRES.matcher(height);
        if (metres.matches()) {
            return OptionalInt.of(new BigDecimal(metres.group(1) + "." + (metres.group(2) == null ? "0" : metres.group(2)))
                    .movePointRight(2).intValueExact());
        }
        Matcher feet = FEET.matcher(height);
        if (feet.matches()) {
            int inches = feet.group(2) == null ? 0 : Integer.parseInt(feet.group(2));
            if (inches >= INCHES_PER_FOOT) {
                return OptionalInt.empty();
            }
            int totalInches = Integer.parseInt(feet.group(1)) * INCHES_PER_FOOT + inches;
            return OptionalInt.of((int) Math.round(totalInches * CM_PER_INCH));
        }
        return OptionalInt.empty();
    }

    // True for text such as 10" that is only inches: what a player typed after a
    // space in "5' 10\"".
    public static boolean isLoneInches(String text) {
        return text != null && LONE_INCHES.matcher(normalize(text)).matches();
    }

    // 175 gives "1.75".
    public static String metres(int centimetres) {
        return BigDecimal.valueOf(centimetres).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    // 175 gives "5'9\"".
    public static String feet(int centimetres) {
        long inches = Math.round(centimetres / CM_PER_INCH);
        return (inches / INCHES_PER_FOOT) + "'" + (inches % INCHES_PER_FOOT) + "\"";
    }

    // A scale such as 0.972, with at most three decimals.
    public static String scale(double scale) {
        return BigDecimal.valueOf(scale).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String normalize(String text) {
        String plainFeet = FEET_MARKS.matcher(text.trim().toLowerCase(Locale.ROOT)).replaceAll("'");
        return INCH_MARKS.matcher(plainFeet).replaceAll("\"");
    }
}
