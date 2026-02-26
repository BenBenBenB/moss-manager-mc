package com.mossman.domain.validation;

import java.util.regex.Pattern;

public final class EntityValidator {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Z0-9]{1,8}$");
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+:[a-z0-9_./\\-]+$");
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Z0-9_]+");

    private EntityValidator() {}

    /** Rejects codepoints 0x00–0x1F, 0x7F (DEL), and 0xA7 (§ section sign). */
    private static boolean hasSafeUnicode(String value) {
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            if (cp <= 0x1F || cp == 0x7F || cp == 0xA7) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    private static void requireSafeUnicode(String fieldName, String value) {
        if (!hasSafeUnicode(value)) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName + ": contains control characters or § (section sign).");
        }
    }

    /** Validates ticket prefix: uppercase alphanumeric, 1–8 chars. Case-insensitive — uppercases before matching. */
    public static void requireValidTicketPrefix(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ticketPrefix must not be blank.");
        }
        String upper = value.toUpperCase();
        if (!ALPHANUMERIC_PATTERN.matcher(upper).matches()) {
            throw new IllegalArgumentException(
                    "Invalid ticketPrefix: \"" + value + "\". Must be 1–8 uppercase alphanumeric characters (A-Z, 0-9).");
        }
    }

    /** Validates project name: required, max 64 chars, safe unicode. */
    public static void requireValidProjectName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank.");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException("Project name must not exceed 64 characters.");
        }
        requireSafeUnicode("name", value);
    }

    /** Validates project description: optional (blank/null ok), max 256 chars, safe unicode. */
    public static void requireValidProjectDescription(String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 256) {
            throw new IllegalArgumentException("Project description must not exceed 256 characters.");
        }
        requireSafeUnicode("description", value);
    }

    /** Validates icon texture: optional (blank/null ok), max 128 chars, safe unicode, resource location pattern. */
    public static void requireValidIconTexture(String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 128) {
            throw new IllegalArgumentException("iconTexture must not exceed 128 characters.");
        }
        requireSafeUnicode("iconTexture", value);
        if (!RESOURCE_LOCATION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid iconTexture: \"" + value + "\". Must be a resource location (e.g. minecraft:textures/item/apple.png).");
        }
    }

    /** Validates ticket title: required, max 128 chars, safe unicode. */
    public static void requireValidTicketTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ticket title must not be blank.");
        }
        if (value.length() > 128) {
            throw new IllegalArgumentException("Ticket title must not exceed 128 characters.");
        }
        requireSafeUnicode("title", value);
    }

    /** Validates ticket description: optional (blank/null ok), max 2048 chars, safe unicode. */
    public static void requireValidTicketDescription(String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 2048) {
            throw new IllegalArgumentException("Ticket description must not exceed 2048 characters.");
        }
        requireSafeUnicode("description", value);
    }

    /** Validates a short name (status, ticket type, relationship type name): required, max 32 chars, safe unicode, no whitespace. */
    public static void requireValidShortName(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        if (value.length() > 32) {
            throw new IllegalArgumentException(fieldName + " must not exceed 32 characters.");
        }
        requireSafeUnicode(fieldName, value);
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            if (Character.isWhitespace(cp)) {
                throw new IllegalArgumentException(fieldName + " must not contain whitespace.");
            }
            i += Character.charCount(cp);
        }
    }

    /** Validates a ticket label: required, max 64 chars, safe unicode, no pipe character (used as storage delimiter). */
    public static void requireValidLabel(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Label must not be blank.");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException("Label must not exceed 64 characters.");
        }
        requireSafeUnicode("label", value);
        if (value.contains("|")) {
            throw new IllegalArgumentException("Label must not contain the '|' character.");
        }
    }

    /** Validates a settings key: required, max 32 chars, uppercase letters/digits/underscores only (e.g. OPEN, IN_PROGRESS). */
    public static void requireValidKey(String fieldLabel, String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldLabel + " is required.");
        if (value.length() > 32)
            throw new IllegalArgumentException(fieldLabel + " must be 32 characters or fewer.");
        if (!KEY_PATTERN.matcher(value).matches())
            throw new IllegalArgumentException(fieldLabel + " must be uppercase letters, digits, and underscores only (e.g. OPEN, IN_PROGRESS).");
    }

    /** Validates a display name: required, max 64 chars, no control characters or section sign. */
    public static void requireValidDisplayName(String fieldLabel, String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldLabel + " is required.");
        if (value.length() > 64)
            throw new IllegalArgumentException(fieldLabel + " must be 64 characters or fewer.");
        for (char c : value.toCharArray())
            if (c < 0x20 || c == 0x7F || c == '\u00A7')
                throw new IllegalArgumentException(fieldLabel + " contains invalid characters.");
    }

    /** Validates a relationship type description field: optional (blank/null ok), max 64 chars, safe unicode. */
    public static void requireValidRelTypeDescription(String fieldName, String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 64) {
            throw new IllegalArgumentException(fieldName + " must not exceed 64 characters.");
        }
        requireSafeUnicode(fieldName, value);
    }

    /** Validates member title: optional (blank/null ok), max 64 chars, safe unicode. */
    public static void requireValidMemberTitle(String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 64) {
            throw new IllegalArgumentException("Member title must not exceed 64 characters.");
        }
        requireSafeUnicode("title", value);
    }
}
