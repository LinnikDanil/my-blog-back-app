package ru.practicum.blog.domain.exception;

public class PostDbException extends RuntimeException {
    public PostDbException(String message) {
        super(message);
    }
}
