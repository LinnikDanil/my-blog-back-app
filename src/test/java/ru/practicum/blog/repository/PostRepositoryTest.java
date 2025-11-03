package ru.practicum.blog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.practicum.blog.config.TestContainersConfig;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.repository.impl.JdbcPostRepositoryImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({TestContainersConfig.class, JdbcPostRepositoryImpl.class})
@DisplayName("JdbcPostRepository")
@DataJdbcTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Nested
    @DisplayName("findPosts")
    class FindPosts {

        @Test
        @DisplayName("should return posts filtered by tag and title")
        void shouldReturnPostsFilteredByTagAndTitle() {
            long post1 = insertPost("Spring Boot Tips", "Content 1", 2, 3);
            long post2 = insertPost("Java Streams", "Content 2", 1, 0);
            long tagSpring = insertTag("spring");
            long tagJava = insertTag("java");
            long tagCloud = insertTag("cloud");
            linkPostTag(post1, tagSpring);
            linkPostTag(post1, tagJava);
            linkPostTag(post2, tagJava);
            linkPostTag(post2, tagCloud);

            List<Post> posts = postRepository.findPosts(Set.of("spring"), "boot", 10, 0);

            assertThat(posts).hasSize(1);
            Post result = posts.getFirst();
            assertThat(result.getId()).isEqualTo(post1);
            assertThat(result.getTitle()).isEqualTo("Spring Boot Tips");
            assertThat(result.getTags()).extracting("name").containsExactlyInAnyOrder("spring", "java");
        }

        @Test
        @DisplayName("should support pagination with offset")
        void shouldSupportPaginationWithOffset() {
            long first = insertPost("Alpha", "Text", 0, 0);
            long second = insertPost("Beta", "Text", 0, 0);

            List<Post> firstPage = postRepository.findPosts(Set.of(), "", 1, 0);
            List<Post> secondPage = postRepository.findPosts(Set.of(), "", 1, 1);

            assertThat(firstPage).hasSize(1);
            assertThat(secondPage).hasSize(1);
            assertThat(firstPage.getFirst().getId()).isEqualTo(second);
            assertThat(secondPage.getFirst().getId()).isEqualTo(first);
        }

        @Test
        @DisplayName("should return empty list when nothing matches")
        void shouldReturnEmptyListWhenNothingMatches() {
            insertPost("Spring Boot Tips", "Content 1", 2, 3);

            List<Post> posts = postRepository.findPosts(Set.of("java"), "unknown", 10, 0);

            assertThat(posts).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPostById")
    class FindPostById {

        @Test
        @DisplayName("should return post with tags")
        void shouldReturnPostWithTags() {
            long postId = insertPost("Post title", "Post text", 5, 4);
            long tagA = insertTag("spring");
            long tagB = insertTag("java");
            linkPostTag(postId, tagA);
            linkPostTag(postId, tagB);

            Post post = postRepository.findPostById(postId).orElseThrow();

            assertThat(post.getTitle()).isEqualTo("Post title");
            assertThat(post.getText()).isEqualTo("Post text");
            assertThat(post.getLikesCount()).isEqualTo(5);
            assertThat(post.getCommentsCount()).isEqualTo(4);
            assertThat(post.getTags()).extracting("name").containsExactlyInAnyOrder("spring", "java");
        }

        @Test
        @DisplayName("should return empty optional when post missing")
        void shouldReturnEmptyOptionalWhenPostMissing() {
            assertThat(postRepository.findPostById(9999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("countPosts")
    class CountPosts {

        @Test
        @DisplayName("should count posts with tags and title filter")
        void shouldCountPostsWithTagsAndTitleFilter() {
            long post1 = insertPost("Spring Boot Tips", "Content 1", 0, 0);
            long post2 = insertPost("Java for Beginners", "Content 2", 0, 0);
            long tagSpring = insertTag("spring");
            long tagJava = insertTag("java");
            linkPostTag(post1, tagSpring);
            linkPostTag(post1, tagJava);
            linkPostTag(post2, tagJava);

            long allPosts = postRepository.countPosts(Set.of(), "");
            long javaPosts = postRepository.countPosts(Set.of("java"), "");
            long springBootPosts = postRepository.countPosts(Set.of("spring"), "boot");

            assertThat(allPosts).isEqualTo(2);
            assertThat(javaPosts).isEqualTo(2);
            assertThat(springBootPosts).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should create post with tags")
        void shouldCreatePostWithTags() {
            Post post = postRepository.createPost("New Title", "Markdown text", List.of("spring", "java"));

            assertThat(post.getId()).isNotNull();
            assertThat(post.getTitle()).isEqualTo("New Title");
            assertThat(post.getText()).isEqualTo("Markdown text");
            assertThat(post.getTags()).extracting("name").containsExactlyInAnyOrder("spring", "java");

            Long tagCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tag WHERE name IN (:names)",
                    Map.of("names", List.of("spring", "java")),
                    Long.class
            );
            assertThat(tagCount).isEqualTo(2);
        }

        @Test
        @DisplayName("should create post without tags")
        void shouldCreatePostWithoutTags() {
            Post post = postRepository.createPost("No Tags", "Plain text", List.of());

            assertThat(post.getId()).isNotNull();
            assertThat(post.getTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should update post and tags")
        void shouldUpdatePostAndTags() {
            Post original = postRepository.createPost("Old title", "Old text", List.of("spring", "java"));

            Post updated = postRepository.updatePost(original.getId(), "New title", "New text", List.of("spring", "cloud"));

            assertThat(updated.getTitle()).isEqualTo("New title");
            assertThat(updated.getText()).isEqualTo("New text");
            assertThat(updated.getTags()).extracting("name").containsExactlyInAnyOrder("spring", "cloud");

            List<String> persistedTags = namedJdbcTemplate.queryForList(
                    "SELECT t.name FROM tag t JOIN post_tag pt ON t.id = pt.tag_id WHERE pt.post_id = :postId",
                    Map.of("postId", original.getId()),
                    String.class
            );
            assertThat(persistedTags).containsExactlyInAnyOrder("spring", "cloud");
        }

        @Test
        @DisplayName("should remove tags when updated list empty")
        void shouldRemoveTagsWhenUpdatedListEmpty() {
            Post original = postRepository.createPost("Old title", "Old text", List.of("spring"));

            Post updated = postRepository.updatePost(original.getId(), "Old title", "New text", List.of());

            assertThat(updated.getTags()).isEmpty();

            Long links = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_tag WHERE post_id = :postId",
                    Map.of("postId", original.getId()),
                    Long.class
            );
            assertThat(links).isZero();
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delete existing post")
        void shouldDeleteExistingPost() {
            Post created = postRepository.createPost("Title", "Text", List.of());

            postRepository.deletePost(created.getId());

            assertThat(postRepository.existsById(created.getId())).isFalse();
        }

        @Test
        @DisplayName("should throw when post missing")
        void shouldThrowWhenPostMissing() {
            assertThatThrownBy(() -> postRepository.deletePost(9999L))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("9999");
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true for existing post")
        void shouldReturnTrueForExistingPost() {
            long postId = insertPost("Title", "Text", 0, 0);

            assertThat(postRepository.existsById(postId)).isTrue();
        }

        @Test
        @DisplayName("should return false for missing post")
        void shouldReturnFalseForMissingPost() {
            assertThat(postRepository.existsById(1234L)).isFalse();
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {

        @Test
        @DisplayName("should increment likes and return current count")
        void shouldIncrementLikesAndReturnCurrentCount() {
            long postId = insertPost("Title", "Text", 0, 0);

            int first = postRepository.incrementLikes(postId);
            int second = postRepository.incrementLikes(postId);

            assertThat(first).isEqualTo(1);
            assertThat(second).isEqualTo(2);

            Integer persisted = namedJdbcTemplate.queryForObject(
                    "SELECT likes_count FROM post WHERE id = :postId",
                    Map.of("postId", postId),
                    Integer.class
            );
            assertThat(persisted).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw when incrementing missing post")
        void shouldThrowWhenIncrementingMissingPost() {
            assertThatThrownBy(() -> postRepository.incrementLikes(555L))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("555");
        }
    }

    @Nested
    @DisplayName("imageOperations")
    class ImageOperations {

        @Test
        @DisplayName("should update and return image bytes")
        void shouldUpdateAndReturnImageBytes() {
            long postId = insertPost("Title", "Text", 0, 0);
            byte[] image = new byte[]{1, 2, 3};

            boolean updated = postRepository.updateImage(postId, image);
            byte[] stored = postRepository.getImage(postId);

            assertTrue(updated);
            assertThat(stored).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("should return false when updating missing post")
        void shouldReturnFalseWhenUpdatingMissingPost() {
            boolean updated = postRepository.updateImage(404L, new byte[]{1});

            assertThat(updated).isFalse();
        }

        @Test
        @DisplayName("should throw when image not available")
        void shouldThrowWhenImageNotAvailable() {
            long postId = insertPost("Title", "Text", 0, 0);

            assertThatThrownBy(() -> postRepository.getImage(postId))
                    .isInstanceOf(PostImageException.class)
                    .hasMessageContaining(String.valueOf(postId));
        }
    }

    @Nested
    @DisplayName("commentsCounter")
    class CommentsCounter {

        @Test
        @DisplayName("should increment and decrement comments count")
        void shouldIncrementAndDecrementCommentsCount() {
            long postId = insertPost("Title", "Text", 0, 0);

            postRepository.incrementComments(postId);
            postRepository.incrementComments(postId);
            postRepository.incrementComments(postId);
            postRepository.decrementComments(postId);

            Integer comments = namedJdbcTemplate.queryForObject(
                    "SELECT comments_count FROM post WHERE id = :postId",
                    Map.of("postId", postId),
                    Integer.class
            );
            assertThat(comments).isEqualTo(2);
        }
    }

    private long insertPost(String title, String text, int likes, int comments) {
        return namedJdbcTemplate.queryForObject(
                "INSERT INTO post (title, text, likes_count, comments_count) VALUES (:title, :text, :likes, :comments) RETURNING id",
                Map.of("title", title, "text", text, "likes", likes, "comments", comments),
                Long.class
        );
    }

    private long insertTag(String name) {
        return namedJdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES (:name) RETURNING id",
                Map.of("name", name),
                Long.class
        );
    }

    private void linkPostTag(long postId, long tagId) {
        namedJdbcTemplate.update(
                "INSERT INTO post_tag (post_id, tag_id) VALUES (:postId, :tagId)",
                Map.of("postId", postId, "tagId", tagId)
        );
    }
}
