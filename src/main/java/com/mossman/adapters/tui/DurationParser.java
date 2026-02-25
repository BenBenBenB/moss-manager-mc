package com.mossman.adapters.tui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([dhm])");

    private DurationParser() {}

    /**
     * Parses "1d 2h 30m" (spaces optional) → total minutes. 1d=1440m, 1h=60m.
     * Throws IllegalArgumentException if blank, no valid tokens, or result <= 0.
     */
    public static long parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Duration must not be blank.");
        }
        Matcher m = TOKEN.matcher(input);
        long minutes = 0;
        boolean found = false;
        while (m.find()) {
            found = true;
            long val = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "d" -> minutes += val * 1440;
                case "h" -> minutes += val * 60;
                case "m" -> minutes += val;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("No valid duration tokens found in: " + input);
        }
        if (minutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero.");
        }
        return minutes;
    }

    /**
     * Formats minutes → "1d 2h 30m" (omits zero units; returns "0m" if zero).
     */
    public static String format(long minutes) {
        if (minutes <= 0) return "0m";
        long d = minutes / 1440;
        long remaining = minutes % 1440;
        long h = remaining / 60;
        long m = remaining % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d");
        if (h > 0) { if (sb.length() > 0) sb.append(" "); sb.append(h).append("h"); }
        if (m > 0) { if (sb.length() > 0) sb.append(" "); sb.append(m).append("m"); }
        return sb.toString();
    }

    /**
     * Splits greedy input into [durationPart, notePart].
     * Consumes leading tokens matching \d+[dhm]; remainder (trimmed) is the note.
     */
    public static String[] splitInputAndNote(String input) {
        if (input == null) return new String[]{"", ""};
        // Collect leading duration tokens
        Matcher m = TOKEN.matcher(input);
        int lastDurationEnd = 0;
        while (m.find()) {
            // Only consume tokens that are "leading" — preceded only by whitespace/digits
            // We scan greedily from the start: accept any token match
            lastDurationEnd = m.end();
        }
        // Walk forward through all token matches; consider "leading" as those where the
        // content before them (from lastDurationEnd onward) is only whitespace.
        // Re-do with a stricter left-to-right scan.
        lastDurationEnd = 0;
        int idx = 0;
        String trimmed = input.stripLeading();
        int leading = input.length() - trimmed.length();
        Matcher m2 = TOKEN.matcher(input);
        boolean scanning = true;
        int scanPos = 0;
        while (scanning && m2.find()) {
            // The text between scanPos and m2.start() must be only whitespace
            String between = input.substring(scanPos, m2.start());
            if (between.isBlank()) {
                scanPos = m2.end();
                lastDurationEnd = m2.end();
            } else {
                scanning = false;
            }
        }
        String durationPart = input.substring(0, lastDurationEnd).trim();
        String notePart = lastDurationEnd < input.length() ? input.substring(lastDurationEnd).trim() : "";
        return new String[]{durationPart, notePart};
    }
}
