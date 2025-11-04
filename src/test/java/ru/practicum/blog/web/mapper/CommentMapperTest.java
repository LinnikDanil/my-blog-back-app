package ru.practicum.blog.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.util.TestDataFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommentMapperTest")
class CommentMapperTest {

    @Nested
    @DisplayName("toCommentDtoList")
    class ToCommentDtoList {

        @Test
        @DisplayName("should return empty list when no comments")
        void shouldReturnEmptyListWhenNoComments() {
            var dtoList = CommentMapper.toCommentDtoList(List.of());

            assertTrue(dtoList.isEmpty());
        }

        @Test
        @DisplayName("should map comments to dto list")
        void shouldMapCommentsToDtoList() {
            Comment comment = TestDataFactory.createComment(1L, "text", 4L);

            var dtoList = CommentMapper.toCommentDtoList(List.of(comment));

            assertEquals(1, dtoList.size());
            var dto = dtoList.getFirst();
            assertEquals(1L, dto.id());
            assertEquals("text", dto.text());
            assertEquals(4L, dto.postId());
        }
    }

    @Nested
    @DisplayName("toCommentDto")
    class ToCommentDto {

        @Test
        @DisplayName("should map comment to dto")
        void shouldMapCommentToDto() {
            Comment comment = TestDataFactory.createComment(2L, "another", 5L);

            var dto = CommentMapper.toCommentDto(comment);

            assertEquals(2L, dto.id());
            assertEquals("another", dto.text());
            assertEquals(5L, dto.postId());
        }
    }
}
