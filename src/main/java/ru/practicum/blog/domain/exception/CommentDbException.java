package ru.practicum.blog.domain.exception;

public class CommentDbException extends RuntimeException {
    public CommentDbException(String message) {
        super(message);
    }
}
