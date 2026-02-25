package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.Member;
import com.mossman.domain.repositories.MemberRepository;
import com.mossman.infrastructure.persistence.models.MemberDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrmLiteMemberRepository implements MemberRepository {
    private final Dao<MemberDb, Long> memberDao;

    public OrmLiteMemberRepository(Dao<MemberDb, Long> memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public Member save(long projectId, Member member) {
        try {
            MemberDb db = new MemberDb(projectId, member);
            memberDao.createOrUpdate(db);
            // Return the domain object (id may be generated but not needed)
            return member;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save member", e);
        }
    }

    @Override
    public List<Member> findByProjectId(long projectId, int offset, int limit) {
        try {
            List<MemberDb> dbList = memberDao.queryBuilder()
                    .offset((long) offset)
                    .limit((long) limit)
                    .where().eq("project_id", projectId)
                    .query();
            return dbList.stream().map(db -> db.toDomain(projectId)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find members for project", e);
        }
    }

    @Override
    public long countByProjectId(long projectId) {
        try {
            return memberDao.queryBuilder().where().eq("project_id", projectId).countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count members", e);
        }
    }

    @Override
    public Optional<Member> findByProjectIdAndUuid(long projectId, UUID uuid) {
        try {
            List<MemberDb> dbList = memberDao.queryBuilder().where().eq("project_id", projectId).and().eq("uuid", uuid).query();
            if (dbList.isEmpty()) return Optional.empty();
            return Optional.of(dbList.get(0).toDomain(projectId));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find member", e);
        }
    }

    @Override
    public void deleteByProjectIdAndUuid(long projectId, UUID uuid) {
        try {
            var deleteBuilder = memberDao.deleteBuilder();
            deleteBuilder.where().eq("project_id", projectId).and().eq("uuid", uuid);
            memberDao.delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete member", e);
        }
    }
}
