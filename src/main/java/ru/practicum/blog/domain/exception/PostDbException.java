package ru.practicum.blog.domain.exception;

public class PostDbException extends RuntimeException {
    private String message;

    public PostDbException(String message) {
        super(message);
    }
}
