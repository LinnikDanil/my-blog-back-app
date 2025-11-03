package ru.practicum.blog.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.blog.domain.exception.CommentBadRequestException;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.service.CommentService;
import ru.practicum.blog.util.TestDataFactory;
import ru.practicum.blog.web.advice.DefaultExceptionHandler;
import ru.practicum.blog.web.dto.CommentRequestDto;
import ru.practicum.blog.web.dto.CommentResponseDto;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommentControllerWebMvc")
@WebMvcTest(controllers = CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoSpyBean
    private DefaultExceptionHandler exceptionHandler;

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("should return comments for post")
        void shouldReturnCommentsForPost() throws Exception {
            List<CommentResponseDto> comments = List.of(
                    new CommentResponseDto(1L, "First", 1L),
                    new CommentResponseDto(2L, "Second", 1L)
            );
            when(commentService.getComments(1L)).thenReturn(comments);

            mockMvc.perform(get("/api/posts/{postId}/comments", 1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].text").value("First"))
                    .andExpect(jsonPath("$[0].postId").value(1))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].text").value("Second"))
                    .andExpect(jsonPath("$[1].postId").value(1));

            verify(commentService).getComments(1L);
        }

        @Test
        @DisplayName("should return empty list when no comments")
        void shouldReturnEmptyListWhenNoComments() throws Exception {
            when(commentService.getComments(2L)).thenReturn(List.of());

            mockMvc.perform(get("/api/posts/{postId}/comments", 2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(commentService).getComments(2L);
        }
    }

    @Nested
    @DisplayName("getComment")
    class GetComment {

        @Test
        @DisplayName("should return single comment")
        void shouldReturnSingleComment() throws Exception {
            CommentResponseDto responseDto = new CommentResponseDto(3L, "Reply", 1L);
            when(commentService.getComment(1L, 3L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", 1, 3))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.text").value("Reply"))
                    .andExpect(jsonPath("$.postId").value(1));

            verify(commentService).getComment(1L, 3L);
        }

        @Test
        @DisplayName("should return 404 when comment missing")
        void shouldReturn404WhenCommentMissing() throws Exception {
            when(commentService.getComment(1L, 5L))
                    .thenThrow(new CommentNotFoundException("Comment with id = 5 for post id = 1 was not found."));

            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", 1, 5))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error")
                            .value("Comment with id = 5 for post id = 1 was not found."));

            verify(commentService).getComment(1L, 5L);
            verify(exceptionHandler, times(1)).commentNotFoundException(any(CommentNotFoundException.class));
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should create comment for post")
        void shouldCreateCommentForPost() throws Exception {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(null, "New comment", 1L);
            CommentResponseDto responseDto = new CommentResponseDto(10L, "New comment", 1L);
            when(commentService.createComment(1L, requestDto)).thenReturn(responseDto);

            mockMvc.perform(post("/api/posts/{postId}/comments", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.text").value("New comment"))
                    .andExpect(jsonPath("$.postId").value(1));

            verify(commentService).createComment(1L, requestDto);
        }

        @Test
        @DisplayName("should return 400 on validation error")
        void shouldReturn400OnValidationError() throws Exception {
            CommentRequestDto invalidRequest = new CommentRequestDto(null, "", 1L);

            mockMvc.perform(post("/api/posts/{postId}/comments", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(exceptionHandler).methodArgumentNotValidException(any());
            verifyNoInteractions(commentService);
        }

        @Test
        @DisplayName("should return 400 when post id mismatch")
        void shouldReturn400WhenPostIdMismatch() throws Exception {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(null, "New comment", 2L);
            when(commentService.createComment(1L, requestDto))
                    .thenThrow(new CommentBadRequestException("Post id in the path and request body must match."));

            mockMvc.perform(post("/api/posts/{postId}/comments", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value("Post id in the path and request body must match."));

            verify(commentService).createComment(1L, requestDto);
            verify(exceptionHandler).commentBadRequestException(any(CommentBadRequestException.class));
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("should update comment when ids match")
        void shouldUpdateCommentWhenIdsMatch() throws Exception {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(4L, "Updated", 1L);
            CommentResponseDto responseDto = new CommentResponseDto(4L, "Updated", 1L);
            when(commentService.updateComment(1L, 4L, requestDto)).thenReturn(responseDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", 1, 4)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.text").value("Updated"))
                    .andExpect(jsonPath("$.postId").value(1));

            verify(commentService).updateComment(1L, 4L, requestDto);
        }

        @Test
        @DisplayName("should return 400 when ids mismatch")
        void shouldReturn400WhenIdsMismatch() throws Exception {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(5L, "Updated", 1L);
            when(commentService.updateComment(1L, 4L, requestDto))
                    .thenThrow(new CommentBadRequestException("Comment id in the path and request body must match."));

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", 1, 4)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value("Comment id in the path and request body must match."));

            verify(commentService).updateComment(1L, 4L, requestDto);
            verify(exceptionHandler).commentBadRequestException(any(CommentBadRequestException.class));
        }

        @Test
        @DisplayName("should return 404 when comment missing on update")
        void shouldReturn404WhenCommentMissingOnUpdate() throws Exception {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(4L, "Updated", 1L);
            when(commentService.updateComment(1L, 4L, requestDto))
                    .thenThrow(new CommentNotFoundException("Comment with id = 4 for post id = 1 was not found."));

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", 1, 4)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error")
                            .value("Comment with id = 4 for post id = 1 was not found."));

            verify(commentService).updateComment(1L, 4L, requestDto);
            verify(exceptionHandler).commentNotFoundException(any(CommentNotFoundException.class));
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment")
        void shouldDeleteComment() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 1, 6))
                    .andExpect(status().isOk());

            verify(commentService).deleteComment(1L, 6L);
        }

        @Test
        @DisplayName("should return 404 when deleting missing comment")
        void shouldReturn404WhenDeletingMissingComment() throws Exception {
            doThrow(new CommentNotFoundException("Comment with id = 6 for post id = 1 was not found."))
                    .when(commentService).deleteComment(1L, 6L);

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 1, 6))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error")
                            .value("Comment with id = 6 for post id = 1 was not found."));

            verify(commentService).deleteComment(1L, 6L);
            verify(exceptionHandler).commentNotFoundException(any(CommentNotFoundException.class));
        }
    }
}
