package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.RelationshipType;
import com.mossman.domain.entities.Status;
import com.mossman.domain.entities.TicketType;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.ProjectCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateProjectUseCase {

    private static final List<Status> DEFAULT_STATUSES = List.of(
            new Status("OPEN", "Open", ""),
            new Status("IN_PROGRESS", "In Progress", ""),
            new Status("IN_REVIEW", "In Review", ""),
            new Status("DONE", "Done", "")
    );

    private static final List<TicketType> DEFAULT_TICKET_TYPES = List.of(
            new TicketType("TASK", "Task", ""),
            new TicketType("BUG", "Bug", ""),
            new TicketType("FEATURE", "Feature", "")
    );

    private static final List<RelationshipType> DEFAULT_RELATIONSHIP_TYPES = List.of(
            new RelationshipType("BLOCKS", "Blocks", "blocks", "is blocked by", ""),
            new RelationshipType("DUPLICATES", "Duplicates", "duplicates", "is duplicated by", ""),
            new RelationshipType("RELATES_TO", "Relates To", "relates to", "relates to", "")
    );

    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public CreateProjectUseCase(ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    public Project execute(Project.Builder projectBuilder, UUID creatorId, String creatorUsername) {
        Member owner = new Member(0, creatorId, creatorUsername, "Project Owner", Permission.OWNER);

        Project project = projectBuilder
                .members(List.of(owner))
                .statuses(DEFAULT_STATUSES)
                .ticketTypes(DEFAULT_TICKET_TYPES)
                .relationshipTypes(DEFAULT_RELATIONSHIP_TYPES)
                .build();

        EntityValidator.requireValidTicketPrefix(project.getTicketPrefix());
        EntityValidator.requireValidProjectName(project.getName());
        EntityValidator.requireValidProjectDescription(project.getDescription());
        EntityValidator.requireValidIconTexture(project.getIconTexture());

        Project savedProject = projectRepository.save(project);
        eventBus.publish(new ProjectCreatedEvent(savedProject, Instant.now()));
        return savedProject;
    }
}
