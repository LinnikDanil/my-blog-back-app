package ru.practicum.blog.domain.exception;

public class PostNotFoundException extends RuntimeException {
    private String message;

    public PostNotFoundException(String message) {
        super(message);
    }
}
