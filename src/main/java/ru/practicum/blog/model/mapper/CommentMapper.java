package ru.practicum.blog.model.mapper;

import ru.practicum.blog.model.Comment;
import ru.practicum.blog.model.dto.CommentResponseDto;

import java.util.Collections;
import java.util.List;

public class CommentMapper {
    public static List<CommentResponseDto> toCommentDtoList(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    public static CommentResponseDto toCommentDto(Comment comment) {
        return new CommentResponseDto(comment.getId(), comment.getText(), comment.getPostId());
    }
}
