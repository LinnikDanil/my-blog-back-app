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
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.domain.model.Tag;
import ru.practicum.blog.repository.PostRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {TestDataSourceConfiguration.class, JdbcPostRepositoryImpl.class})
@TestPropertySource(locations = "classpath:test-application.properties")
@DisplayName("JdbcPostRepositoryIT")
class JdbcPostRepositoryIT {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private PostRepository postRepository;

    private long post1Id;
    private long post2Id;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM post_tag", Map.of());
        jdbcTemplate.update("DELETE FROM comment", Map.of());
        jdbcTemplate.update("DELETE FROM post", Map.of());
        jdbcTemplate.update("DELETE FROM tag", Map.of());

        post1Id = jdbcTemplate.queryForObject(
                "INSERT INTO post (title, text, likes_count, comments_count) " +
                        "VALUES (:title, :text, :likes, :comments) RETURNING id",
                new MapSqlParameterSource()
                        .addValue("title", "Spring Guide")
                        .addValue("text", "Detailed content")
                        .addValue("likes", 2)
                        .addValue("comments", 1),
                Long.class
        );

        post2Id = jdbcTemplate.queryForObject(
                "INSERT INTO post (title, text, likes_count, comments_count) " +
                        "VALUES (:title, :text, :likes, :comments) RETURNING id",
                new MapSqlParameterSource()
                        .addValue("title", "Java Basics")
                        .addValue("text", "Short text")
                        .addValue("likes", 0)
                        .addValue("comments", 0),
                Long.class
        );

        long springTagId = jdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES(:name) RETURNING id",
                Map.of("name", "spring"),
                Long.class
        );

        long javaTagId = jdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES(:name) RETURNING id",
                Map.of("name", "java"),
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO post_tag (post_id, tag_id) VALUES(:postId, :tagId)",
                Map.of("postId", post1Id, "tagId", springTagId)
        );
        jdbcTemplate.update(
                "INSERT INTO post_tag (post_id, tag_id) VALUES(:postId, :tagId)",
                Map.of("postId", post2Id, "tagId", javaTagId)
        );
    }

    @Nested
    @DisplayName("findPosts")
    class FindPosts {
        @Test
        @DisplayName("should filter posts by title and tags")
        void shouldFilterPostsByTitleAndTags() {
            List<Post> posts = postRepository.findPosts(Set.of("spring"), "guide", 10, 0L);

            assertEquals(1, posts.size());
            Post post = posts.getFirst();

            assertEquals(post1Id, post.getId());

            assertEquals("Spring Guide", post.getTitle());
            assertEquals(List.of("spring"),
                    post.getTags().stream().map(Tag::getName).toList());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {
        @Test
        @DisplayName("should persist post with tags")
        void shouldPersistPostWithTags() {
            Post post = postRepository.createPost("New", "Body", List.of("spring", "jdbc"));

            assertNotNull(post.getId());
            List<String> tags = post.getTags().stream().map(Tag::getName).toList();
            assertTrue(tags.containsAll(List.of("spring", "jdbc")));

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE title = :title",
                    Map.of("title", "New"),
                    Long.class
            );
            assertEquals(1L, count);
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {
        @Test
        @DisplayName("should update post and replace tags")
        void shouldUpdatePostAndReplaceTags() {
            Post post = postRepository.updatePost(post1Id, "Updated", "Content", List.of("java"));

            assertEquals("Updated", post.getTitle());
            assertEquals(List.of("java"), post.getTags().stream().map(Tag::getName).toList());

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_tag WHERE post_id = :postId",
                    Map.of("postId", post1Id),
                    Long.class
            );
            assertEquals(1L, count);
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {
        @Test
        @DisplayName("should remove post and relations")
        void shouldRemovePostAndRelations() {
            postRepository.deletePost(post2Id);

            Long posts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE id = :id",
                    Map.of("id", post2Id),
                    Long.class
            );
            Long tags = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_tag WHERE post_id = :id",
                    Map.of("id", post2Id),
                    Long.class
            );
            assertEquals(0L, posts);
            assertEquals(0L, tags);
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {
        @Test
        @DisplayName("should return updated likes value")
        void shouldReturnUpdatedLikesValue() {
            int likes = postRepository.incrementLikes(post1Id);
            assertEquals(3, likes);
        }
    }

    @Nested
    @DisplayName("imageOperations")
    class ImageOperations {
        @Test
        @DisplayName("should update and fetch image")
        void shouldUpdateAndFetchImage() {
            byte[] image = "image".getBytes();

            boolean updated = postRepository.updateImage(post1Id, image);
            byte[] stored = postRepository.getImage(post1Id);

            assertTrue(updated);
            assertEquals("image", new String(stored));
        }
    }
}
