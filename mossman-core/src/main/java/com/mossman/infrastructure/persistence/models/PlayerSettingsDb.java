package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.PlayerSettings;

import java.util.UUID;

@DatabaseTable(tableName = "player_settings")
public class PlayerSettingsDb {
    /** UUID string used as the primary key — one row per player. */
    @DatabaseField(id = true, canBeNull = false)
    private String playerId;

    @DatabaseField(canBeNull = false)
    private String timezone;

    public PlayerSettingsDb() {}

    public PlayerSettingsDb(PlayerSettings settings) {
        this.playerId = settings.playerId().toString();
        this.timezone = settings.timezone();
    }

    public PlayerSettings toDomain() {
        return new PlayerSettings(UUID.fromString(playerId), timezone);
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
