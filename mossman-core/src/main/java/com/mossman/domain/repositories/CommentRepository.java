package com.mossman.domain.repositories;

import com.mossman.domain.entities.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(long id);
    List<Comment> findByTicketId(long ticketId);
    List<Comment> findByTicketId(long ticketId, int offset, int limit);
    long countByTicketId(long ticketId);
    void delete(long id);
}
