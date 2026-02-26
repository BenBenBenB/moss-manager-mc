package com.mossman.domain.entities;

public record RelationshipType(String key, String displayName,
        String sourceToTargetDescription, String targetToSourceDescription,
        String textColor) {
}
