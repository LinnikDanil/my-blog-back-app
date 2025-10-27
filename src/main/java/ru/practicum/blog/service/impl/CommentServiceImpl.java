package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.blog.domain.exception.CommentBadRequestException;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.repository.CommentRepository;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.CommentService;
import ru.practicum.blog.web.dto.CommentRequestDto;
import ru.practicum.blog.web.dto.CommentResponseDto;
import ru.practicum.blog.web.mapper.CommentMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(long postId) {
        checkExistencePost(postId);

        List<Comment> comments = commentRepository.findCommentsByPostId(postId);
        return CommentMapper.toCommentDtoList(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponseDto getComment(long postId, long commentId) {
        checkExistencePost(postId);

        Comment comment = commentRepository.findCommentById(postId, commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Комментарий с id = %d у поста id = %d не найден".formatted(commentId, postId)));

        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
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
