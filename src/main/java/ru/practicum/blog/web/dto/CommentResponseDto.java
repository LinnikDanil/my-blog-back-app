package ru.practicum.blog.web.dto;

public record CommentResponseDto(
        Long id,
        String text,
        Long postId
) {
}
