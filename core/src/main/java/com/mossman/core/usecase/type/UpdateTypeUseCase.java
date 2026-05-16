package com.mossman.core.usecase.type;

import com.mossman.core.model.Project;
import com.mossman.core.model.TicketType;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UpdateTypeUseCase {

    private final ProjectRepository repository;

    public UpdateTypeUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public TicketType execute(UUID actor, String projectId, UUID typeId,
                              String newName, int newTextColor, int newBackgroundColor) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(typeId, "typeId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        if (project.findType(typeId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TYPE, typeId.toString());
        }
        TicketType updated = new TicketType(typeId, newName, newTextColor, newBackgroundColor);

        List<TicketType> next = new ArrayList<>(project.types().size());
        for (TicketType t : project.types()) next.add(t.id().equals(typeId) ? updated : t);
        repository.save(project.withTypes(next));
        return updated;
    }
}
