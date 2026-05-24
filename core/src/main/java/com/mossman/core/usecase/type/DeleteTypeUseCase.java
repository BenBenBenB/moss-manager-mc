package com.mossman.core.usecase.type;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketType;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DeleteTypeUseCase {

    private final ProjectRepository repository;

    public DeleteTypeUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(UUID actor, String projectId, UUID typeId, UUID replacementTypeId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(replacementTypeId, "replacementTypeId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        if (replacementTypeId.equals(typeId)) {
            throw new ValidationException("replacement type must differ from the type being deleted");
        }
        if (project.findType(typeId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TYPE, typeId.toString());
        }
        if (project.types().size() == 1) {
            throw new ValidationException("cannot delete the last remaining type");
        }
        if (project.findType(replacementTypeId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TYPE, replacementTypeId.toString());
        }

        List<TicketType> nextTypes = new ArrayList<>(project.types().size() - 1);
        for (TicketType t : project.types()) if (!t.id().equals(typeId)) nextTypes.add(t);

        List<Ticket> nextTickets = new ArrayList<>(project.tickets().size());
        for (Ticket t : project.tickets()) {
            nextTickets.add(t.typeId().equals(typeId) ? t.withType(replacementTypeId) : t);
        }

        Project updated = new Project(
                project.id(),
                project.name(),
                project.ownerUuid(),
                project.defaultRoleId(),
                project.allowNonMembers(),
                project.roles(),
                project.memberRoles(),
                project.statuses(),
                nextTypes,
                nextTickets,
                project.nextTicketNumber());
        repository.save(updated);
    }
}
