package ru.practicum.blog.exception;

public class CommentBadRequestException extends RuntimeException {
    public CommentBadRequestException(String message) {
        super(message);
    }
}
