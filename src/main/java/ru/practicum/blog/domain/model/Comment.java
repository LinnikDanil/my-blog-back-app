package ru.practicum.blog.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {
    Long id;
    String text;
    Long postId;
    LocalDateTime createdAt;
}
