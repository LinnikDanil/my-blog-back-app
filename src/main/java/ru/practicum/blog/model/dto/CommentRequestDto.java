package ru.practicum.blog.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CommentRequestDto(
        Long id,
        @NotBlank String text,
        @NotNull @Positive Long postId
) {
}
