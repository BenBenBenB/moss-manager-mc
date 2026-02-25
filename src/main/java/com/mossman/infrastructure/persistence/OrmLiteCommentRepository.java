package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.Comment;
import com.mossman.domain.repositories.CommentRepository;
import com.mossman.infrastructure.persistence.models.CommentDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteCommentRepository implements CommentRepository {
    private final Dao<CommentDb, Long> commentDao;

    public OrmLiteCommentRepository(Dao<CommentDb, Long> commentDao) {
        this.commentDao = commentDao;
    }

    @Override
    public Comment save(Comment comment) {
        try {
            CommentDb dbModel = new CommentDb(comment);
            commentDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save comment", e);
        }
    }

    @Override
    public Optional<Comment> findById(long id) {
        try {
            CommentDb dbModel = commentDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(CommentDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find comment by id", e);
        }
    }

    @Override
    public List<Comment> findByTicketId(long ticketId) {
        try {
            return commentDao.queryBuilder()
                    .orderBy("createdAt", true)
                    .where().eq("ticketId", ticketId)
                    .query().stream()
                    .map(CommentDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find comments by ticketId", e);
        }
    }

    @Override
    public List<Comment> findByTicketId(long ticketId, int offset, int limit) {
        try {
            return commentDao.queryBuilder()
                    .orderBy("createdAt", true)
                    .offset((long) offset)
                    .limit((long) limit)
                    .where().eq("ticketId", ticketId)
                    .query().stream()
                    .map(CommentDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find comments by ticketId", e);
        }
    }

    @Override
    public long countByTicketId(long ticketId) {
        try {
            return commentDao.queryBuilder()
                    .where().eq("ticketId", ticketId)
                    .countOf();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count comments by ticketId", e);
        }
    }

    @Override
    public void delete(long id) {
        try {
            commentDao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete comment", e);
        }
    }
}
