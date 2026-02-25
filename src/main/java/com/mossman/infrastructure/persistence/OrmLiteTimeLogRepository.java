package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.TimeLog;
import com.mossman.domain.repositories.TimeLogRepository;
import com.mossman.infrastructure.persistence.models.TimeLogDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteTimeLogRepository implements TimeLogRepository {
    private final Dao<TimeLogDb, Long> timeLogDao;

    public OrmLiteTimeLogRepository(Dao<TimeLogDb, Long> timeLogDao) {
        this.timeLogDao = timeLogDao;
    }

    @Override
    public TimeLog save(TimeLog log) {
        try {
            TimeLogDb dbModel = new TimeLogDb(log);
            timeLogDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save time log", e);
        }
    }

    @Override
    public Optional<TimeLog> findById(long id) {
        try {
            TimeLogDb dbModel = timeLogDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(TimeLogDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find time log by id", e);
        }
    }

    @Override
    public List<TimeLog> findByTicketId(long ticketId, int offset, int limit) {
        try {
            return timeLogDao.queryBuilder()
                    .orderBy("loggedAt", false)
                    .offset((long) offset)
                    .limit((long) limit)
                    .where().eq("ticketId", ticketId)
                    .query().stream()
                    .map(TimeLogDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find time logs by ticketId", e);
        }
    }

    @Override
    public long countByTicketId(long ticketId) {
        try {
            return timeLogDao.queryBuilder()
                    .where().eq("ticketId", ticketId)
                    .countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count time logs by ticketId", e);
        }
    }

    @Override
    public long sumMinutesByTicketId(long ticketId) {
        try {
            com.j256.ormlite.dao.GenericRawResults<String[]> raw = timeLogDao.queryRaw(
                    "SELECT SUM(minutes) FROM time_logs WHERE ticketId = ?",
                    String.valueOf(ticketId));
            String[] first = raw.getFirstResult();
            if (first == null || first[0] == null || first[0].isBlank()) return 0;
            try { return Long.parseLong(first[0].trim()); }
            catch (NumberFormatException e) { return 0; }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum minutes by ticketId", e);
        }
    }

    @Override
    public void delete(long id) {
        try {
            timeLogDao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete time log", e);
        }
    }
}
