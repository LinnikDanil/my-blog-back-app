package ru.practicum.blog.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
    Long id;
    String title;
    String text;
    Integer likesCount;
    Integer commentsCount;
    byte[] image;
    List<Tag> tags;
    LocalDateTime createdAt;
}
