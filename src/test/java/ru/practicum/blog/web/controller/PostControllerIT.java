package ru.practicum.blog.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.blog.config.TestWebApplicationConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(classes = TestWebApplicationConfiguration.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@WebAppConfiguration
@DisplayName("PostControllerIT")
class PostControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    private long post1Id;
    private long post2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        jdbcTemplate.update("DELETE FROM post_tag", Map.of());
        jdbcTemplate.update("DELETE FROM comment", Map.of());
        jdbcTemplate.update("DELETE FROM tag", Map.of());
        jdbcTemplate.update("DELETE FROM post", Map.of());

        post1Id = insertPost("Spring Guide", "Detailed content".repeat(20), 1, 2);
        post2Id = insertPost("Java Tips", "Short", 0, 0);

        long springTagId = insertTag("spring");
        long javaTagId = insertTag("java");

        linkPostTag(post1Id, springTagId);
        linkPostTag(post1Id, javaTagId);
        linkPostTag(post2Id, javaTagId);

        insertComment("Nice", post1Id);
        insertComment("Great", post1Id);
    }

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("should return paginated posts with truncation")
        void shouldReturnPaginatedPostsWithTruncation() throws Exception {
            mockMvc.perform(get("/api/posts")
                            .param("search", "guide #java")
                            .param("pageNumber", "1")
                            .param("pageSize", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[0].id").value((int) post1Id))
                    .andExpect(jsonPath("$.posts[0].tags").isArray())
                    .andExpect(jsonPath("$.posts[0].text").value(endsWith("…")))
                    .andExpect(jsonPath("$.hasPrev").value(false))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.lastPage").value(1));
        }

        @ParameterizedTest(name = "page {0}")
        @CsvSource({"1,false", "2,true"})
        @DisplayName("should expose navigation flags")
        void shouldExposeNavigationFlags(int pageNumber, boolean hasPrev) throws Exception {
            mockMvc.perform(get("/api/posts")
                            .param("search", "#java")
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPrev").value(hasPrev));
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("should return full post information")
        void shouldReturnFullPostInformation() throws Exception {
            mockMvc.perform(get("/api/posts/{id}", post1Id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value((int) post1Id))
                    .andExpect(jsonPath("$.tags", containsInAnyOrder("spring", "java")))
                    .andExpect(jsonPath("$.commentsCount").value(2));
        }

        @Test
        @DisplayName("should return 404 when post missing")
        void shouldReturn404WhenPostMissing() throws Exception {
            mockMvc.perform(get("/api/posts/{id}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should create post and return dto")
        void shouldCreatePostAndReturnDto() throws Exception {
            String payload = """
                    {
                      "title":"New",
                      "text":"Content",
                      "tags":["java"]
                    }
                    """;

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New"))
                    .andExpect(jsonPath("$.likesCount").value(0))
                    .andExpect(jsonPath("$.commentsCount").value(0))
                    .andExpect(jsonPath("$.tags", hasItem("java")));

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE title = :title",
                    Map.of("title", "New"),
                    Long.class
            );
            assertEquals(1L, count);
        }

        @Test
        @DisplayName("should return 400 on validation error")
        void shouldReturn400OnValidationError() throws Exception {
            String payload = """
                    {
                      "title":"",
                      "text":"Content",
                      "tags":["java"]
                    }
                    """;

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should update post when ids match")
        void shouldUpdatePostWhenIdsMatch() throws Exception {
            String payload = """
                    {
                      "id": %d,
                      "title":"Updated",
                      "text":"New text",
                      "tags":["spring"]
                    }
                    """.formatted(post1Id);

            mockMvc.perform(put("/api/posts/{id}", post1Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated"))
                    .andExpect(jsonPath("$.tags", containsInAnyOrder("spring")));
        }

        @Test
        @DisplayName("should return 400 when ids mismatch")
        void shouldReturn400WhenIdsMismatch() throws Exception {
            String payload = """
                    {
                      "id": %d,
                      "title":"Updated",
                      "text":"New text",
                      "tags":["spring"]
                    }
                    """.formatted(post2Id);

            mockMvc.perform(put("/api/posts/{id}", post1Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delete post and comments")
        void shouldDeletePostAndComments() throws Exception {
            mockMvc.perform(delete("/api/posts/{id}", post1Id))
                    .andExpect(status().isOk());

            Long postCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE id = :id",
                    Map.of("id", post1Id),
                    Long.class
            );
            assertEquals(0L, postCount);
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {

        @Test
        @DisplayName("should increase likes count")
        void shouldIncreaseLikesCount() throws Exception {
            mockMvc.perform(post("/api/posts/{id}/likes", post1Id))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2")); // было 1, стало 2
        }
    }

    @Nested
    @DisplayName("imageOperations")
    class ImageOperations {

        @Test
        @DisplayName("should update image when file provided")
        void shouldUpdateImageWhenFileProvided() throws Exception {
            byte[] data = "data".getBytes(StandardCharsets.UTF_8);
            MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", data);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{id}/image", post1Id)
                            .file(file)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk());

            byte[] stored = jdbcTemplate.queryForObject(
                    "SELECT image FROM post WHERE id = :id",
                    Map.of("id", post1Id),
                    byte[].class
            );
            assertArrayEquals(data, stored);
        }

        @Test
        @DisplayName("should return 400 when image empty")
        void shouldReturn400WhenImageEmpty() throws Exception {
            MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{id}/image", post1Id)
                            .file(file)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return image bytes")
        void shouldReturnImageBytes() throws Exception {
            jdbcTemplate.update(
                    "UPDATE post SET image = :image WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("image", "hello".getBytes(StandardCharsets.UTF_8))
                            .addValue("id", post1Id)
            );

            mockMvc.perform(get("/api/posts/{id}/image", post1Id))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("image")))
                    .andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private long insertPost(String title, String text, int likes, int comments) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO post (title, text, likes_count, comments_count) " +
                        "VALUES (:title, :text, :likes, :comments) RETURNING id",
                new MapSqlParameterSource()
                        .addValue("title", title)
                        .addValue("text", text)
                        .addValue("likes", likes)
                        .addValue("comments", comments),
                Long.class
        );
    }

    private long insertTag(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES (:name) RETURNING id",
                new MapSqlParameterSource().addValue("name", name),
                Long.class
        );
    }

    private void linkPostTag(long postId, long tagId) {
        jdbcTemplate.update(
                "INSERT INTO post_tag (post_id, tag_id) VALUES(:postId, :tagId)",
                new MapSqlParameterSource().addValue("postId", postId).addValue("tagId", tagId)
        );
    }

    private long insertComment(String text, long postId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO comment (text, post_id) VALUES(:text, :postId) RETURNING id",
                new MapSqlParameterSource().addValue("text", text).addValue("postId", postId),
                Long.class
        );
    }
}
