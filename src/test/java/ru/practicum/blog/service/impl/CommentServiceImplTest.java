package ru.practicum.blog.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.blog.domain.exception.CommentBadRequestException;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.repository.CommentRepository;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.util.TestDataFactory;
import ru.practicum.blog.web.dto.CommentRequestDto;
import ru.practicum.blog.web.dto.CommentResponseDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImplTest")
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("should fetch comments when post exists")
        void shouldFetchCommentsWhenPostExists() {
            Comment comment = TestDataFactory.createComment(1L, "text", 4L);
            when(postRepository.existsById(4L)).thenReturn(true);
            when(commentRepository.findCommentsByPostId(4L)).thenReturn(List.of(comment));

            List<CommentResponseDto> comments = commentService.getComments(4L);

            assertEquals(1, comments.size());
            assertEquals("text", comments.getFirst().text());
        }

        @Test
        @DisplayName("should throw when post does not exist")
        void shouldThrowWhenPostDoesNotExist() {
            when(postRepository.existsById(9L)).thenReturn(false);

            assertThrows(PostNotFoundException.class, () -> commentService.getComments(9L));
        }
    }

    @Nested
    @DisplayName("getComment")
    class GetComment {

        @Test
        @DisplayName("should return comment when it exists")
        void shouldReturnCommentWhenItExists() {
            Comment comment = TestDataFactory.createComment(5L, "body", 3L);
            when(postRepository.existsById(3L)).thenReturn(true);
            when(commentRepository.findCommentById(3L, 5L)).thenReturn(Optional.of(comment));

            CommentResponseDto dto = commentService.getComment(3L, 5L);

            assertEquals(5L, dto.id());
            assertEquals("body", dto.text());
        }

        @Test
        @DisplayName("should throw when comment missing")
        void shouldThrowWhenCommentMissing() {
            when(postRepository.existsById(3L)).thenReturn(true);
            when(commentRepository.findCommentById(3L, 5L)).thenReturn(Optional.empty());
            
            assertThrows(CommentNotFoundException.class, () -> commentService.getComment(3L, 5L));
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @ParameterizedTest(name = "ids {0}")
        @CsvSource({"10,11", "10,9"})
        @DisplayName("should validate post ids")
        void shouldValidatePostIds(long pathId, long bodyId) {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(null, "text", bodyId);
            when(postRepository.existsById(pathId)).thenReturn(true);

            assertThrows(CommentBadRequestException.class, () -> commentService.createComment(pathId, requestDto));
            verify(commentRepository, never()).createComment(anyLong(), anyString());
        }

        @Test
        @DisplayName("should create comment and increment counter")
        void shouldCreateCommentAndIncrementCounter() {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(null, "text", 3L);
            Comment comment = TestDataFactory.createComment(8L, "text", 3L);
            when(postRepository.existsById(3L)).thenReturn(true);
            when(commentRepository.createComment(3L, "text")).thenReturn(comment);

            CommentResponseDto dto = commentService.createComment(3L, requestDto);

            assertEquals(8L, dto.id());
            verify(postRepository).incrementComments(3L);
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @ParameterizedTest(name = "ids {0}-{1}")
        @CsvSource(value = {"2|3", "NULL|1"}, delimiter = '|', nullValues = "NULL")
        @DisplayName("should validate path and body ids")
        void shouldValidatePathAndBodyIds(Long bodyId, Long pathId) {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(bodyId, "text", 5L);

            assertThrows(CommentBadRequestException.class, () -> commentService.updateComment(5L, pathId, requestDto));
        }

        @Test
        @DisplayName("should update comment when ids valid")
        void shouldUpdateCommentWhenIdsValid() {
            CommentRequestDto requestDto = TestDataFactory.createCommentRequestDto(2L, "updated", 5L);
            Comment comment = TestDataFactory.createComment(2L, "updated", 5L);
            when(postRepository.existsById(5L)).thenReturn(true);
            when(commentRepository.existsById(5L, 2L)).thenReturn(true);
            when(commentRepository.updateComment(5L, 2L, "updated")).thenReturn(comment);

            CommentResponseDto dto = commentService.updateComment(5L, 2L, requestDto);

            assertEquals("updated", dto.text());
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete and decrement counter")
        void shouldDeleteAndDecrementCounter() {
            commentService.deleteComment(4L, 3L);

            verify(commentRepository).deleteComment(4L, 3L);
            verify(postRepository).decrementComments(4L);
        }
    }
}
