package com.mossman.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mossman.core.model.Project;

import java.io.IOException;
import java.util.UUID;

/**
 * Configures GSON to round-trip {@link Project} graphs. Critically, UUID is
 * registered as a string adapter — that way {@code Map<UUID, …>} keys
 * serialize as plain JSON object keys instead of the verbose array-of-pairs
 * form GSON falls back to for non-string map keys.
 */
public final class ProjectJson {

    private static final TypeAdapter<UUID> UUID_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public UUID read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return UUID.fromString(in.nextString());
        }
    };

    private final Gson gson;

    public ProjectJson() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(UUID.class, UUID_ADAPTER.nullSafe())
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    public String toJson(Project project) {
        return gson.toJson(project);
    }

    public Project fromJson(String json) {
        return gson.fromJson(json, Project.class);
    }
}
