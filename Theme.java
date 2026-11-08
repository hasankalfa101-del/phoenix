package dev.phoenix.util;

public final class Theme {
    public static int accent = 0xFF2F7BFF;

    public static final int[] PRESETS = {
        0xFF2F7BFF, 0xFF8B5CF6, 0xFF22C55E, 0xFFEF4444,
        0xFFF59E0B, 0xFF06B6D4, 0xFFEC4899, 0xFFE5E7EB
    };
    public static final String[] NAMES = {
        "Blue", "Purple", "Green", "Red", "Orange", "Cyan", "Pink", "White"
    };

    // Colours sampled from the reference screenshot.
    public static final int PANEL = 0xF01B212B;
    public static final int ROW_ON = 0xFF2A3140;
    public static final int ROW_HOVER = 0xFF232A36;
    public static final int ROW_SUB = 0xFF171C25;
    public static final int TEXT = 0xFFDDE1E7;
    public static final int TEXT_DIM = 0xFF8A93A3;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int INPUT = 0xFF11151B;
    public static final int PILL = 0xF0161B23;
    public static final int PILL_BORDER = 0xFF2B3340;

    private Theme() {}
}
