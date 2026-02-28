package com.mossman;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.usecases.*;
import com.mossman.infrastructure.persistence.*;
import com.mossman.infrastructure.persistence.DatabaseManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Public API for the MossMan core mod. Other mods (mossman-tui, mossman-gui)
 * access use cases and repositories through this class rather than through
 * the mod initializer directly.
 */
public class MossManApi {

    private static DomainEventBus eventBus;
    private static DatabaseManager databaseManager;

    private static CreateProjectUseCase createProjectUseCase;
    private static CreateTicketUseCase createTicketUseCase;
    private static UpdateTicketUseCase updateTicketUseCase;
    private static UpdateProjectUseCase updateProjectUseCase;
    private static AddMemberUseCase addMemberUseCase;
    private static UpdateMemberUseCase updateMemberUseCase;
    private static RemoveMemberUseCase removeMemberUseCase;
    private static TransferOwnershipUseCase transferOwnershipUseCase;
    private static UpdateProjectStatusesUseCase updateProjectStatusesUseCase;
    private static UpdateProjectTicketTypesUseCase updateProjectTicketTypesUseCase;
    private static UpdateProjectRelationshipTypesUseCase updateProjectRelationshipTypesUseCase;
    private static AssignTicketUseCase assignTicketUseCase;
    private static UnassignTicketUseCase unassignTicketUseCase;
    private static ObserveTicketUseCase observeTicketUseCase;
    private static UnobserveTicketUseCase unobserveTicketUseCase;
    private static SendMailUseCase sendMailUseCase;
    private static MarkMailReadUseCase markMailReadUseCase;
    private static LinkTicketsUseCase linkTicketsUseCase;
    private static UnlinkTicketsUseCase unlinkTicketsUseCase;
    private static AddCommentUseCase addCommentUseCase;
    private static DeleteCommentUseCase deleteCommentUseCase;
    private static LogTimeUseCase logTimeUseCase;
    private static DeleteTimeLogUseCase deleteTimeLogUseCase;

    private static OrmLiteProjectRepository projectRepository;
    private static OrmLiteTicketRepository ticketRepository;
    private static OrmLiteMemberRepository memberRepository;
    private static OrmLiteMailRepository mailRepository;
    private static OrmLiteTicketRelationshipRepository ticketRelationshipRepository;
    private static OrmLiteCommentRepository commentRepository;
    private static OrmLitePlayerSettingsRepository playerSettingsRepository;
    private static OrmLiteTimeLogRepository timeLogRepository;

    private MossManApi() {}

    /** Called by {@link MossManMod} during initialization. Package-private. */
    static void initialize(
            DomainEventBus eventBus,
            DatabaseManager databaseManager,
            OrmLiteProjectRepository projectRepository,
            OrmLiteTicketRepository ticketRepository,
            OrmLiteMemberRepository memberRepository,
            OrmLiteMailRepository mailRepository,
            OrmLiteTicketRelationshipRepository ticketRelationshipRepository,
            OrmLiteCommentRepository commentRepository,
            OrmLitePlayerSettingsRepository playerSettingsRepository,
            OrmLiteTimeLogRepository timeLogRepository,
            CreateProjectUseCase createProjectUseCase,
            CreateTicketUseCase createTicketUseCase,
            UpdateTicketUseCase updateTicketUseCase,
            UpdateProjectUseCase updateProjectUseCase,
            AddMemberUseCase addMemberUseCase,
            UpdateMemberUseCase updateMemberUseCase,
            RemoveMemberUseCase removeMemberUseCase,
            TransferOwnershipUseCase transferOwnershipUseCase,
            UpdateProjectStatusesUseCase updateProjectStatusesUseCase,
            UpdateProjectTicketTypesUseCase updateProjectTicketTypesUseCase,
            UpdateProjectRelationshipTypesUseCase updateProjectRelationshipTypesUseCase,
            AssignTicketUseCase assignTicketUseCase,
            UnassignTicketUseCase unassignTicketUseCase,
            ObserveTicketUseCase observeTicketUseCase,
            UnobserveTicketUseCase unobserveTicketUseCase,
            SendMailUseCase sendMailUseCase,
            MarkMailReadUseCase markMailReadUseCase,
            LinkTicketsUseCase linkTicketsUseCase,
            UnlinkTicketsUseCase unlinkTicketsUseCase,
            AddCommentUseCase addCommentUseCase,
            DeleteCommentUseCase deleteCommentUseCase,
            LogTimeUseCase logTimeUseCase,
            DeleteTimeLogUseCase deleteTimeLogUseCase) {

        MossManApi.eventBus = eventBus;
        MossManApi.databaseManager = databaseManager;
        MossManApi.projectRepository = projectRepository;
        MossManApi.ticketRepository = ticketRepository;
        MossManApi.memberRepository = memberRepository;
        MossManApi.mailRepository = mailRepository;
        MossManApi.ticketRelationshipRepository = ticketRelationshipRepository;
        MossManApi.commentRepository = commentRepository;
        MossManApi.playerSettingsRepository = playerSettingsRepository;
        MossManApi.timeLogRepository = timeLogRepository;
        MossManApi.createProjectUseCase = createProjectUseCase;
        MossManApi.createTicketUseCase = createTicketUseCase;
        MossManApi.updateTicketUseCase = updateTicketUseCase;
        MossManApi.updateProjectUseCase = updateProjectUseCase;
        MossManApi.addMemberUseCase = addMemberUseCase;
        MossManApi.updateMemberUseCase = updateMemberUseCase;
        MossManApi.removeMemberUseCase = removeMemberUseCase;
        MossManApi.transferOwnershipUseCase = transferOwnershipUseCase;
        MossManApi.updateProjectStatusesUseCase = updateProjectStatusesUseCase;
        MossManApi.updateProjectTicketTypesUseCase = updateProjectTicketTypesUseCase;
        MossManApi.updateProjectRelationshipTypesUseCase = updateProjectRelationshipTypesUseCase;
        MossManApi.assignTicketUseCase = assignTicketUseCase;
        MossManApi.unassignTicketUseCase = unassignTicketUseCase;
        MossManApi.observeTicketUseCase = observeTicketUseCase;
        MossManApi.unobserveTicketUseCase = unobserveTicketUseCase;
        MossManApi.sendMailUseCase = sendMailUseCase;
        MossManApi.markMailReadUseCase = markMailReadUseCase;
        MossManApi.linkTicketsUseCase = linkTicketsUseCase;
        MossManApi.unlinkTicketsUseCase = unlinkTicketsUseCase;
        MossManApi.addCommentUseCase = addCommentUseCase;
        MossManApi.deleteCommentUseCase = deleteCommentUseCase;
        MossManApi.logTimeUseCase = logTimeUseCase;
        MossManApi.deleteTimeLogUseCase = deleteTimeLogUseCase;
    }

    public static DomainEventBus getEventBus() { return eventBus; }
    public static DatabaseManager getDatabaseManager() { return databaseManager; }

    public static CreateProjectUseCase getCreateProjectUseCase() { return createProjectUseCase; }
    public static CreateTicketUseCase getCreateTicketUseCase() { return createTicketUseCase; }
    public static UpdateTicketUseCase getUpdateTicketUseCase() { return updateTicketUseCase; }
    public static UpdateProjectUseCase getUpdateProjectUseCase() { return updateProjectUseCase; }
    public static AddMemberUseCase getAddMemberUseCase() { return addMemberUseCase; }
    public static UpdateMemberUseCase getUpdateMemberUseCase() { return updateMemberUseCase; }
    public static RemoveMemberUseCase getRemoveMemberUseCase() { return removeMemberUseCase; }
    public static TransferOwnershipUseCase getTransferOwnershipUseCase() { return transferOwnershipUseCase; }
    public static UpdateProjectStatusesUseCase getUpdateProjectStatusesUseCase() { return updateProjectStatusesUseCase; }
    public static UpdateProjectTicketTypesUseCase getUpdateProjectTicketTypesUseCase() { return updateProjectTicketTypesUseCase; }
    public static UpdateProjectRelationshipTypesUseCase getUpdateProjectRelationshipTypesUseCase() { return updateProjectRelationshipTypesUseCase; }
    public static AssignTicketUseCase getAssignTicketUseCase() { return assignTicketUseCase; }
    public static UnassignTicketUseCase getUnassignTicketUseCase() { return unassignTicketUseCase; }
    public static ObserveTicketUseCase getObserveTicketUseCase() { return observeTicketUseCase; }
    public static UnobserveTicketUseCase getUnobserveTicketUseCase() { return unobserveTicketUseCase; }
    public static SendMailUseCase getSendMailUseCase() { return sendMailUseCase; }
    public static MarkMailReadUseCase getMarkMailReadUseCase() { return markMailReadUseCase; }
    public static LinkTicketsUseCase getLinkTicketsUseCase() { return linkTicketsUseCase; }
    public static UnlinkTicketsUseCase getUnlinkTicketsUseCase() { return unlinkTicketsUseCase; }
    public static AddCommentUseCase getAddCommentUseCase() { return addCommentUseCase; }
    public static DeleteCommentUseCase getDeleteCommentUseCase() { return deleteCommentUseCase; }
    public static LogTimeUseCase getLogTimeUseCase() { return logTimeUseCase; }
    public static DeleteTimeLogUseCase getDeleteTimeLogUseCase() { return deleteTimeLogUseCase; }

    public static OrmLiteProjectRepository getProjectRepository() { return projectRepository; }
    public static OrmLiteTicketRepository getTicketRepository() { return ticketRepository; }
    public static OrmLiteMemberRepository getMemberRepository() { return memberRepository; }
    public static OrmLiteMailRepository getMailRepository() { return mailRepository; }
    public static OrmLiteTicketRelationshipRepository getTicketRelationshipRepository() { return ticketRelationshipRepository; }
    public static OrmLiteCommentRepository getCommentRepository() { return commentRepository; }
    public static OrmLitePlayerSettingsRepository getPlayerSettingsRepository() { return playerSettingsRepository; }
    public static OrmLiteTimeLogRepository getTimeLogRepository() { return timeLogRepository; }

    /**
     * Adds a top-level subcommand to the /mossman command tree.
     *
     * Call this inside a CommandRegistrationCallback listener to extend /mossman
     * from any mod (core, TUI, GUI, or third-party).
     */
    public static void registerSubcommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            LiteralArgumentBuilder<ServerCommandSource> subcommand) {
        // Ensure the mossman root node exists (no-op if already registered).
        dispatcher.register(CommandManager.literal("mossman"));
        // Always fetch the live node — dispatcher.register() returns the newly-built
        // node, not the one already in the tree when the literal already existed.
        LiteralCommandNode<ServerCommandSource> root =
                (LiteralCommandNode<ServerCommandSource>) dispatcher.getRoot().getChild("mossman");
        root.addChild(subcommand.build());
    }
}
