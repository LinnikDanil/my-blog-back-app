package ru.practicum.blog.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.domain.exception.CommentDbException;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.repository.CommentRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcCommentRepositoryImpl implements CommentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Comment> findCommentsByPostId(long postId) {
        return jdbcTemplate.query(
                "SELECT id, text, post_id FROM comment WHERE post_id = :postId ORDER BY created_at DESC, id DESC",
                Map.of("postId", postId),
                (resultSet, rowNum) -> Comment.builder()
                        .id(resultSet.getLong("id"))
                        .text(resultSet.getString("text"))
                        .postId(resultSet.getLong("post_id"))
                        .build()
        );
    }

    @Override
    public Optional<Comment> findCommentById(long postId, long commentId) {
        return jdbcTemplate.query(
                "SELECT id, text, post_id FROM comment WHERE id = :commentId AND post_id = :postId",
                Map.of("commentId", commentId, "postId", postId),
                (resultSet, rowNum) -> Comment.builder()
                        .id(resultSet.getLong("id"))
                        .text(resultSet.getString("text"))
                        .postId(resultSet.getLong("post_id"))
                        .build()
        ).stream().findFirst();
    }

    @Override
    public Comment createComment(long postId, String text) {
        return jdbcTemplate.query(
                "INSERT INTO comment (text, post_id) VALUES(:text, :postId) RETURNING id, text, post_id",
                Map.of("text", text, "postId", postId),
                (resultSet, rowNum) -> Comment.builder()
                        .id(resultSet.getLong("id"))
                        .text(resultSet.getString("text"))
                        .postId(resultSet.getLong("post_id"))
                        .build()
        ).stream().findFirst().orElseThrow(() -> new CommentDbException("Failed to create comment."));
    }

    @Override
    public boolean existsById(long postId, long commentId) {
        Boolean commentExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM comment WHERE id = :commentId AND post_id = :postId)",
                Map.of("commentId", commentId, "postId", postId),
                Boolean.class
        );
        return Boolean.TRUE.equals(commentExists);
    }

    @Override
    public Comment updateComment(long postId, long commentId, String text) {
        String sql = """
                UPDATE comment
                SET text = :text,
                updated_at = CURRENT_TIMESTAMP
                WHERE id = :commentId
                    AND post_id = :postId
                RETURNING id, text, post_id
                """;

        return jdbcTemplate.query(
                sql,
                Map.of("text", text, "commentId", commentId, "postId", postId),
                (resultSet, rowNum) -> Comment.builder()
                        .id(resultSet.getLong("id"))
                        .text(resultSet.getString("text"))
                        .postId(resultSet.getLong("post_id"))
                        .build()
        ).stream().findFirst().orElseThrow(() -> new CommentDbException("Failed to update comment."));
    }

    @Override
    public void deleteComment(long postId, long commentId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM comment WHERE post_id = :postId AND id = :commentId",
                Map.of("postId", postId, "commentId", commentId)
        );
        if (deleted == 0) {
            throw new CommentNotFoundException("Comment with id = %d for post with id = %d does not exist."
                    .formatted(commentId, postId));
        }
    }
}
