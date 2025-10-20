package ru.practicum.blog.model.dto;

public record CommentResponseDto(
        Long id,
        String text,
        Long postId
) {
}
