package com.mossman.domain.entities;

public record Sprint(long id, long projectId, String name, long startTime, long endTime, SprintStatus status) {
}
