package ru.practicum.blog.repository;

import ru.practicum.blog.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    List<Comment> findCommentsByPostId(long postId);

    Optional<Comment> findCommentById(long postId, long commentId);

    Comment createComment(long postId, String text);

    boolean existsById(long postId, long commentId);

    Comment updateComment(long postId, long commentId, String text);

    void deleteComment(long postId, long commentId);
}
