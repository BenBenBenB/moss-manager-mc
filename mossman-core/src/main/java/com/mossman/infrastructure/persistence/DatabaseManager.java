package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mossman.infrastructure.persistence.models.CommentDb;
import com.mossman.infrastructure.persistence.models.PlayerSettingsDb;
import com.mossman.infrastructure.persistence.models.MailDb;
import com.mossman.infrastructure.persistence.models.MemberDb;
import com.mossman.infrastructure.persistence.models.ProjectDb;
import com.mossman.infrastructure.persistence.models.TicketDb;
import com.mossman.infrastructure.persistence.models.TicketRelationshipDb;
import com.mossman.infrastructure.persistence.models.TimeLogDb;

import java.sql.SQLException;

public class DatabaseManager {
    private final ConnectionSource connectionSource;
    private final Dao<ProjectDb, Long> projectDao;
    private final Dao<TicketDb, Long> ticketDao;
    private final Dao<MemberDb, Long> memberDao;
    private final Dao<MailDb, Long> mailDao;
    private final Dao<TicketRelationshipDb, Long> ticketRelationshipDao;
    private final Dao<CommentDb, Long> commentDao;
    private final Dao<PlayerSettingsDb, String> playerSettingsDao;
    private final Dao<TimeLogDb, Long> timeLogDao;

    public DatabaseManager(String databaseUrl) throws SQLException {
        this.connectionSource = new JdbcConnectionSource(databaseUrl);
        createTables();
        this.projectDao = DaoManager.createDao(connectionSource, ProjectDb.class);
        this.ticketDao = DaoManager.createDao(connectionSource, TicketDb.class);
        this.memberDao = DaoManager.createDao(connectionSource, MemberDb.class);
        this.mailDao = DaoManager.createDao(connectionSource, MailDb.class);
        this.ticketRelationshipDao = DaoManager.createDao(connectionSource, TicketRelationshipDb.class);
        this.commentDao = DaoManager.createDao(connectionSource, CommentDb.class);
        this.playerSettingsDao = DaoManager.createDao(connectionSource, PlayerSettingsDb.class);
        this.timeLogDao = DaoManager.createDao(connectionSource, TimeLogDb.class);
    }

    private void createTables() throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, ProjectDb.class);
        TableUtils.createTableIfNotExists(connectionSource, TicketDb.class);
        TableUtils.createTableIfNotExists(connectionSource, MemberDb.class);
        TableUtils.createTableIfNotExists(connectionSource, MailDb.class);
        TableUtils.createTableIfNotExists(connectionSource, TicketRelationshipDb.class);
        TableUtils.createTableIfNotExists(connectionSource, CommentDb.class);
        TableUtils.createTableIfNotExists(connectionSource, PlayerSettingsDb.class);
        TableUtils.createTableIfNotExists(connectionSource, TimeLogDb.class);
    }

    /** Drops and recreates all tables — for admin use only. */
    public void dropAndRecreateAllTables() throws SQLException {
        TableUtils.dropTable(connectionSource, TimeLogDb.class, true);
        TableUtils.dropTable(connectionSource, PlayerSettingsDb.class, true);
        TableUtils.dropTable(connectionSource, CommentDb.class, true);
        TableUtils.dropTable(connectionSource, TicketRelationshipDb.class, true);
        TableUtils.dropTable(connectionSource, MailDb.class, true);
        TableUtils.dropTable(connectionSource, MemberDb.class, true);
        TableUtils.dropTable(connectionSource, TicketDb.class, true);
        TableUtils.dropTable(connectionSource, ProjectDb.class, true);
        createTables();
    }

    public Dao<ProjectDb, Long> getProjectDao() { return projectDao; }
    public Dao<TicketDb, Long> getTicketDao() { return ticketDao; }
    public Dao<MemberDb, Long> getMemberDao() { return memberDao; }
    public Dao<MailDb, Long> getMailDao() { return mailDao; }
    public Dao<TicketRelationshipDb, Long> getTicketRelationshipDao() { return ticketRelationshipDao; }
    public Dao<CommentDb, Long> getCommentDao() { return commentDao; }
    public Dao<PlayerSettingsDb, String> getPlayerSettingsDao() { return playerSettingsDao; }
    public Dao<TimeLogDb, Long> getTimeLogDao() { return timeLogDao; }
    public ConnectionSource getConnectionSource() { return connectionSource; }

    public void close() throws Exception {
        connectionSource.close();
    }
}
