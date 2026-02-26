package com.mossman;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
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

public class MossManMod implements ModInitializer {
    public static final String MOD_ID = "mossman";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MossMan Tracker...");

        ConfigManager.loadConfig();

        try {
            File dbFile = new File(FabricLoader.getInstance().getGameDir().toFile(), "mossman.db");
            String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
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

            LOGGER.info("Database and Use Cases initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("MossMan: failed to initialize database — commands and events will not work", e);
        }
    }
}
