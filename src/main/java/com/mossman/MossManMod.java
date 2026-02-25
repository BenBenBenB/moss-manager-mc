package com.mossman;

import com.mossman.adapters.commands.MossManCommand;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.entities.Comment;
import com.mossman.domain.events.TicketAssignedEvent;
import com.mossman.domain.events.TicketCommentedEvent;
import com.mossman.domain.events.TicketUpdatedEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mossman.infrastructure.config.ConfigManager;
import com.mossman.infrastructure.events.SimpleEventBus;
import com.mossman.infrastructure.persistence.DatabaseManager;
import com.mossman.infrastructure.persistence.OrmLiteMailRepository;
import com.mossman.infrastructure.persistence.OrmLiteProjectRepository;
import com.mossman.infrastructure.persistence.OrmLiteMemberRepository;
import com.mossman.infrastructure.persistence.OrmLiteTicketRepository;
import com.mossman.infrastructure.persistence.OrmLiteCommentRepository;
import com.mossman.infrastructure.persistence.OrmLitePlayerSettingsRepository;
import com.mossman.infrastructure.persistence.OrmLiteTicketRelationshipRepository;
import com.mossman.infrastructure.persistence.OrmLiteTimeLogRepository;
import com.mossman.domain.usecases.*;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

public class MossManMod implements ModInitializer {
    public static final String MOD_ID = "mossman";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static DatabaseManager databaseManager;
    private static SimpleEventBus eventBus;
    private static MinecraftServer server;

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

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MossMan Tracker...");

        ConfigManager.loadConfig();

        try {
            File dbFile = new File(FabricLoader.getInstance().getGameDir().toFile(), "mossman.db");
            String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            databaseManager = new DatabaseManager(dbUrl);
            eventBus = new SimpleEventBus();

            memberRepository = new OrmLiteMemberRepository(databaseManager.getMemberDao());
            projectRepository = new OrmLiteProjectRepository(databaseManager.getProjectDao(), memberRepository);
            ticketRepository = new OrmLiteTicketRepository(databaseManager.getTicketDao());
            mailRepository = new OrmLiteMailRepository(databaseManager.getMailDao());
            ticketRelationshipRepository = new OrmLiteTicketRelationshipRepository(databaseManager.getTicketRelationshipDao());
            commentRepository = new OrmLiteCommentRepository(databaseManager.getCommentDao());
            playerSettingsRepository = new OrmLitePlayerSettingsRepository(databaseManager.getPlayerSettingsDao());
            timeLogRepository = new OrmLiteTimeLogRepository(databaseManager.getTimeLogDao());

            createProjectUseCase = new CreateProjectUseCase(projectRepository, eventBus);
            createTicketUseCase = new CreateTicketUseCase(ticketRepository, projectRepository, eventBus);
            updateTicketUseCase = new UpdateTicketUseCase(ticketRepository, projectRepository, eventBus);
            updateProjectUseCase = new UpdateProjectUseCase(projectRepository, eventBus);
            addMemberUseCase = new AddMemberUseCase(projectRepository);
            updateMemberUseCase = new UpdateMemberUseCase(projectRepository);
            removeMemberUseCase = new RemoveMemberUseCase(projectRepository);
            transferOwnershipUseCase = new TransferOwnershipUseCase(projectRepository);
            updateProjectStatusesUseCase = new UpdateProjectStatusesUseCase(projectRepository);
            updateProjectTicketTypesUseCase = new UpdateProjectTicketTypesUseCase(projectRepository);
            updateProjectRelationshipTypesUseCase = new UpdateProjectRelationshipTypesUseCase(projectRepository);
            assignTicketUseCase = new AssignTicketUseCase(ticketRepository, projectRepository, eventBus);
            unassignTicketUseCase = new UnassignTicketUseCase(ticketRepository, projectRepository, eventBus);
            observeTicketUseCase = new ObserveTicketUseCase(ticketRepository, projectRepository);
            unobserveTicketUseCase = new UnobserveTicketUseCase(ticketRepository);
            sendMailUseCase = new SendMailUseCase(mailRepository);
            markMailReadUseCase = new MarkMailReadUseCase(mailRepository);
            linkTicketsUseCase = new LinkTicketsUseCase(ticketRelationshipRepository, ticketRepository, projectRepository);
            unlinkTicketsUseCase = new UnlinkTicketsUseCase(ticketRelationshipRepository, ticketRepository, projectRepository);
            addCommentUseCase = new AddCommentUseCase(commentRepository, ticketRepository, projectRepository, eventBus);
            deleteCommentUseCase = new DeleteCommentUseCase(commentRepository, ticketRepository, projectRepository);
            logTimeUseCase = new LogTimeUseCase(timeLogRepository, ticketRepository, projectRepository);
            deleteTimeLogUseCase = new DeleteTimeLogUseCase(timeLogRepository, ticketRepository, projectRepository);

            // Notify new assignee via mail
            eventBus.subscribe(TicketAssignedEvent.class, event -> {
                try {
                    var project = projectRepository.findById(event.ticket().getProjectId()).orElse(null);
                    String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                    MailMessage saved = sendMailUseCase.execute(new MailMessage(0, event.assigneeId(), null, "MossMan",
                            "Assigned to [" + key + "]",
                            "You have been assigned to [" + key + "]: " + event.ticket().getTitle(),
                            false, System.currentTimeMillis()));
                    if (server != null) deliverMailNow(saved, server);
                } catch (Exception e) {
                    LOGGER.error("Failed to send assignment notification", e);
                }
            });

            // Notify observers on ticket update (excluding the requester)
            eventBus.subscribe(TicketUpdatedEvent.class, event -> {
                try {
                    if (event.ticket().getObservers().isEmpty()) return;
                    var project = projectRepository.findById(event.ticket().getProjectId()).orElse(null);
                    String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                    String subject = "[" + key + "] was updated";
                    String body = "Ticket [" + key + "]: " + event.ticket().getTitle() + " has been updated.";
                    for (UUID observerId : event.ticket().getObservers()) {
                        if (observerId.equals(event.requesterId())) continue;
                        MailMessage saved = sendMailUseCase.execute(new MailMessage(0, observerId, null, "MossMan",
                                subject, body, false, System.currentTimeMillis()));
                        if (server != null) deliverMailNow(saved, server);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to send update notification", e);
                }
            });

            // Notify observers when a comment is added (excluding the commenter)
            eventBus.subscribe(TicketCommentedEvent.class, event -> {
                try {
                    if (event.ticket().getObservers().isEmpty()) return;
                    var project = projectRepository.findById(event.ticket().getProjectId()).orElse(null);
                    String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                    String subject = "[" + key + "] new comment";
                    String body = event.comment().authorName() + " commented on [" + key + "]: " + event.ticket().getTitle()
                            + "\n\n" + event.comment().message();
                    for (UUID observerId : event.ticket().getObservers()) {
                        if (observerId.equals(event.requesterId())) continue;
                        MailMessage saved = sendMailUseCase.execute(new MailMessage(0, observerId, event.requesterId(),
                                event.comment().authorName(), subject, body, false, System.currentTimeMillis()));
                        if (server != null) deliverMailNow(saved, server);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to send comment notification", e);
                }
            });

            LOGGER.info("Database and Use Cases initialized successfully.");
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize Database Manager", e);
        }

        if (ConfigManager.getConfig().enableCommands) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                MossManCommand.register(dispatcher);
            });
            LOGGER.info("MossMan commands registered.");
        } else {
            LOGGER.info("MossMan commands are disabled by configuration.");
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, joinServer) -> {
            MossManMod.server = joinServer;
            if (mailRepository == null) return;
            UUID playerId = handler.player.getUuid();
            long unread = mailRepository.countUnread(playerId);
            if (unread > 0) {
                MutableText msg = Text.literal("MossMan: You have " + unread
                        + " unread message" + (unread == 1 ? "" : "s") + ". ")
                        .formatted(Formatting.GOLD)
                        .append(TuiHelper.createRunLink("[View]", "/mossman mail",
                                "Open your MossMan inbox", Formatting.GREEN));
                handler.player.sendMessage(msg);
            }
        });
    }

    public static void deliverMailNow(MailMessage saved, MinecraftServer srv) {
        ServerPlayerEntity player = srv.getPlayerManager().getPlayer(saved.recipientId());
        if (player == null) return;
        MutableText notification = Text.empty()
                .append(TuiHelper.createRunLink("[#" + saved.id() + "]",
                        "/mossman mail read " + saved.id(),
                        "Open message", Formatting.GOLD))
                .append(Text.literal(" " + saved.senderName() + ": " + saved.subject())
                        .formatted(Formatting.GOLD));
        player.sendMessage(notification);
    }

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
    public static OrmLiteProjectRepository getProjectRepository() { return projectRepository; }
    public static OrmLiteMemberRepository getMemberRepository() { return memberRepository; }
    public static OrmLiteTicketRepository getTicketRepository() { return ticketRepository; }
    public static OrmLiteMailRepository getMailRepository() { return mailRepository; }
    public static OrmLiteTicketRelationshipRepository getTicketRelationshipRepository() { return ticketRelationshipRepository; }
    public static AddCommentUseCase getAddCommentUseCase() { return addCommentUseCase; }
    public static DeleteCommentUseCase getDeleteCommentUseCase() { return deleteCommentUseCase; }
    public static OrmLiteCommentRepository getCommentRepository() { return commentRepository; }
    public static OrmLitePlayerSettingsRepository getPlayerSettingsRepository() { return playerSettingsRepository; }
    public static OrmLiteTimeLogRepository getTimeLogRepository() { return timeLogRepository; }
    public static LogTimeUseCase getLogTimeUseCase() { return logTimeUseCase; }
    public static DeleteTimeLogUseCase getDeleteTimeLogUseCase() { return deleteTimeLogUseCase; }
    public static DatabaseManager getDatabaseManager() { return databaseManager; }
}
