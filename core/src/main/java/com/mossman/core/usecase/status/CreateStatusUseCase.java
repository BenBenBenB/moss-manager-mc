package com.mossman.core.usecase.status;

import com.mossman.core.model.Project;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateStatusUseCase {

    private final ProjectRepository repository;
    private final Supplier<UUID> idSupplier;

    public CreateStatusUseCase(ProjectRepository repository, Supplier<UUID> idSupplier) {
        this.repository = Objects.requireNonNull(repository);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public CreateStatusUseCase(ProjectRepository repository) {
        this(repository, UUID::randomUUID);
    }

    public TicketStatus execute(UUID actor, String projectId, String name, int textColor, int backgroundColor) {
        Objects.requireNonNull(actor, "actor");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        TicketStatus status = new TicketStatus(idSupplier.get(), name, textColor, backgroundColor);
        List<TicketStatus> next = new ArrayList<>(project.statuses());
        next.add(status);
        repository.save(project.withStatuses(next));
        return status;
    }
}
