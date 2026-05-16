package com.mossman.core.usecase;

import com.mossman.core.permission.Permission;

import java.util.Optional;
import java.util.UUID;

public final class PermissionDeniedException extends UseCaseException {

    private final String projectId;
    private final UUID actor;
    private final Permission required;
    private final boolean ownerOnly;

    private PermissionDeniedException(String projectId, UUID actor, Permission required, boolean ownerOnly) {
        super(buildMessage(projectId, actor, required, ownerOnly));
        this.projectId = projectId;
        this.actor = actor;
        this.required = required;
        this.ownerOnly = ownerOnly;
    }

    public static PermissionDeniedException forPermission(String projectId, UUID actor, Permission required) {
        return new PermissionDeniedException(projectId, actor, required, false);
    }

    public static PermissionDeniedException forOwnerOnly(String projectId, UUID actor) {
        return new PermissionDeniedException(projectId, actor, null, true);
    }

    public String projectId() { return projectId; }
    public UUID actor() { return actor; }
    public Optional<Permission> requiredPermission() { return Optional.ofNullable(required); }
    public boolean isOwnerOnly() { return ownerOnly; }

    private static String buildMessage(String projectId, UUID actor, Permission required, boolean ownerOnly) {
        if (ownerOnly) {
            return "actor " + actor + " is not the owner of project " + projectId;
        }
        return "actor " + actor + " lacks " + required + " on project " + projectId;
    }
}
