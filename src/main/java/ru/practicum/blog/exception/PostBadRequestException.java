package ru.practicum.blog.exception;

public class PostBadRequestException extends RuntimeException {
    public PostBadRequestException(String message) {
        super(message);
    }
}
