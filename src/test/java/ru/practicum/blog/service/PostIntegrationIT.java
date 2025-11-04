package ru.practicum.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import ru.practicum.blog.config.TestContainersConfig;
import ru.practicum.blog.web.dto.PostRequestDto;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestContainersConfig.class)
@SpringBootTest
@DisplayName("PostService integration")
class PostIntegrationIT {

    @Autowired
    private PostService postService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        namedJdbcTemplate.update("TRUNCATE TABLE comment, post_tag, tag, post RESTART IDENTITY CASCADE", Collections.emptyMap());
    }

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("should return posts matching search query and tags")
        void shouldReturnPostsMatchingSearchQueryAndTags() {
            long matchingPostId = insertPost("Spring Boot Tips", "Detailed content", 4, 2);
            long otherPostId = insertPost("Kotlin News", "Something else", 1, 0);
            long tagSpring = insertTag("spring");
            long tagJava = insertTag("java");
            long tagKotlin = insertTag("kotlin");
            linkPostTag(matchingPostId, tagSpring);
            linkPostTag(matchingPostId, tagJava);
            linkPostTag(otherPostId, tagKotlin);

            PostsResponseDto response = postService.getPosts("Boot #java", 1, 5);

            assertThat(response.posts()).hasSize(1);
            PostResponseDto dto = response.posts().getFirst();
            assertThat(dto.id()).isEqualTo(matchingPostId);
            assertThat(dto.title()).isEqualTo("Spring Boot Tips");
            assertThat(dto.tags()).containsExactlyInAnyOrder("spring", "java");
            assertThat(response.hasPrev()).isFalse();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.lastPage()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("should return post with full text and tags")
        void shouldReturnPostWithFullTextAndTags() {
            long postId = insertPost("Post title", "Post text", 3, 1);
            long tagSpring = insertTag("spring");
            long tagCloud = insertTag("cloud");
            linkPostTag(postId, tagSpring);
            linkPostTag(postId, tagCloud);

            PostResponseDto dto = postService.getPost(postId);

            assertThat(dto.id()).isEqualTo(postId);
            assertThat(dto.text()).isEqualTo("Post text");
            assertThat(dto.tags()).containsExactlyInAnyOrder("spring", "cloud");
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should persist new post with normalized tags")
        void shouldPersistNewPostWithNormalizedTags() {
            PostRequestDto requestDto = new PostRequestDto(null, "New Title", "Markdown text", List.of(" Java ", "SPRING"));

            PostResponseDto responseDto = postService.createPost(requestDto);

            assertThat(responseDto.id()).isNotNull();
            assertThat(responseDto.tags()).containsExactlyInAnyOrder("java", "spring");

            Long count = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE id = :id",
                    Map.of("id", responseDto.id()),
                    Long.class
            );
            assertThat(count).isEqualTo(1L);

            List<String> persistedTags = namedJdbcTemplate.queryForList(
                    "SELECT t.name FROM tag t JOIN post_tag pt ON t.id = pt.tag_id WHERE pt.post_id = :postId",
                    Map.of("postId", responseDto.id()),
                    String.class
            );
            assertThat(persistedTags).containsExactlyInAnyOrder("java", "spring");
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should update title text and tags")
        void shouldUpdateTitleTextAndTags() {
            PostResponseDto created = postService.createPost(new PostRequestDto(null, "Old title", "Old text", List.of("java")));
            PostRequestDto updateRequest = new PostRequestDto(created.id(), "Updated title", "Updated text", List.of(" Spring ", "CLOUD"));

            PostResponseDto updated = postService.updatePost(created.id(), updateRequest);

            assertThat(updated.title()).isEqualTo("Updated title");
            assertThat(updated.text()).isEqualTo("Updated text");
            assertThat(updated.tags()).containsExactlyInAnyOrder("spring", "cloud");

            Map<String, Object> params = Map.of("id", created.id());
            String persistedTitle = namedJdbcTemplate.queryForObject(
                    "SELECT title FROM post WHERE id = :id",
                    params,
                    String.class
            );
            assertThat(persistedTitle).isEqualTo("Updated title");

            List<String> persistedTags = namedJdbcTemplate.queryForList(
                    "SELECT t.name FROM tag t JOIN post_tag pt ON t.id = pt.tag_id WHERE pt.post_id = :postId",
                    Map.of("postId", created.id()),
                    String.class
            );
            assertThat(persistedTags).containsExactlyInAnyOrder("spring", "cloud");
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delete post and related tags links")
        void shouldDeletePostAndRelatedTagsLinks() {
            PostResponseDto created = postService.createPost(new PostRequestDto(null, "Title", "Text", List.of("java")));

            postService.deletePost(created.id());

            Long count = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE id = :id",
                    Map.of("id", created.id()),
                    Long.class
            );
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {

        @Test
        @DisplayName("should increment likes count for post")
        void shouldIncrementLikesCountForPost() {
            long postId = insertPost("Title", "Text", 0, 0);

            int likes = postService.incrementLikes(postId);

            assertThat(likes).isEqualTo(1);
            Integer persisted = namedJdbcTemplate.queryForObject(
                    "SELECT likes_count FROM post WHERE id = :id",
                    Map.of("id", postId),
                    Integer.class
            );
            assertThat(persisted).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateImage")
    class UpdateImage {

        @Test
        @DisplayName("should store image bytes in database")
        void shouldStoreImageBytesInDatabase() {
            long postId = insertPost("Title", "Text", 0, 0);
            byte[] imageBytes = new byte[]{1, 2, 3, 4};
            MockMultipartFile image = new MockMultipartFile("image", "image.png", "image/png", imageBytes);

            postService.updateImage(postId, image);

            byte[] stored = namedJdbcTemplate.queryForObject(
                    "SELECT image FROM post WHERE id = :id",
                    Map.of("id", postId),
                    byte[].class
            );
            assertThat(stored).containsExactly(imageBytes);
        }
    }

    @Nested
    @DisplayName("getImage")
    class GetImage {

        @Test
        @DisplayName("should return stored image bytes")
        void shouldReturnStoredImageBytes() {
            long postId = insertPost("Title", "Text", 0, 0);
            byte[] imageBytes = new byte[]{9, 8, 7};
            namedJdbcTemplate.update(
                    "UPDATE post SET image = :image WHERE id = :id",
                    Map.of("image", imageBytes, "id", postId)
            );

            byte[] loaded = postService.getImage(postId);

            assertThat(loaded).containsExactly(imageBytes);
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
