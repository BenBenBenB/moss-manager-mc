package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.query.TicketFilter;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.infrastructure.persistence.models.TicketDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteTicketRepository implements TicketRepository {
    private final Dao<TicketDb, Long> ticketDao;

    public OrmLiteTicketRepository(Dao<TicketDb, Long> ticketDao) {
        this.ticketDao = ticketDao;
    }

    @Override
    public Ticket save(Ticket ticket) {
        try {
            TicketDb dbModel = new TicketDb(ticket);
            ticketDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ticket", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        try {
            TicketDb dbModel = ticketDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(TicketDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket by id", e);
        }
    }

    @Override
    public List<Ticket> findByProjectId(long projectId, int offset, int limit) {
        try {
            return ticketDao.queryBuilder()
                    .offset((long) offset)
                    .limit((long) limit)
                    .where().eq("projectId", projectId)
                    .query().stream()
                    .map(TicketDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tickets by projectId", e);
        }
    }

    @Override
    public List<Ticket> findByProjectId(long projectId, TicketFilter filter, int offset, int limit) {
        try {
            if (filter.labels().isPresent()) {
                // Label filter must be applied in memory; fetch all SQL matches first
                QueryBuilder<TicketDb, Long> qb = ticketDao.queryBuilder();
                Where<TicketDb, Long> where = qb.where().eq("projectId", projectId);
                applyFilter(where, filter);
                List<Ticket> all = qb.query().stream()
                        .map(TicketDb::toDomain)
                        .filter(t -> matchesAnyLabel(t, filter.labels().get()))
                        .collect(Collectors.toList());
                int from = Math.min(offset, all.size());
                int to = Math.min(offset + limit, all.size());
                return all.subList(from, to);
            }
            QueryBuilder<TicketDb, Long> qb = ticketDao.queryBuilder();
            qb.offset((long) offset).limit((long) limit);
            Where<TicketDb, Long> where = qb.where().eq("projectId", projectId);
            applyFilter(where, filter);
            return qb.query().stream()
                    .map(TicketDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find filtered tickets", e);
        }
    }

    @Override
    public long countByProjectId(long projectId) {
        try {
            return ticketDao.queryBuilder().where().eq("projectId", projectId).countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count tickets", e);
        }
    }

    @Override
    public long countByProjectId(long projectId, TicketFilter filter) {
        try {
            if (filter.labels().isPresent()) {
                QueryBuilder<TicketDb, Long> qb = ticketDao.queryBuilder();
                Where<TicketDb, Long> where = qb.where().eq("projectId", projectId);
                applyFilter(where, filter);
                return qb.query().stream()
                        .map(TicketDb::toDomain)
                        .filter(t -> matchesAnyLabel(t, filter.labels().get()))
                        .count();
            }
            QueryBuilder<TicketDb, Long> qb = ticketDao.queryBuilder();
            Where<TicketDb, Long> where = qb.where().eq("projectId", projectId);
            applyFilter(where, filter);
            return qb.countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count filtered tickets", e);
        }
    }

    /** Returns true if the ticket has at least one label matching any entry in filterLabels (case-insensitive). */
    private static boolean matchesAnyLabel(Ticket ticket, List<String> filterLabels) {
        for (String filterLabel : filterLabels) {
            for (String ticketLabel : ticket.getLabels()) {
                if (ticketLabel.equalsIgnoreCase(filterLabel)) return true;
            }
        }
        return false;
    }

    private void applyFilter(Where<TicketDb, Long> where, TicketFilter filter) throws SQLException {
        if (filter.status().isPresent()) {
            where.and().eq("status", filter.status().get().toUpperCase());
        }
        if (filter.type().isPresent()) {
            where.and().eq("type", filter.type().get());
        }
        if (filter.priority().isPresent()) {
            where.and().eq("priority", filter.priority().get().toUpperCase());
        }
        if (filter.title().isPresent()) {
            where.and().like("title", "%" + filter.title().get() + "%");
        }
    }

    @Override
    public void delete(long id) {
        try {
            ticketDao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete ticket", e);
        }
    }

    @Override
    public int getNextTicketNumber(long projectId) {
        try {
            GenericRawResults<String[]> results = ticketDao.queryRaw(
                    "SELECT MAX(ticketNumber) FROM tickets WHERE projectId = ?",
                    Long.toString(projectId));
            String[] row = results.getFirstResult();
            if (row == null || row[0] == null) {
                return 1;
            }
            return Integer.parseInt(row[0]) + 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get next ticket number", e);
        }
    }
}
