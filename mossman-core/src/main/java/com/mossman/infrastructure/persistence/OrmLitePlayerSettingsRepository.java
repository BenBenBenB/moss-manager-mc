package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.PlayerSettings;
import com.mossman.domain.repositories.PlayerSettingsRepository;
import com.mossman.infrastructure.persistence.models.PlayerSettingsDb;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class OrmLitePlayerSettingsRepository implements PlayerSettingsRepository {
    private final Dao<PlayerSettingsDb, String> dao;

    public OrmLitePlayerSettingsRepository(Dao<PlayerSettingsDb, String> dao) {
        this.dao = dao;
    }

    @Override
    public PlayerSettings save(PlayerSettings settings) {
        try {
            PlayerSettingsDb dbModel = new PlayerSettingsDb(settings);
            dao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save player settings", e);
        }
    }

    @Override
    public Optional<PlayerSettings> findByPlayerId(UUID playerId) {
        try {
            PlayerSettingsDb dbModel = dao.queryForId(playerId.toString());
            return Optional.ofNullable(dbModel).map(PlayerSettingsDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find player settings", e);
        }
    }
}
