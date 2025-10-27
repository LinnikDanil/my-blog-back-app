package ru.practicum.blog.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.service.PostService;
import ru.practicum.blog.web.dto.PostRequestDto;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @GetMapping
    public PostsResponseDto getPosts(
            @RequestParam("search") @NotNull String search,
            @RequestParam("pageNumber") @Min(1) int pageNumber,
            @RequestParam("pageSize") @Min(1) int pageSize
    ) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public PostResponseDto getPost(@PathVariable("id") long id) {
        return postService.getPost(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponseDto createPost(@RequestBody @Valid PostRequestDto postRequestDto) {
        return postService.createPost(postRequestDto);
    }

    @PutMapping("/{id}")
    public PostResponseDto updatePost(@PathVariable("id") long id, @RequestBody @Valid PostRequestDto postRequestDto) {
        return postService.updatePost(id, postRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable("id") long id) {
        postService.deletePost(id);
    }

    @PostMapping("/{id}/likes")
    public int incrementLikes(@PathVariable("id") long id) {
        return postService.incrementLikes(id);
    }

    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void updateImage(@PathVariable("id") long id, @RequestPart("image") MultipartFile image) {
        postService.updateImage(id, image);
    }

    @GetMapping("/{id}/image")
    public byte[] getImage(@PathVariable("id") long id) {
        return postService.getImage(id);
    }
}
