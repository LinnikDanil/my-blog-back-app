package ru.practicum.blog.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.domain.exception.PostBadRequestException;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.util.TestDataFactory;
import ru.practicum.blog.web.dto.PostRequestDto;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImplTest")
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostServiceImpl postService;

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("should split search query and calculate pagination")
        void shouldSplitSearchQueryAndCalculatePagination() {
            Post post = TestDataFactory.createPost(1L, "Spring", "content", List.of("java"), 2, 1);
            when(postRepository.findPosts(any(), any(), eq(5), eq(5L))).thenReturn(List.of(post));
            when(postRepository.countPosts(any(), any())).thenReturn(8L);

            PostsResponseDto responseDto = postService.getPosts("  Spring  #JAVA  ", 2, 5);

            ArgumentCaptor<Set<String>> tagsCaptor = ArgumentCaptor.forClass(Set.class);
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(postRepository).findPosts(tagsCaptor.capture(), titleCaptor.capture(), eq(5), eq(5L));

            assertEquals(Set.of("java"), tagsCaptor.getValue());
            assertEquals("spring", titleCaptor.getValue());
            assertEquals(1, responseDto.posts().size());
            assertTrue(responseDto.hasPrev());
            assertFalse(responseDto.hasNext());
            assertEquals(2, responseDto.lastPage());
        }

        @Test
        @DisplayName("should throw when requested page exceeds last page")
        void shouldThrowWhenRequestedPageExceedsLastPage() {
            when(postRepository.findPosts(any(), any(), eq(5), eq(10L))).thenReturn(List.of());
            when(postRepository.countPosts(any(), any())).thenReturn(10L);

            assertThrows(PostBadRequestException.class, () -> postService.getPosts("test", 3, 5));
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("should return post response when exists")
        void shouldReturnPostResponseWhenExists() {
            Post post = TestDataFactory.createPost(5L, "Spring", "Full text", List.of("java"), 1, 0);
            when(postRepository.findPostById(5L)).thenReturn(Optional.of(post));

            PostResponseDto dto = postService.getPost(5L);

            assertEquals(5L, dto.id());
            assertEquals("Full text", dto.text());
        }

        @Test
        @DisplayName("should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            when(postRepository.findPostById(10L)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPost(10L));
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @ParameterizedTest(name = "tags {0}")
        @CsvSource(value = {"JAVA|SPRING", "java| spring "}, delimiter = '|')
        @DisplayName("should normalize tags before persisting")
        void shouldNormalizeTagsBeforePersisting(String tagOne, String tagTwo) {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(null, "Title", "Body", List.of(tagOne, tagTwo));
            Post post = TestDataFactory.createPost(7L, "Title", "Body", List.of("java", "spring"), 0, 0);
            when(postRepository.createPost(eq("Title"), eq("Body"), eq(List.of("java", "spring")))).thenReturn(post);

            PostResponseDto dto = postService.createPost(requestDto);

            assertEquals(7L, dto.id());
            assertEquals(List.of("java", "spring"), dto.tags());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should throw when ids do not match")
        void shouldThrowWhenIdsDoNotMatch() {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(10L, "Title", "Text", List.of("java"));

            assertThrows(PostBadRequestException.class, () -> postService.updatePost(5L, requestDto));
        }

        @Test
        @DisplayName("should update post when ids match")
        void shouldUpdatePostWhenIdsMatch() {
            PostRequestDto requestDto = TestDataFactory.createPostRequestDto(5L, "Updated", "Text", List.of(" Java ", "SPRING"));
            Post updatedPost = TestDataFactory.createPost(5L, "Updated", "Text", List.of("java", "spring"), 1, 1);
            when(postRepository.existsById(5L)).thenReturn(true);
            when(postRepository.updatePost(eq(5L), eq("Updated"), eq("Text"), eq(List.of("java", "spring")))).thenReturn(updatedPost);

            PostResponseDto dto = postService.updatePost(5L, requestDto);

            assertEquals(5L, dto.id());
            assertEquals("Updated", dto.title());
            assertEquals(List.of("java", "spring"), dto.tags());
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delegate deletion to repository")
        void shouldDelegateDeletionToRepository() {
            postService.deletePost(4L);

            verify(postRepository).deletePost(4L);
        }
    }

    @Nested
    @DisplayName("incrementLikes")
    class IncrementLikes {

        @Test
        @DisplayName("should return incremented likes count")
        void shouldReturnIncrementedLikesCount() {
            when(postRepository.incrementLikes(3L)).thenReturn(11);

            int likes = postService.incrementLikes(3L);

            assertEquals(11, likes);
        }
    }

    @Nested
    @DisplayName("updateImage")
    class UpdateImage {

        @Test
        @DisplayName("should throw when image empty")
        void shouldThrowWhenImageEmpty() {
            MultipartFile image = TestDataFactory.createEmptyMultipartFile("image");
            when(postRepository.existsById(2L)).thenReturn(true);

            assertThrows(PostImageException.class, () -> postService.updateImage(2L, image));
        }

        @Test
        @DisplayName("should throw when repository returns false")
        void shouldThrowWhenRepositoryReturnsFalse() {
            MultipartFile image = TestDataFactory.createMultipartFile("image", TestDataFactory.stringAsBytes("image"));
            when(postRepository.existsById(2L)).thenReturn(true);
            when(postRepository.updateImage(eq(2L), any())).thenReturn(false);

            assertThrows(PostImageException.class, () -> postService.updateImage(2L, image));
        }

        @Test
        @DisplayName("should throw when image bytes reading fails")
        void shouldThrowWhenImageBytesReadingFails() throws IOException {
            MultipartFile image = mock(MultipartFile.class);
            when(image.isEmpty()).thenReturn(false);
            when(image.getBytes()).thenThrow(new IOException("IO"));
            when(postRepository.existsById(2L)).thenReturn(true);

            assertThrows(PostImageException.class, () -> postService.updateImage(2L, image));
        }
    }

    @Nested
    @DisplayName("getImage")
    class GetImage {

        @Test
        @DisplayName("should return image bytes when post exists")
        void shouldReturnImageBytesWhenPostExists() {
            when(postRepository.existsById(9L)).thenReturn(true);
            when(postRepository.getImage(9L)).thenReturn(TestDataFactory.stringAsBytes("img"));

            byte[] image = postService.getImage(9L);

            assertEquals("img", new String(image));
        }
    }
}
