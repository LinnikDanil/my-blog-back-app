package ru.practicum.blog.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.practicum.blog.config.TestDataSourceConfiguration;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.repository.CommentRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {TestDataSourceConfiguration.class, JdbcCommentRepositoryImpl.class})
@TestPropertySource(locations = "classpath:test-application.properties")
@DisplayName("JdbcCommentRepositoryIT")
class JdbcCommentRepositoryIT {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM comment", Map.of());
        jdbcTemplate.update("DELETE FROM post", Map.of());

        jdbcTemplate.update(
                "INSERT INTO post (id, title, text) VALUES(:id, :title, :text)",
                new MapSqlParameterSource()
                        .addValue("id", 1L)
                        .addValue("title", "Post")
                        .addValue("text", "Body")
        );

        jdbcTemplate.update(
                "INSERT INTO comment (id, text, post_id) VALUES(:id, :text, :postId)",
                new MapSqlParameterSource()
                        .addValue("id", 5L)
                        .addValue("text", "First")
                        .addValue("postId", 1L)
        );

        jdbcTemplate.update(
                "INSERT INTO comment (id, text, post_id) VALUES(:id, :text, :postId)",
                new MapSqlParameterSource()
                        .addValue("id", 6L)
                        .addValue("text", "Second")
                        .addValue("postId", 1L)
        );
    }

    @Nested
    @DisplayName("findCommentsByPostId")
    class FindCommentsByPostId {

        @Test
        @DisplayName("should return comments ordered by creation")
        void shouldReturnCommentsOrderedByCreation() {
            List<Comment> comments = commentRepository.findCommentsByPostId(1L);

            assertEquals(2, comments.size());
            assertEquals(6L, comments.getFirst().getId());
            assertEquals(5L, comments.get(1).getId());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should insert comment and return entity")
        void shouldInsertCommentAndReturnEntity() {
            Comment comment = commentRepository.createComment(1L, "New comment");

            assertTrue(comment.getId() > 0);
            assertEquals("New comment", comment.getText());
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("should update comment text")
        void shouldUpdateCommentText() {
            Comment updated = commentRepository.updateComment(1L, 5L, "Updated");

            assertEquals("Updated", updated.getText());
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should verify comment existence")
        void shouldVerifyCommentExistence() {
            assertTrue(commentRepository.existsById(1L, 5L));
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment from database")
        void shouldDeleteCommentFromDatabase() {
            commentRepository.deleteComment(1L, 5L);

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE id = :id",
                    Map.of("id", 5L),
                    Long.class
            );
            assertEquals(0L, count);
        }
    }
}
