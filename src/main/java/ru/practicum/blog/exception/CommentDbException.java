package ru.practicum.blog.exception;

public class CommentDbException extends RuntimeException {
    public CommentDbException(String message) {
        super(message);
    }
}
