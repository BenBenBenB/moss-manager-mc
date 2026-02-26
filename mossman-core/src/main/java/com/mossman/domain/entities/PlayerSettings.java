package com.mossman.domain.entities;

import java.util.UUID;

public record PlayerSettings(UUID playerId, String timezone) {}
