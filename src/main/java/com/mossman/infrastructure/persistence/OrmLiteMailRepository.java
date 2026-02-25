package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.repositories.MailRepository;
import com.mossman.infrastructure.persistence.models.MailDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrmLiteMailRepository implements MailRepository {
    private final Dao<MailDb, Long> mailDao;

    public OrmLiteMailRepository(Dao<MailDb, Long> mailDao) {
        this.mailDao = mailDao;
    }

    @Override
    public MailMessage save(MailMessage message) {
        try {
            MailDb dbModel = new MailDb(message);
            mailDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save mail message", e);
        }
    }

    @Override
    public Optional<MailMessage> findById(long id) {
        try {
            MailDb dbModel = mailDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(MailDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mail message by id", e);
        }
    }

    @Override
    public List<MailMessage> findByRecipientId(UUID recipientId, int offset, int limit) {
        try {
            return mailDao.queryBuilder()
                    .orderBy("sentAt", false)
                    .offset((long) offset)
                    .limit((long) limit)
                    .where().eq("recipientId", recipientId.toString())
                    .query().stream()
                    .map(MailDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mail messages by recipientId", e);
        }
    }

    @Override
    public long countByRecipientId(UUID recipientId) {
        try {
            return mailDao.queryBuilder()
                    .where().eq("recipientId", recipientId.toString())
                    .countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count mail messages", e);
        }
    }

    @Override
    public long countUnread(UUID recipientId) {
        try {
            return mailDao.queryBuilder()
                    .where().eq("recipientId", recipientId.toString())
                    .and().eq("isRead", false)
                    .countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count unread mail messages", e);
        }
    }
}
