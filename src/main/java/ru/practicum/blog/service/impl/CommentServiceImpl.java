package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.blog.exception.CommentBadRequestException;
import ru.practicum.blog.exception.CommentNotFoundException;
import ru.practicum.blog.exception.PostNotFoundException;
import ru.practicum.blog.model.Comment;
import ru.practicum.blog.model.dto.CommentRequestDto;
import ru.practicum.blog.model.dto.CommentResponseDto;
import ru.practicum.blog.model.mapper.CommentMapper;
import ru.practicum.blog.repository.CommentRepository;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.CommentService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    public List<CommentResponseDto> getComments(long postId) {
        checkExistencePost(postId);

        List<Comment> comments = commentRepository.findCommentsByPostId(postId);
        return CommentMapper.toCommentDtoList(comments);
    }

    @Override
    public CommentResponseDto getComment(long postId, long commentId) {
        checkExistencePost(postId);

        Comment comment = commentRepository.findCommentById(postId, commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Комментарий с id = %d у поста id = %d не найден".formatted(commentId, postId)));

        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public CommentResponseDto createComment(long postId, CommentRequestDto commentRequestDto) {
        checkExistencePost(postId);

        if (postId != commentRequestDto.postId()) {
            throw new CommentBadRequestException("id поста в переменной пути и теле запроса должны совпадать.");
        }

        Comment comment = commentRepository.createComment(postId, commentRequestDto.text());
        postRepository.incrementComments(postId);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public CommentResponseDto updateComment(long postId, long commentId, CommentRequestDto commentRequestDto) {

        if (postId != commentRequestDto.postId()) {
            throw new CommentBadRequestException("id поста в переменной пути и теле запроса должны совпадать.");
        }

        if (commentRequestDto.id() == null || commentRequestDto.id() != commentId) {
            throw new CommentBadRequestException("id комментария в переменной пути и теле запроса должны совпадать.");
        }

        checkExistencePost(postId);
        checkExistenceComment(postId, commentId);

        Comment comment = commentRepository.updateComment(postId, commentId, commentRequestDto.text());

        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public void deleteComment(long postId, long commentId) {
        commentRepository.deleteComment(postId, commentId);
        postRepository.decrementComments(postId);
    }

    private void checkExistencePost(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Пост с id = %d не найден".formatted(postId));
        }
    }

    private void checkExistenceComment(long postId, long commentId) {
        if (!commentRepository.existsById(postId, commentId)) {
            throw new CommentNotFoundException("Комментарий с id = %d у поста с id = %d не найден"
                    .formatted(commentId, postId)
            );
        }
    }
}
