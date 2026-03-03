package com.mossman;

import com.mossman.adapters.commands.AdminCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
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

import java.nio.file.Files;
import java.nio.file.Path;

public class MossManMod implements ModInitializer {
    public static final String MOD_ID = "mossman";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MossMan Tracker...");

        ConfigManager.loadConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AdminCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(this::initializeForWorld);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MossManApi.shutdown());
    }

    private void initializeForWorld(MinecraftServer server) {
        try {
            // LEVEL_DAT parent gives the world save root without the trailing "." from WorldSavePath.ROOT
            Path dataDir = server.getSavePath(WorldSavePath.LEVEL_DAT)
                    .getParent()
                    .toAbsolutePath()
                    .normalize()
                    .resolve("data");
            Files.createDirectories(dataDir);
            Path dbPath = dataDir.resolve("mossman.db");
            String dbUrl = "jdbc:sqlite:" + dbPath;
            DatabaseManager databaseManager = new DatabaseManager(dbUrl);
            SimpleEventBus eventBus = new SimpleEventBus();

            OrmLiteMemberRepository memberRepository = new OrmLiteMemberRepository(databaseManager.getMemberDao());
            OrmLiteProjectRepository projectRepository = new OrmLiteProjectRepository(databaseManager.getProjectDao(), memberRepository);
            OrmLiteTicketRepository ticketRepository = new OrmLiteTicketRepository(databaseManager.getTicketDao());
            OrmLiteMailRepository mailRepository = new OrmLiteMailRepository(databaseManager.getMailDao());
            OrmLiteTicketRelationshipRepository ticketRelationshipRepository = new OrmLiteTicketRelationshipRepository(databaseManager.getTicketRelationshipDao());
            OrmLiteCommentRepository commentRepository = new OrmLiteCommentRepository(databaseManager.getCommentDao());
            OrmLitePlayerSettingsRepository playerSettingsRepository = new OrmLitePlayerSettingsRepository(databaseManager.getPlayerSettingsDao());
            OrmLiteTimeLogRepository timeLogRepository = new OrmLiteTimeLogRepository(databaseManager.getTimeLogDao());

            CreateProjectUseCase createProjectUseCase = new CreateProjectUseCase(projectRepository, eventBus);
            CreateTicketUseCase createTicketUseCase = new CreateTicketUseCase(ticketRepository, projectRepository, eventBus);
            UpdateTicketUseCase updateTicketUseCase = new UpdateTicketUseCase(ticketRepository, projectRepository, eventBus);
            UpdateProjectUseCase updateProjectUseCase = new UpdateProjectUseCase(projectRepository, eventBus);
            AddMemberUseCase addMemberUseCase = new AddMemberUseCase(projectRepository);
            UpdateMemberUseCase updateMemberUseCase = new UpdateMemberUseCase(projectRepository);
            RemoveMemberUseCase removeMemberUseCase = new RemoveMemberUseCase(projectRepository);
            TransferOwnershipUseCase transferOwnershipUseCase = new TransferOwnershipUseCase(projectRepository);
            UpdateProjectStatusesUseCase updateProjectStatusesUseCase = new UpdateProjectStatusesUseCase(projectRepository);
            UpdateProjectTicketTypesUseCase updateProjectTicketTypesUseCase = new UpdateProjectTicketTypesUseCase(projectRepository);
            UpdateProjectRelationshipTypesUseCase updateProjectRelationshipTypesUseCase = new UpdateProjectRelationshipTypesUseCase(projectRepository);
            AssignTicketUseCase assignTicketUseCase = new AssignTicketUseCase(ticketRepository, projectRepository, eventBus);
            UnassignTicketUseCase unassignTicketUseCase = new UnassignTicketUseCase(ticketRepository, projectRepository, eventBus);
            ObserveTicketUseCase observeTicketUseCase = new ObserveTicketUseCase(ticketRepository, projectRepository);
            UnobserveTicketUseCase unobserveTicketUseCase = new UnobserveTicketUseCase(ticketRepository);
            SendMailUseCase sendMailUseCase = new SendMailUseCase(mailRepository);
            MarkMailReadUseCase markMailReadUseCase = new MarkMailReadUseCase(mailRepository);
            LinkTicketsUseCase linkTicketsUseCase = new LinkTicketsUseCase(ticketRelationshipRepository, ticketRepository, projectRepository);
            UnlinkTicketsUseCase unlinkTicketsUseCase = new UnlinkTicketsUseCase(ticketRelationshipRepository, ticketRepository, projectRepository);
            AddCommentUseCase addCommentUseCase = new AddCommentUseCase(commentRepository, ticketRepository, projectRepository, eventBus);
            DeleteCommentUseCase deleteCommentUseCase = new DeleteCommentUseCase(commentRepository, ticketRepository, projectRepository);
            LogTimeUseCase logTimeUseCase = new LogTimeUseCase(timeLogRepository, ticketRepository, projectRepository);
            DeleteTimeLogUseCase deleteTimeLogUseCase = new DeleteTimeLogUseCase(timeLogRepository, ticketRepository, projectRepository);

            MossManApi.initialize(
                    eventBus, databaseManager,
                    projectRepository, ticketRepository, memberRepository, mailRepository,
                    ticketRelationshipRepository, commentRepository, playerSettingsRepository, timeLogRepository,
                    createProjectUseCase, createTicketUseCase, updateTicketUseCase, updateProjectUseCase,
                    addMemberUseCase, updateMemberUseCase, removeMemberUseCase, transferOwnershipUseCase,
                    updateProjectStatusesUseCase, updateProjectTicketTypesUseCase, updateProjectRelationshipTypesUseCase,
                    assignTicketUseCase, unassignTicketUseCase, observeTicketUseCase, unobserveTicketUseCase,
                    sendMailUseCase, markMailReadUseCase, linkTicketsUseCase, unlinkTicketsUseCase,
                    addCommentUseCase, deleteCommentUseCase, logTimeUseCase, deleteTimeLogUseCase);

            LOGGER.info("MossMan: database initialized at {}", dbPath);
        } catch (Exception e) {
            LOGGER.error("MossMan: failed to initialize database for world — commands will not work", e);
        }
    }
}
