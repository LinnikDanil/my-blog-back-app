package ru.practicum.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.practicum.blog.config.TestContainersConfig;
import ru.practicum.blog.web.dto.CommentRequestDto;
import ru.practicum.blog.web.dto.CommentResponseDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestContainersConfig.class)
@SpringBootTest
@DisplayName("CommentService integration")
class CommentIntegrationIT {

    @Autowired
    private CommentService commentService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        namedJdbcTemplate.update("TRUNCATE TABLE comment, post_tag, tag, post RESTART IDENTITY CASCADE", Collections.emptyMap());
    }

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("should return comments ordered by creation time")
        void shouldReturnCommentsOrderedByCreationTime() {
            long postId = insertPost("Title", "Text", 0, 0);
            long firstComment = insertComment(postId, "First");
            long secondComment = insertComment(postId, "Second");

            List<CommentResponseDto> comments = commentService.getComments(postId);

            assertThat(comments).hasSize(2);
            assertThat(comments.getFirst().id()).isEqualTo(secondComment);
            assertThat(comments.getFirst().text()).isEqualTo("Second");
            assertThat(comments.get(1).id()).isEqualTo(firstComment);
        }
    }

    @Nested
    @DisplayName("getComment")
    class GetComment {

        @Test
        @DisplayName("should return comment for post")
        void shouldReturnCommentForPost() {
            long postId = insertPost("Title", "Text", 0, 0);
            long commentId = insertComment(postId, "Sample");

            CommentResponseDto response = commentService.getComment(postId, commentId);

            assertThat(response.id()).isEqualTo(commentId);
            assertThat(response.postId()).isEqualTo(postId);
            assertThat(response.text()).isEqualTo("Sample");
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should create comment and increment post counter")
        void shouldCreateCommentAndIncrementPostCounter() {
            long postId = insertPost("Title", "Text", 0, 0);
            CommentRequestDto requestDto = new CommentRequestDto(null, "Nice post", postId);

            CommentResponseDto response = commentService.createComment(postId, requestDto);

            assertThat(response.id()).isNotNull();
            assertThat(response.text()).isEqualTo("Nice post");

            String persisted = namedJdbcTemplate.queryForObject(
                    "SELECT text FROM comment WHERE id = :id",
                    Map.of("id", response.id()),
                    String.class
            );
            assertThat(persisted).isEqualTo("Nice post");

            Integer commentsCount = namedJdbcTemplate.queryForObject(
                    "SELECT comments_count FROM post WHERE id = :id",
                    Map.of("id", postId),
                    Integer.class
            );
            assertThat(commentsCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("should update comment text")
        void shouldUpdateCommentText() {
            long postId = insertPost("Title", "Text", 0, 0);
            long commentId = insertComment(postId, "Old");
            CommentRequestDto requestDto = new CommentRequestDto(commentId, "Updated", postId);

            CommentResponseDto response = commentService.updateComment(postId, commentId, requestDto);

            assertThat(response.text()).isEqualTo("Updated");
            String persisted = namedJdbcTemplate.queryForObject(
                    "SELECT text FROM comment WHERE id = :id",
                    Map.of("id", commentId),
                    String.class
            );
            assertThat(persisted).isEqualTo("Updated");
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment and decrement counter")
        void shouldDeleteCommentAndDecrementCounter() {
            long postId = insertPost("Title", "Text", 0, 1);
            long commentId = insertComment(postId, "To delete");

            commentService.deleteComment(postId, commentId);

            Integer commentsCount = namedJdbcTemplate.queryForObject(
                    "SELECT comments_count FROM post WHERE id = :id",
                    Map.of("id", postId),
                    Integer.class
            );
            assertThat(commentsCount).isZero();

            Integer commentExists = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE id = :id",
                    Map.of("id", commentId),
                    Integer.class
            );
            assertThat(commentExists).isZero();
        }
    }

    private long insertPost(String title, String text, int likes, int comments) {
        return namedJdbcTemplate.queryForObject(
                "INSERT INTO post (title, text, likes_count, comments_count) VALUES (:title, :text, :likes, :comments) RETURNING id",
                Map.of("title", title, "text", text, "likes", likes, "comments", comments),
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
