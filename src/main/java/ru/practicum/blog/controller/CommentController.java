package ru.practicum.blog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.blog.model.dto.CommentRequestDto;
import ru.practicum.blog.model.dto.CommentResponseDto;
import ru.practicum.blog.service.CommentService;

import java.util.Set;

@RestController
@RequestMapping("api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Set<CommentResponseDto> getComments(
            @PathVariable("postId") long postId
    ) {
        return commentService.getComments(postId);
    }

    @GetMapping("/{commentId}")
    public CommentResponseDto getComment(
            @PathVariable("postId") long postId,
            @PathVariable("commentId") long commentId
    ) {
        return commentService.getComment(postId, commentId);
    }

    @PostMapping
    public CommentResponseDto createComment(
            @PathVariable("postId") long postId,
            @RequestBody @Valid CommentRequestDto commentRequestDto
    ) {
        return commentService.createComment(postId, commentRequestDto);
    }

    @PutMapping("/{commentId}")
    public CommentResponseDto updateComment(
            @PathVariable("postId") long postId,
            @PathVariable("commentId") long commentId,
            @RequestBody @Valid CommentRequestDto commentRequestDto
    ) {
        return commentService.updateComment(postId, commentId, commentRequestDto);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable("postId") long postId,
            @PathVariable("commentId") long commentId
    ) {
        commentService.deleteComment(postId, commentId);
    }
}
