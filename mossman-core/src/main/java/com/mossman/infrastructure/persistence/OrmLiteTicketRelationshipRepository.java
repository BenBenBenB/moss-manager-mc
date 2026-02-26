package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.TicketRelationship;
import com.mossman.domain.repositories.TicketRelationshipRepository;
import com.mossman.infrastructure.persistence.models.TicketRelationshipDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteTicketRelationshipRepository implements TicketRelationshipRepository {
    private final Dao<TicketRelationshipDb, Long> dao;

    public OrmLiteTicketRelationshipRepository(Dao<TicketRelationshipDb, Long> dao) {
        this.dao = dao;
    }

    @Override
    public TicketRelationship save(TicketRelationship relationship) {
        try {
            TicketRelationshipDb dbModel = new TicketRelationshipDb(relationship);
            dao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ticket relationship", e);
        }
    }

    @Override
    public Optional<TicketRelationship> findById(long id) {
        try {
            TicketRelationshipDb dbModel = dao.queryForId(id);
            return Optional.ofNullable(dbModel).map(TicketRelationshipDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket relationship by id", e);
        }
    }

    @Override
    public List<TicketRelationship> findByTicketId(long ticketId) {
        try {
            return dao.queryBuilder()
                    .where().eq("sourceTicketId", ticketId)
                    .or().eq("targetTicketId", ticketId)
                    .query().stream()
                    .map(TicketRelationshipDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket relationships by ticketId", e);
        }
    }

    @Override
    public Optional<TicketRelationship> findBySourceAndTargetAndType(long sourceId, long targetId, String type) {
        try {
            TicketRelationshipDb result = dao.queryBuilder()
                    .where().eq("sourceTicketId", sourceId)
                    .and().eq("targetTicketId", targetId)
                    .and().eq("type", type)
                    .queryForFirst();
            return Optional.ofNullable(result).map(TicketRelationshipDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket relationship by source/target/type", e);
        }
    }

    @Override
    public void delete(long id) {
        try {
            dao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete ticket relationship", e);
        }
    }
}
