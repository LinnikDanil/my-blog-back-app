package ru.practicum.blog.service;

import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.model.dto.PostRequestDto;
import ru.practicum.blog.model.dto.PostResponseDto;
import ru.practicum.blog.model.dto.PostsResponseDto;

public interface PostService {
    PostsResponseDto getPosts(String search, int pageNumber, int pageSize);

    PostResponseDto getPost(long id);

    PostResponseDto createPost(PostRequestDto postRequestDto);

    PostResponseDto updatePost(long id, PostRequestDto postRequestDto);

    void deletePost(long id);

    int incrementLikes(long id);

    void updateImage(long id, MultipartFile image);

    byte[] getImage(long id);
}
