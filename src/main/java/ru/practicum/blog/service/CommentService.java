package ru.practicum.blog.service;

import ru.practicum.blog.model.dto.CommentRequestDto;
import ru.practicum.blog.model.dto.CommentResponseDto;

import java.util.List;

public interface CommentService {
    List<CommentResponseDto> getComments(long postId);

    CommentResponseDto getComment(long postId, long commentId);

    CommentResponseDto createComment(long postId, CommentRequestDto commentRequestDto);

    CommentResponseDto updateComment(long postId, long commentId, CommentRequestDto commentRequestDto);

    void deleteComment(long postId, long commentId);
}
