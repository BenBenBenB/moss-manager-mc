package com.mossman.api.event;

import com.mossman.core.model.Project;

public sealed interface ProjectEvent {

    record Created(Project project) implements ProjectEvent {}

    record Deleted(String projectId) implements ProjectEvent {}

    record Updated(Project before, Project after) implements ProjectEvent {}
}
