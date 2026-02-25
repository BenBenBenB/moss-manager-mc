package com.mossman.domain.entities;

import java.util.UUID;

public record Member(long id, UUID uuid, String username, String title, Permission permission) {
}
