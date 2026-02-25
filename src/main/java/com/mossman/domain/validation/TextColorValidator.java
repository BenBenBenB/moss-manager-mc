package com.mossman.domain.validation;

import java.util.Set;
import java.util.regex.Pattern;

public final class TextColorValidator {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static final Set<String> NAMED_COLORS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue",
            "green", "aqua", "red", "light_purple", "yellow", "white"
    );

    private TextColorValidator() {}

    /** Returns true for blank/null (= no color), a valid named color, or a valid #RRGGBB hex string. */
    public static boolean isValid(String color) {
        if (color == null || color.isBlank()) return true;
        return NAMED_COLORS.contains(color.toLowerCase()) || HEX_PATTERN.matcher(color).matches();
    }

    /** Throws {@link IllegalArgumentException} if the color string is not blank and not a valid color. */
    public static void requireValid(String fieldName, String color) {
        if (!isValid(color)) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName + ": \"" + color + "\". Use a Minecraft color name (e.g. gold) or a hex code (e.g. #FF5500).");
        }
    }
}
