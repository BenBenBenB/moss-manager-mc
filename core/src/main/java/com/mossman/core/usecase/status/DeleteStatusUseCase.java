package com.mossman.core.usecase.status;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DeleteStatusUseCase {

    private final ProjectRepository repository;

    public DeleteStatusUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(UUID actor, String projectId, UUID statusId, UUID replacementStatusId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(statusId, "statusId");
        Objects.requireNonNull(replacementStatusId, "replacementStatusId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        if (replacementStatusId.equals(statusId)) {
            throw new ValidationException("replacement status must differ from the status being deleted");
        }
        if (project.findStatus(statusId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.STATUS, statusId.toString());
        }
        if (project.statuses().size() == 1) {
            throw new ValidationException("cannot delete the last remaining status");
        }
        if (project.findStatus(replacementStatusId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.STATUS, replacementStatusId.toString());
        }

        List<TicketStatus> nextStatuses = new ArrayList<>(project.statuses().size() - 1);
        for (TicketStatus s : project.statuses()) if (!s.id().equals(statusId)) nextStatuses.add(s);

        List<Ticket> nextTickets = new ArrayList<>(project.tickets().size());
        for (Ticket t : project.tickets()) {
            nextTickets.add(t.statusId().equals(statusId) ? t.withStatus(replacementStatusId) : t);
        }

        repository.save(project.withStatusesAndTickets(nextStatuses, nextTickets));
    }
}
