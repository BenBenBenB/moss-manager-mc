package com.mossman.persistence;

import com.mossman.core.model.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultProjectTemplateTest {

    private final ProjectJson json = new ProjectJson();

    @Test
    void seedsBakedTemplateWhenFileMissing(@TempDir Path dir) throws Exception {
        Path templatePath = dir.resolve("default.json");
        assertFalse(Files.exists(templatePath));

        Project loaded = DefaultProjectTemplate.load(templatePath, json);

        assertNotNull(loaded);
        assertTrue(Files.exists(templatePath), "missing template should be written to disk");
        assertEquals(Project.TEMPLATE_OWNER, loaded.ownerUuid());
        assertEquals("Default", loaded.defaultRole().name());
    }

    @Test
    void roundTripsExistingTemplate(@TempDir Path dir) throws Exception {
        Path templatePath = dir.resolve("default.json");
        Project custom = Project.create("default-template", "Custom", Project.TEMPLATE_OWNER)
                .withName("My Custom Template");
        Files.writeString(templatePath, json.toJson(custom), StandardCharsets.UTF_8);

        Project loaded = DefaultProjectTemplate.load(templatePath, json);

        assertEquals("My Custom Template", loaded.name());
        assertEquals(Project.TEMPLATE_OWNER, loaded.ownerUuid());
    }
}
