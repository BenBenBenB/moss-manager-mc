package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;

import java.util.UUID;

@DatabaseTable(tableName = "members")
public class MemberDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false, columnName = "project_id")
    private long projectId;

    @DatabaseField(canBeNull = false)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    private String username;

    @DatabaseField(canBeNull = true)
    private String title;

    @DatabaseField(canBeNull = false)
    private Permission permission;

    // ORMLite needs a no-arg constructor
    public MemberDb() {}

    public MemberDb(long projectId, Member member) {
        this.id = member.id();
        this.projectId = projectId;
        this.uuid = member.uuid();
        this.username = member.username();
        this.title = member.title();
        this.permission = member.permission();
    }

    public static MemberDb fromDomain(long projectId, Member member) {
        return new MemberDb(projectId, member);
    }

    public Member toDomain(long projectId) {
        return new Member(id, uuid, username, title, permission);
    }
}
