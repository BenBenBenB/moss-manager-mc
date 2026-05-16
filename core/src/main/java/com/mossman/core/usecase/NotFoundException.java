package com.mossman.core.usecase;

public final class NotFoundException extends UseCaseException {

    public enum Kind { PROJECT, ROLE, STATUS, TYPE, TICKET, MEMBER }

    private final Kind kind;
    private final String id;

    public NotFoundException(Kind kind, String id) {
        super(kind.name().toLowerCase() + " not found: " + id);
        this.kind = kind;
        this.id = id;
    }

    public Kind kind() { return kind; }
    public String id() { return id; }
}
