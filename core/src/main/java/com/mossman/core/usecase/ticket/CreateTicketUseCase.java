package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateTicketUseCase {

    private final ProjectRepository repository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public CreateTicketUseCase(ProjectRepository repository, Clock clock, Supplier<UUID> idSupplier) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public CreateTicketUseCase(ProjectRepository repository) {
        this(repository, Clock.systemUTC(), UUID::randomUUID);
    }

    public Ticket execute(UUID actor, String projectId, String title, String description,
                          UUID assignee, UUID statusId, UUID typeId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(statusId, "statusId");
        Objects.requireNonNull(typeId, "typeId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.CREATE_TICKETS);
        if (project.findStatus(statusId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.STATUS, statusId.toString());
        }
        if (project.findType(typeId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TYPE, typeId.toString());
        }

        int nextNumber = project.tickets().stream()
                .mapToInt(Ticket::number)
                .max()
                .orElse(0) + 1;
        Ticket ticket = new Ticket(
                idSupplier.get(),
                nextNumber,
                title,
                description,
                assignee,
                statusId,
                typeId,
                clock.millis());
        repository.save(project.addTicket(ticket));
        return ticket;
    }
}
