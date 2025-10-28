package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(long postId) {
        log.debug("Fetching comments for postId={}", postId);
        checkExistencePost(postId);

        List<Comment> comments = commentRepository.findCommentsByPostId(postId);
        log.debug("Found {} comments for postId={}", comments.size(), postId);
        return CommentMapper.toCommentDtoList(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponseDto getComment(long postId, long commentId) {
        log.debug("Fetching comment with id={} for postId={}", commentId, postId);
        checkExistencePost(postId);

        Comment comment = commentRepository.findCommentById(postId, commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Comment with id = %d for post id = %d was not found.".formatted(commentId, postId)));

        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(long postId, CommentRequestDto commentRequestDto) {
        log.info("Creating comment for postId={}", postId);
        checkExistencePost(postId);

        if (postId != commentRequestDto.postId()) {
            throw new CommentBadRequestException("Post id in the path and request body must match.");
        }

        Comment comment = commentRepository.createComment(postId, commentRequestDto.text());
        postRepository.incrementComments(postId);
        log.debug("Comment with id={} created for postId={}", comment.getId(), postId);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(long postId, long commentId, CommentRequestDto commentRequestDto) {

        if (postId != commentRequestDto.postId()) {
            throw new CommentBadRequestException("Post id in the path and request body must match.");
        }

        if (commentRequestDto.id() == null || commentRequestDto.id() != commentId) {
            throw new CommentBadRequestException("Comment id in the path and request body must match.");
        }

        log.info("Updating comment with id={} for postId={}", commentId, postId);
        checkExistencePost(postId);
        checkExistenceComment(postId, commentId);

        Comment comment = commentRepository.updateComment(postId, commentId, commentRequestDto.text());

        log.debug("Comment with id={} updated for postId={}", commentId, postId);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public void deleteComment(long postId, long commentId) {
        log.info("Deleting comment with id={} for postId={}", commentId, postId);
        commentRepository.deleteComment(postId, commentId);
        postRepository.decrementComments(postId);
    }

    private void checkExistencePost(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Post with id = %d was not found.".formatted(postId));
        }
    }

    private void checkExistenceComment(long postId, long commentId) {
        if (!commentRepository.existsById(postId, commentId)) {
            throw new CommentNotFoundException("Comment with id = %d for post with id = %d was not found."
                    .formatted(commentId, postId)
            );
        }
    }
}
