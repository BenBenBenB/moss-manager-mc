package com.mossman.domain.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityValidatorTest {

    // ── requireValidTicketPrefix ──────────────────────────────────────────────

    @Test
    void ticketPrefix_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketPrefix("ABC"));
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketPrefix("MOSSMAN"));
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketPrefix("A1B2C3D4")); // 8 chars
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketPrefix("abc")); // lowercased → uppercased
    }

    @Test
    void ticketPrefix_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix(""));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix("   "));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix(null));
    }

    @Test
    void ticketPrefix_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix("ABCDEFGHI")); // 9 chars
    }

    @Test
    void ticketPrefix_invalidChars_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix("AB-C")); // hyphen
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketPrefix("AB C")); // space
    }

    // ── requireValidProjectName ───────────────────────────────────────────────

    @Test
    void projectName_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectName("My Project"));
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectName("A".repeat(64)));
    }

    @Test
    void projectName_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName(""));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName(null));
    }

    @Test
    void projectName_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName("A".repeat(65)));
    }

    @Test
    void projectName_controlChar_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName("Bad\u0001Name"));
    }

    @Test
    void projectName_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName("§red text"));
    }

    // ── requireValidProjectDescription ───────────────────────────────────────

    @Test
    void projectDescription_blank_ok() {
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectDescription(""));
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectDescription(null));
    }

    @Test
    void projectDescription_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectDescription("A nice description."));
        assertDoesNotThrow(() -> EntityValidator.requireValidProjectDescription("A".repeat(256)));
    }

    @Test
    void projectDescription_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectDescription("A".repeat(257)));
    }

    @Test
    void projectDescription_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectDescription("§colored"));
    }

    // ── requireValidIconTexture ───────────────────────────────────────────────

    @Test
    void iconTexture_blank_ok() {
        assertDoesNotThrow(() -> EntityValidator.requireValidIconTexture(""));
        assertDoesNotThrow(() -> EntityValidator.requireValidIconTexture(null));
    }

    @Test
    void iconTexture_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidIconTexture("minecraft:textures/item/apple.png"));
        assertDoesNotThrow(() -> EntityValidator.requireValidIconTexture("mossman:icons/my-icon.png"));
    }

    @Test
    void iconTexture_tooLong_throws() {
        String longPath = "minecraft:" + "a".repeat(120);
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidIconTexture(longPath));
    }

    @Test
    void iconTexture_invalidPattern_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidIconTexture("notaresourcelocation"));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidIconTexture("UPPER:case"));
    }

    // ── requireValidTicketTitle ───────────────────────────────────────────────

    @Test
    void ticketTitle_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketTitle("Fix the login bug"));
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketTitle("A".repeat(128)));
    }

    @Test
    void ticketTitle_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle(""));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle(null));
    }

    @Test
    void ticketTitle_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle("A".repeat(129)));
    }

    @Test
    void ticketTitle_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle("§red title"));
    }

    @Test
    void ticketTitle_controlChar_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle("bad\u001Ftitle"));
    }

    // ── requireValidTicketDescription ────────────────────────────────────────

    @Test
    void ticketDescription_blank_ok() {
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketDescription(""));
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketDescription(null));
    }

    @Test
    void ticketDescription_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidTicketDescription("A".repeat(2048)));
    }

    @Test
    void ticketDescription_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketDescription("A".repeat(2049)));
    }

    @Test
    void ticketDescription_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketDescription("§injected"));
    }

    // ── requireValidShortName ─────────────────────────────────────────────────

    @Test
    void shortName_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidShortName("status name", "IN_PROGRESS"));
        assertDoesNotThrow(() -> EntityValidator.requireValidShortName("status name", "A".repeat(32)));
    }

    @Test
    void shortName_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("status name", ""));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("status name", null));
    }

    @Test
    void shortName_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("status name", "A".repeat(33)));
    }

    @Test
    void shortName_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("status name", "§bad"));
    }

    @Test
    void shortName_withSpace_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("type name", "blocks forever"));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("type name", " leading"));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidShortName("type name", "trailing "));
    }

    @Test
    void shortName_unicodeNoWhitespace_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidShortName("type name", "BLOCKS"));
        assertDoesNotThrow(() -> EntityValidator.requireValidShortName("type name", "blocks_forever"));
        assertDoesNotThrow(() -> EntityValidator.requireValidShortName("type name", "関連")); // Japanese, no spaces
    }

    // ── requireValidRelTypeDescription ───────────────────────────────────────

    @Test
    void relTypeDescription_blank_ok() {
        assertDoesNotThrow(() -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", ""));
        assertDoesNotThrow(() -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", null));
    }

    @Test
    void relTypeDescription_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", "blocks"));
        assertDoesNotThrow(() -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", "A".repeat(64)));
    }

    @Test
    void relTypeDescription_tooLong_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", "A".repeat(65)));
    }

    @Test
    void relTypeDescription_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", "§colored"));
    }

    // ── requireValidMemberTitle ───────────────────────────────────────────────

    @Test
    void memberTitle_blank_ok() {
        assertDoesNotThrow(() -> EntityValidator.requireValidMemberTitle(""));
        assertDoesNotThrow(() -> EntityValidator.requireValidMemberTitle(null));
    }

    @Test
    void memberTitle_valid() {
        assertDoesNotThrow(() -> EntityValidator.requireValidMemberTitle("Project Owner"));
        assertDoesNotThrow(() -> EntityValidator.requireValidMemberTitle("A".repeat(64)));
    }

    @Test
    void memberTitle_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidMemberTitle("A".repeat(65)));
    }

    @Test
    void memberTitle_sectionSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidMemberTitle("§Owner"));
    }

    // ── DEL character (0x7F) ─────────────────────────────────────────────────

    @Test
    void del_char_rejected() {
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidProjectName("bad\u007Fname"));
        assertThrows(IllegalArgumentException.class, () -> EntityValidator.requireValidTicketTitle("bad\u007Ftitle"));
    }
}
