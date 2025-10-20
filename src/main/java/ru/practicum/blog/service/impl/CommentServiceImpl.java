package ru.practicum.blog.service.impl;

import org.springframework.stereotype.Service;
import ru.practicum.blog.model.dto.CommentRequestDto;
import ru.practicum.blog.model.dto.CommentResponseDto;
import ru.practicum.blog.service.CommentService;

import java.util.Set;

@Service
public class CommentServiceImpl implements CommentService {
    @Override
    public Set<CommentResponseDto> getComments(long postId) {
        return Set.of();
    }

    @Override
    public CommentResponseDto getComment(long postId, long commentId) {
        return null;
    }

    @Override
    public CommentResponseDto createComment(long postId, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    public CommentResponseDto updateComment(long postId, long commentId, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    public void deleteComment(long postId, long commentId) {

    }
}
