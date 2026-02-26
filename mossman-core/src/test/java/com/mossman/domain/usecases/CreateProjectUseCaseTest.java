package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.ProjectCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateProjectUseCaseTest {

    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private CreateProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new CreateProjectUseCase(projectRepository, eventBus);
    }

    @Test
    void testExecute_CreatesProjectWithOwner() {
        UUID creatorId = UUID.randomUUID();
        String creatorUsername = "alice";

        Project expected = Project.builder()
                .id(1L)
                .name("MyProject")
                .ticketPrefix("MP")
                .members(java.util.List.of(
                        new Member(0, creatorId, creatorUsername, "Project Owner", Permission.OWNER)
                ))
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(expected);

        Project.Builder builder = Project.builder().name("MyProject").ticketPrefix("MP");
        Project result = useCase.execute(builder, creatorId, creatorUsername);

        assertNotNull(result);
        assertEquals("MyProject", result.getName());
        assertTrue(result.getOwner().isPresent());
        assertEquals(creatorId, result.getOwner().get().uuid());
        assertEquals(Permission.OWNER, result.getOwner().get().permission());
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(eventBus, times(1)).publish(any(ProjectCreatedEvent.class));
    }

    @Test
    void testExecute_PublishesDomainEvent() {
        UUID creatorId = UUID.randomUUID();
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        useCase.execute(Project.builder().name("Proj").ticketPrefix("PRJ"), creatorId, "bob");

        verify(eventBus, times(1)).publish(any(ProjectCreatedEvent.class));
    }

    @Test
    void testExecute_ThrowsIfNameBlank() {
        UUID creatorId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(Project.builder().name("").ticketPrefix("PRJ"), creatorId, "bob"));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTicketPrefixInvalid() {
        UUID creatorId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(Project.builder().name("My Project").ticketPrefix("TOOLONGPREFIX"), creatorId, "bob"));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfNameContainsSectionSign() {
        UUID creatorId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(Project.builder().name("§red name").ticketPrefix("MP"), creatorId, "bob"));
        verify(projectRepository, never()).save(any());
    }
}
