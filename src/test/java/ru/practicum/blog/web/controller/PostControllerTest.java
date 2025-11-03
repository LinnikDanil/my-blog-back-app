package ru.practicum.blog.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.blog.domain.exception.PostBadRequestException;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.service.PostService;
import ru.practicum.blog.util.TestDataFactory;
import ru.practicum.blog.web.advice.DefaultExceptionHandler;
import ru.practicum.blog.web.dto.PostRequestDto;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PostControllerWebMvc")
@WebMvcTest(controllers = PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoSpyBean
    private DefaultExceptionHandler exceptionHandler;

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("should return paginated posts with truncation")
        void shouldReturnPaginatedPostsWithTruncation() throws Exception {
            var responseDto1 = new PostResponseDto(1L, "New title 1", "Markdown text 1", List.of("tag_1", "tag_2"), 0, 0);
            var responseDto2 = new PostResponseDto(2L, "New title 2", "Markdown text 2", List.of("tag_3", "tag_4"), 1, 2);
            var postsResponseDto = new PostsResponseDto(List.of(responseDto1, responseDto2), false, true, 10);


            when(postService.getPosts("text", 1, 10))
                    .thenReturn(postsResponseDto);

            mockMvc.perform(get("/api/posts")
                            .param("search", "text")
                            .param("pageNumber", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.posts.length()").value(2))
                    .andExpect(jsonPath("$.posts[0].id").value(1))
                    .andExpect(jsonPath("$.posts[0].title").value("New title 1"))
                    .andExpect(jsonPath("$.posts[0].text").value("Markdown text 1"))
                    .andExpect(jsonPath("$.posts[0].tags", containsInAnyOrder("tag_1", "tag_2")))
                    .andExpect(jsonPath("$.posts[0].likesCount").value(0))
                    .andExpect(jsonPath("$.posts[0].commentsCount").value(0))
                    .andExpect(jsonPath("$.posts[1].id").value(2))
                    .andExpect(jsonPath("$.posts[1].title").value("New title 2"))
                    .andExpect(jsonPath("$.posts[1].text").value("Markdown text 2"))
                    .andExpect(jsonPath("$.posts[1].tags", containsInAnyOrder("tag_3", "tag_4")))
                    .andExpect(jsonPath("$.posts[1].likesCount").value(1))
                    .andExpect(jsonPath("$.posts[1].commentsCount").value(2))
                    .andExpect(jsonPath("$.hasPrev").value(false))
                    .andExpect(jsonPath("$.hasNext").value(true))
                    .andExpect(jsonPath("$.lastPage").value(10));
        }

        @ParameterizedTest(name = "page {0}")
        @CsvSource({"1,false", "2,true"})
        @DisplayName("should expose navigation flags")
        void shouldExposeNavigationFlags(int pageNumber, boolean hasPrev) throws Exception {
            when(postService.getPosts("#java", pageNumber, 1))
                    .thenReturn(new PostsResponseDto(List.of(), hasPrev, !hasPrev, 2));

            mockMvc.perform(get("/api/posts")
                            .param("search", "#java")
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPrev").value(hasPrev))
                    .andExpect(jsonPath("$.hasNext").value(!hasPrev));
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("should return full post information")
        void shouldReturnFullPostInformation() throws Exception {
            var responseDto = new PostResponseDto(1L, "New title 1", "Markdown text 1", List.of("tag_1", "tag_2"), 5, 2);
            when(postService.getPost(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/posts/{id}", 1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("New title 1"))
                    .andExpect(jsonPath("$.text").value("Markdown text 1"))
                    .andExpect(jsonPath("$.tags", containsInAnyOrder("tag_1", "tag_2")))
                    .andExpect(jsonPath("$.likesCount").value(5))
                    .andExpect(jsonPath("$.commentsCount").value(2));
        }

        @Test
        @DisplayName("should return 404 when post missing")
        void shouldReturn404WhenPostMissing() throws Exception {
            when(postService.getPost(99L)).thenThrow(new PostNotFoundException("Post with id = 99 was not found."));

            mockMvc.perform(get("/api/posts/{id}", 99))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Post with id = 99 was not found."));

            verify(postService).getPost(99L);
            verify(exceptionHandler, times(1)).postNotFoundException(any(PostNotFoundException.class));
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should create post and return dto")
        void shouldCreatePostAndReturnDto() throws Exception {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(null, "New title", "Markdown text", List.of("tag_1", "tag_2"));
            PostResponseDto responseDto = new PostResponseDto(3L, "New title", "Markdown text", List.of("tag_1", "tag_2"), 0, 0);
            when(postService.createPost(requestDto)).thenReturn(responseDto);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.title").value("New title"))
                    .andExpect(jsonPath("$.text").value("Markdown text"))
                    .andExpect(jsonPath("$.tags", containsInAnyOrder("tag_1", "tag_2")))
                    .andExpect(jsonPath("$.likesCount").value(0))
                    .andExpect(jsonPath("$.commentsCount").value(0));

            verify(postService).createPost(requestDto);
        }

        @Test
        @DisplayName("should return 400 on validation error")
        void shouldReturn400OnValidationError() throws Exception {
            PostRequestDto invalidRequest = new PostRequestDto(null, "", "Valid text", List.of("tag_1"));

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(exceptionHandler).methodArgumentNotValidException(any());
            verifyNoInteractions(postService);
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should update post when ids match")
        void shouldUpdatePostWhenIdsMatch() throws Exception {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(5L, "Updated", "Updated text", List.of("tag_1"));
            PostResponseDto responseDto = new PostResponseDto(5L, "Updated", "Updated text", List.of("tag_1"), 7, 3);
            when(postService.updatePost(5L, requestDto)).thenReturn(responseDto);

            mockMvc.perform(put("/api/posts/{id}", 5)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.title").value("Updated"))
                    .andExpect(jsonPath("$.text").value("Updated text"))
                    .andExpect(jsonPath("$.tags", containsInAnyOrder("tag_1")))
                    .andExpect(jsonPath("$.likesCount").value(7))
                    .andExpect(jsonPath("$.commentsCount").value(3));

            verify(postService).updatePost(5L, requestDto);
        }

        @Test
        @DisplayName("should return 400 when ids mismatch")
        void shouldReturn400WhenIdsMismatch() throws Exception {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(7L, "Updated", "Updated text", List.of("tag_1"));
            when(postService.updatePost(5L, requestDto)).thenThrow(new PostBadRequestException("Post id in the path and request body must match."));

            mockMvc.perform(put("/api/posts/{id}", 5)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Post id in the path and request body must match."));

            verify(postService).updatePost(5L, requestDto);
            verify(exceptionHandler).postBadRequestException(any(PostBadRequestException.class));
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delete post and comments")
        void shouldDeletePostAndComments() throws Exception {
            mockMvc.perform(delete("/api/posts/{id}", 7))
                    .andExpect(status().isOk());

            verify(postService).deletePost(7L);
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {

        @Test
        @DisplayName("should increase likes count")
        void shouldIncreaseLikesCount() throws Exception {
            when(postService.incrementLikes(9L)).thenReturn(11);

            mockMvc.perform(post("/api/posts/{id}/likes", 9))
                    .andExpect(status().isOk())
                    .andExpect(content().string("11"));

            verify(postService).incrementLikes(9L);
        }
    }

    @Nested
    @DisplayName("imageOperations")
    class ImageOperations {

        @Test
        @DisplayName("should update image when file provided")
        void shouldUpdateImageWhenFileProvided() throws Exception {
            MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", "binary".getBytes());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{id}/image", 1)
                            .file(file)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());

            verify(postService).updateImage(eq(1L), any());
        }

        @Test
        @DisplayName("should return 400 when image empty")
        void shouldReturn400WhenImageEmpty() throws Exception {
            MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);
            doThrow(new PostImageException("Image cannot be empty.")).when(postService).updateImage(eq(1L), any());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{id}/image", 1)
                            .file(file)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Image cannot be empty."));

            verify(postService).updateImage(eq(1L), any());
            verify(exceptionHandler).postImageException(any(PostImageException.class));
        }

        @Test
        @DisplayName("should return image bytes")
        void shouldReturnImageBytes() throws Exception {
            byte[] image = "binary".getBytes();
            when(postService.getImage(3L)).thenReturn(image);

            mockMvc.perform(get("/api/posts/{id}/image", 3)
                            .accept(MediaType.IMAGE_JPEG))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", startsWith(MediaType.IMAGE_JPEG_VALUE)))
                    .andExpect(content().bytes(image));

            verify(postService).getImage(3L);
        }
    }
}
