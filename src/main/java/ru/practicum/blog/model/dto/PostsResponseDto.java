package ru.practicum.blog.model.dto;

import java.util.List;

public record PostsResponseDto(
        List<PostResponseDto> posts,
        boolean hasPrev, // true - если текущая страница не первая
        boolean hasNext, // true - если текущая страница не последняя
        int lastPage // номер последней страницы
) {
}
