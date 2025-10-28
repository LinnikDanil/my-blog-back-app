package ru.practicum.blog.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.util.TestDataFactory;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PostMapperTest")
class PostMapperTest {

    @Nested
    @DisplayName("toPostsResponseDto")
    class ToPostsResponseDto {

        @Test
        @DisplayName("should map posts with truncated text and flags")
        void shouldMapPostsWithTruncatedTextAndFlags() {
            Post post = TestDataFactory.createPost(
                    1L,
                    "Title",
                    "T".repeat(200),
                    List.of("java", "spring"),
                    5,
                    2
            );

            PostsResponseDto dto = PostMapper.toPostsResponseDto(List.of(post), true, false, 3);

            assertEquals(1, dto.posts().size());
            PostResponseDto responseDto = dto.posts().getFirst();
            assertEquals(1L, responseDto.id());
            assertEquals(129, responseDto.text().length());
            assertTrue(responseDto.text().endsWith("…"));
            assertEquals(List.of("java", "spring"), responseDto.tags());
            assertTrue(dto.hasPrev());
            assertFalse(dto.hasNext());
            assertEquals(3, dto.lastPage());
        }
    }

    @Nested
    @DisplayName("toPostResponseDto")
    class ToPostResponseDto {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ru.practicum.blog.web.mapper.PostMapperTest#textProvider")
        @DisplayName("should keep provided text value")
        void shouldKeepProvidedTextValue(String description, String text) {
            Post post = TestDataFactory.createPost(
                    10L,
                    "Another",
                    text,
                    List.of("java"),
                    0,
                    0
            );

            PostResponseDto dto = PostMapper.toPostResponseDto(post, text);

            assertEquals(10L, dto.id());
            assertEquals(text, dto.text());
            assertEquals(List.of("java"), dto.tags());
        }
    }

    static Stream<Arguments> textProvider() {
        return Stream.of(
                Arguments.of("short", "short text"),
                Arguments.of("long", "L".repeat(140))
        );
    }
}
