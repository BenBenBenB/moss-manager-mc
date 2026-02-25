package com.mossman.domain.repositories;

import com.mossman.domain.entities.PlayerSettings;

import java.util.Optional;
import java.util.UUID;

public interface PlayerSettingsRepository {
    PlayerSettings save(PlayerSettings settings);
    Optional<PlayerSettings> findByPlayerId(UUID playerId);
}
