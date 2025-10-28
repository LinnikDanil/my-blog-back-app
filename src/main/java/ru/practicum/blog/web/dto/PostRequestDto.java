package ru.practicum.blog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostRequestDto(
        Long id,
        @NotBlank String title,
        @NotBlank String text,
        @NotNull List<@NotBlank String> tags
) {
}
