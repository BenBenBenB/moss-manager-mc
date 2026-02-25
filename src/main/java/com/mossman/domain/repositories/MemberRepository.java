package com.mossman.domain.repositories;

import com.mossman.domain.entities.Member;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Member save(long projectId, Member member);
    List<Member> findByProjectId(long projectId, int offset, int limit);
    long countByProjectId(long projectId);
    Optional<Member> findByProjectIdAndUuid(long projectId, UUID uuid);
    void deleteByProjectIdAndUuid(long projectId, UUID uuid);
}
