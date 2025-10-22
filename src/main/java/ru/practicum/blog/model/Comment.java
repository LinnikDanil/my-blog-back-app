package ru.practicum.blog.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {
    Long id;
    String text;
    Long postId;
    LocalDateTime createdAt;
}
