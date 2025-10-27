package ru.practicum.blog.web.dto;

import java.util.List;

public record PostResponseDto(
        Long id,
        String title,
        String text,
        List<String> tags,
        Integer likesCount,
        Integer commentsCount
) {
    public PostResponseDto {
        tags = List.copyOf(tags); // Иммутабельность для неиммутабельного поля
    }
}
