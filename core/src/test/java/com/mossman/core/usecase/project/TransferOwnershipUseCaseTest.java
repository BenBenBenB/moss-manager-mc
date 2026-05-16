package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferOwnershipUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final TransferOwnershipUseCase usecase = new TransferOwnershipUseCase(repo);

    @Test
    void onlyCurrentOwnerCanTransfer() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(p);
        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", Players.BOB));
        assertTrue(ex.isOwnerOnly());
    }

    @Test
    void newOwnerInheritsUnionOfRoleSets() {
        Project seeded = Projects.seeded(Players.OWNER);                  // OWNER -> Admin
        Project base   = Projects.assignByName(seeded, Players.ALICE, "Viewer");
        repo.save(base);

        UUID adminId  = Projects.roleIdByName(base, "Admin");
        UUID viewerId = Projects.roleIdByName(base, "Viewer");

        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE);

        assertEquals(Players.ALICE, after.ownerUuid());
        // ALICE's role set is the union of her existing {Viewer} and old owner's {Admin}.
        assertEquals(Set.of(adminId, viewerId), after.memberRoles().get(Players.ALICE));
        // Old owner's role set is left untouched.
        assertEquals(Set.of(adminId), after.memberRoles().get(Players.OWNER));
    }

    @Test
    void newOwnerWithoutPriorRolesGetsOldOwnersRolesOnly() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID adminId = Projects.roleIdByName(base, "Admin");

        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE);

        assertEquals(Set.of(adminId), after.memberRoles().get(Players.ALICE));
        assertEquals(Set.of(adminId), after.memberRoles().get(Players.OWNER));
    }

    @Test
    void ownerOnlyPowersResolveToNewOwnerAfterTransfer() {
        repo.save(Projects.seeded(Players.OWNER));
        usecase.execute(Players.OWNER, "test", Players.ALICE);

        // Old owner can no longer transfer.
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.OWNER, "test", Players.BOB));

        // New owner can transfer.
        Project p = usecase.execute(Players.ALICE, "test", Players.BOB);
        assertEquals(Players.BOB, p.ownerUuid());
    }

    @Test
    void cannotTransferToSelf() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", Players.OWNER));
    }

    @Test
    void missingProjectThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "absent", Players.ALICE));
    }
}
