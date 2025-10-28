package ru.practicum.blog.domain.exception;

public class PostBadRequestException extends RuntimeException {
    public PostBadRequestException(String message) {
        super(message);
    }
}
