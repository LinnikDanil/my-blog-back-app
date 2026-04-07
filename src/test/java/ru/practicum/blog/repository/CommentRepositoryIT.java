package ru.practicum.blog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.practicum.blog.config.TestContainersConfig;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.repository.impl.JdbcCommentRepositoryImpl;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({TestContainersConfig.class, JdbcCommentRepositoryImpl.class})
@DisplayName("JdbcCommentRepository")
@DataJdbcTest
class CommentRepositoryIT {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Nested
    @DisplayName("findCommentsByPostId")
    class FindCommentsByPostId {

        @Test
        @DisplayName("should return comments ordered by creation date")
        void shouldReturnCommentsOrderedByCreationDate() {
            long postId = insertPost("Title", "Text");
            long firstId = insertComment(postId, "First comment");
            long secondId = insertComment(postId, "Second comment");

            List<Comment> comments = commentRepository.findCommentsByPostId(postId);

            assertThat(comments).hasSize(2);
            assertThat(comments.getFirst().getId()).isEqualTo(secondId);
            assertThat(comments.getFirst().getText()).isEqualTo("Second comment");
            assertThat(comments.get(1).getId()).isEqualTo(firstId);
            assertThat(comments.get(1).getText()).isEqualTo("First comment");
        }

        @Test
        @DisplayName("should return empty list when no comments")
        void shouldReturnEmptyListWhenNoComments() {
            long postId = insertPost("Title", "Text");

            List<Comment> comments = commentRepository.findCommentsByPostId(postId);

            assertThat(comments).isEmpty();
        }
    }

    @Nested
    @DisplayName("findCommentById")
    class FindCommentById {

        @Test
        @DisplayName("should return comment when exists")
        void shouldReturnCommentWhenExists() {
            long postId = insertPost("Title", "Text");
            long commentId = insertComment(postId, "Sample comment");

            Comment comment = commentRepository.findCommentById(postId, commentId).orElseThrow();

            assertThat(comment.getId()).isEqualTo(commentId);
            assertThat(comment.getText()).isEqualTo("Sample comment");
            assertThat(comment.getPostId()).isEqualTo(postId);
        }

        @Test
        @DisplayName("should return empty optional when comment missing")
        void shouldReturnEmptyOptionalWhenCommentMissing() {
            long postId = insertPost("Title", "Text");

            assertThat(commentRepository.findCommentById(postId, 9999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should create comment and return entity")
        void shouldCreateCommentAndReturnEntity() {
            long postId = insertPost("Title", "Text");

            Comment comment = commentRepository.createComment(postId, "New comment");

            assertThat(comment.getId()).isNotNull();
            assertThat(comment.getText()).isEqualTo("New comment");
            assertThat(comment.getPostId()).isEqualTo(postId);

            String persisted = namedJdbcTemplate.queryForObject(
                    "SELECT text FROM comment WHERE id = :commentId",
                    Map.of("commentId", comment.getId()),
                    String.class
            );
            assertThat(persisted).isEqualTo("New comment");
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true for existing comment")
        void shouldReturnTrueForExistingComment() {
            long postId = insertPost("Title", "Text");
            long commentId = insertComment(postId, "Existing comment");

            assertThat(commentRepository.existsById(postId, commentId)).isTrue();
        }

        @Test
        @DisplayName("should return false for missing comment")
        void shouldReturnFalseForMissingComment() {
            long postId = insertPost("Title", "Text");

            assertThat(commentRepository.existsById(postId, 111L)).isFalse();
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("should update comment text")
        void shouldUpdateCommentText() {
            long postId = insertPost("Title", "Text");
            long commentId = insertComment(postId, "Old text");

            Comment updated = commentRepository.updateComment(postId, commentId, "Updated text");

            assertThat(updated.getId()).isEqualTo(commentId);
            assertThat(updated.getText()).isEqualTo("Updated text");

            String persisted = namedJdbcTemplate.queryForObject(
                    "SELECT text FROM comment WHERE id = :commentId",
                    Map.of("commentId", commentId),
                    String.class
            );
            assertThat(persisted).isEqualTo("Updated text");
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete existing comment")
        void shouldDeleteExistingComment() {
            long postId = insertPost("Title", "Text");
            long commentId = insertComment(postId, "To delete");

            commentRepository.deleteComment(postId, commentId);

            Integer count = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE id = :commentId",
                    Map.of("commentId", commentId),
                    Integer.class
            );
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should throw when comment missing")
        void shouldThrowWhenCommentMissing() {
            long postId = insertPost("Title", "Text");

            assertThatThrownBy(() -> commentRepository.deleteComment(postId, 999L))
                    .isInstanceOf(CommentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    private long insertPost(String title, String text) {
        return namedJdbcTemplate.queryForObject(
                "INSERT INTO post (title, text) VALUES (:title, :text) RETURNING id",
                Map.of("title", title, "text", text),
                Long.class
        );
    }

    private long insertComment(long postId, String text) {
        return namedJdbcTemplate.queryForObject(
                "INSERT INTO comment (text, post_id) VALUES (:text, :postId) RETURNING id",
                Map.of("text", text, "postId", postId),
                Long.class
        );
    }
}
