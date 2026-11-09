package org.skysongdev.skysongstats.definition;

// A stat's "scale:" block in stats.yml: the stat is a height in centimetres, and
// its total sets the player's Minecraft scale. normal is the height that is scale
// 1.0 (a normal player is 1.8 blocks tall, so 180 cm), and the scale is linear:
// 360 cm is scale 2. Commands refuse heights outside min..max.
public record HeightScale(int normal, int min, int max) {
    // The scale attribute's limits in Minecraft.
    public static final double MIN_SCALE = 0.0625;
    public static final double MAX_SCALE = 16;

    public boolean allows(int height) {
        return height >= min && height <= max;
    }

    // The scale for a height total. Modifiers can push the total past min or max,
    // so the height used is kept within them.
    public double scaleFor(int height) {
        return (double) Math.clamp(height, min, max) / normal;
    }
}
