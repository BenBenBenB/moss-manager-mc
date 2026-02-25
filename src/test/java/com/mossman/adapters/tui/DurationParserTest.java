package com.mossman.adapters.tui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {

    // --- parse() ---

    @Test
    void parse_fullWithSpaces() {
        assertEquals(1590, DurationParser.parse("1d 2h 30m"));
    }

    @Test
    void parse_fullNoSpaces() {
        assertEquals(1590, DurationParser.parse("1d2h30m"));
    }

    @Test
    void parse_hoursOnly() {
        assertEquals(120, DurationParser.parse("2h"));
    }

    @Test
    void parse_minutesOnly() {
        assertEquals(30, DurationParser.parse("30m"));
    }

    @Test
    void parse_daysOnly() {
        assertEquals(1440, DurationParser.parse("1d"));
    }

    @Test
    void parse_throwsOnBlank() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("   "));
    }

    @Test
    void parse_throwsOnNoTokens() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
    }

    @Test
    void parse_throwsOnZeroResult() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0m"));
    }

    @Test
    void parse_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(null));
    }

    // --- format() ---

    @Test
    void format_full() {
        assertEquals("1d 2h 30m", DurationParser.format(1590));
    }

    @Test
    void format_hoursAndMinutes() {
        assertEquals("2h 30m", DurationParser.format(150));
    }

    @Test
    void format_daysOnly() {
        assertEquals("1d", DurationParser.format(1440));
    }

    @Test
    void format_minutesOnly() {
        assertEquals("45m", DurationParser.format(45));
    }

    @Test
    void format_zero() {
        assertEquals("0m", DurationParser.format(0));
    }

    @Test
    void format_negative() {
        assertEquals("0m", DurationParser.format(-10));
    }

    // --- splitInputAndNote() ---

    @Test
    void split_durationOnly() {
        String[] parts = DurationParser.splitInputAndNote("1d 2h 30m");
        assertEquals("1d 2h 30m", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void split_durationAndNote() {
        String[] parts = DurationParser.splitInputAndNote("2h 30m Fixed auth bug");
        assertEquals("2h 30m", parts[0]);
        assertEquals("Fixed auth bug", parts[1]);
    }

    @Test
    void split_durationAndNoteNoSpace() {
        String[] parts = DurationParser.splitInputAndNote("1hFixed auth bug");
        // "1h" is a token, then "Fixed auth bug" is not a duration token
        assertEquals("1h", parts[0]);
        assertEquals("Fixed auth bug", parts[1]);
    }

    @Test
    void split_justNote() {
        String[] parts = DurationParser.splitInputAndNote("some note without duration");
        assertEquals("", parts[0]);
        assertEquals("some note without duration", parts[1]);
    }

    @Test
    void split_null() {
        String[] parts = DurationParser.splitInputAndNote(null);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }
}
