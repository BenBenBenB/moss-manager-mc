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
import java.util.function.Supplier;

public final class CreateTypeUseCase {

    private final ProjectRepository repository;
    private final Supplier<UUID> idSupplier;

    public CreateTypeUseCase(ProjectRepository repository, Supplier<UUID> idSupplier) {
        this.repository = Objects.requireNonNull(repository);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public CreateTypeUseCase(ProjectRepository repository) {
        this(repository, UUID::randomUUID);
    }

    public TicketType execute(UUID actor, String projectId, String name, int textColor, int backgroundColor) {
        Objects.requireNonNull(actor, "actor");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        TicketType type = new TicketType(idSupplier.get(), name, textColor, backgroundColor);
        List<TicketType> next = new ArrayList<>(project.types());
        next.add(type);
        repository.save(project.withTypes(next));
        return type;
    }
}
