package com.mossman.persistence;

import com.mossman.core.model.Project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads (or seeds) the per-world default-project template at
 * {@code <world>/mossmandata/default.json}. New projects copy this template's
 * role/status/type catalog via {@link Project#copyAsNew(String, String, java.util.UUID)}.
 *
 * <p>The template lives outside the {@code projects/} directory so
 * {@link JsonProjectRepository} won't pick it up as a player-visible project.
 * Operators may hand-edit the file; on next server start the edits take effect.
 */
public final class DefaultProjectTemplate {

    private DefaultProjectTemplate() {}

    /**
     * Reads the template from {@code templatePath}. If the file does not exist
     * (or fails to parse), writes a fresh baked-in template to that path and
     * returns it.
     */
    public static Project load(Path templatePath, ProjectJson json) {
        if (Files.exists(templatePath)) {
            try {
                String text = Files.readString(templatePath, StandardCharsets.UTF_8);
                Project parsed = json.fromJson(text);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                throw new UncheckedIOException("reading default project template " + templatePath,
                        e instanceof IOException io ? io : new IOException(e));
            }
        }
        Project baked = Project.create("default-template", "Default Template", Project.TEMPLATE_OWNER);
        try {
            Files.createDirectories(templatePath.getParent());
            Files.writeString(templatePath, json.toJson(baked), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("writing default project template " + templatePath, e);
        }
        return baked;
    }
}
