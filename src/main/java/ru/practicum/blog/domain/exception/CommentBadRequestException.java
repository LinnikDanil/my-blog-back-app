package ru.practicum.blog.domain.exception;

public class CommentBadRequestException extends RuntimeException {
    public CommentBadRequestException(String message) {
        super(message);
    }
}
