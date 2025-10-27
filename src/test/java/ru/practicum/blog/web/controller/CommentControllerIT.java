package ru.practicum.blog.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.blog.config.TestWebApplicationConfiguration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = TestWebApplicationConfiguration.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@WebAppConfiguration
@ActiveProfiles("test")
@DisplayName("CommentControllerIT")
class CommentControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("DELETE FROM comment", Map.of());
        jdbcTemplate.update("DELETE FROM post", Map.of());

        jdbcTemplate.update(
                "INSERT INTO post (id, title, text, comments_count) VALUES(:id, :title, :text, :comments)",
                new MapSqlParameterSource()
                        .addValue("id", 1L)
                        .addValue("title", "Post")
                        .addValue("text", "Body")
                        .addValue("comments", 2)
        );

        jdbcTemplate.update(
                "INSERT INTO comment (id, text, post_id) VALUES(:id, :text, :postId)",
                new MapSqlParameterSource().addValue("id", 11L).addValue("text", "First").addValue("postId", 1L)
        );
        jdbcTemplate.update(
                "INSERT INTO comment (id, text, post_id) VALUES(:id, :text, :postId)",
                new MapSqlParameterSource().addValue("id", 12L).addValue("text", "Second").addValue("postId", 1L)
        );
    }

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("should return comments for post")
        void shouldReturnCommentsForPost() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(12))
                    .andExpect(jsonPath("$[0].text").value("Second"));
        }

        @Test
        @DisplayName("should return 404 when post missing")
        void shouldReturn404WhenPostMissing() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", 9L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("getComment")
    class GetComment {

        @Test
        @DisplayName("should return comment by id")
        void shouldReturnCommentById() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", 1L, 11L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("First"));
        }

        @Test
        @DisplayName("should return 404 when comment absent")
        void shouldReturn404WhenCommentAbsent() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", 1L, 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should create comment")
        void shouldCreateComment() throws Exception {
            String payload = "{" +
                    "\"text\":\"New\"," +
                    "\"postId\":1}";

            mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("New"));

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE text = :text",
                    Map.of("text", "New"),
                    Long.class
            );
            assertEquals(1L, count);
        }

        @Test
        @DisplayName("should return 400 when postId mismatch")
        void shouldReturn400WhenPostIdMismatch() throws Exception {
            String payload = "{" +
                    "\"text\":\"New\"," +
                    "\"postId\":2}";

            mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("should update comment when ids match")
        void shouldUpdateCommentWhenIdsMatch() throws Exception {
            String payload = "{" +
                    "\"id\":11," +
                    "\"text\":\"Updated\"," +
                    "\"postId\":1}";

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", 1L, 11L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Updated"));
        }

        @Test
        @DisplayName("should return 400 on id mismatch")
        void shouldReturn400OnIdMismatch() throws Exception {
            String payload = "{" +
                    "\"id\":10," +
                    "\"text\":\"Updated\"," +
                    "\"postId\":1}";

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", 1L, 11L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment")
        void shouldDeleteComment() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 1L, 11L))
                    .andExpect(status().isOk());

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE id = :id",
                    Map.of("id", 11L),
                    Long.class
            );
            assertEquals(0L, count);
        }
    }
}
