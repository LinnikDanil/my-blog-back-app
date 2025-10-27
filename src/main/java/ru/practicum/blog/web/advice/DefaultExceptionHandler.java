package ru.practicum.blog.web.advice;

import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.blog.domain.exception.CommentBadRequestException;
import ru.practicum.blog.domain.exception.CommentDbException;
import ru.practicum.blog.domain.exception.CommentNotFoundException;
import ru.practicum.blog.domain.exception.PostBadRequestException;
import ru.practicum.blog.domain.exception.PostDbException;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.web.advice.model.ErrorResponse;
import ru.practicum.blog.web.controller.CommentController;
import ru.practicum.blog.web.controller.PostController;

@RestControllerAdvice(assignableTypes = {PostController.class, CommentController.class})
public class DefaultExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse exception(Exception e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validationException(ValidationException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse postBadRequestException(PostBadRequestException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse postImageException(PostImageException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse postNotFoundException(PostNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse postDbException(PostDbException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse commentBadRequestException(CommentBadRequestException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse commentNotFoundException(CommentNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse commentDbException(CommentDbException e) {
        return new ErrorResponse(e.getMessage());
    }
}
