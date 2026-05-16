package com.mossman.core.usecase;

public sealed class UseCaseException extends RuntimeException
        permits PermissionDeniedException, NotFoundException, ValidationException {

    protected UseCaseException(String message) {
        super(message);
    }
}
