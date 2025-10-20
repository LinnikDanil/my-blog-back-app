package ru.practicum.blog.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PostRequestDto(
        Long id,
        @NotBlank String title,
        @NotBlank String text,
        @NotEmpty List<@NotBlank String> tags
) {
}
