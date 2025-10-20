package ru.practicum.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.model.dto.PostRequestDto;
import ru.practicum.blog.model.dto.PostResponseDto;
import ru.practicum.blog.model.dto.PostsResponseDto;
import ru.practicum.blog.service.PostService;

@Service
public class PostServiceImpl implements PostService {
    @Override
    public PostsResponseDto getPosts(String search, int pageNumber, int pageSize) {
        return null;
    }

    @Override
    public PostResponseDto getPost(long id) {
        return null;
    }

    @Override
    public PostResponseDto createPost(PostRequestDto postRequestDto) {
        return null;
    }

    @Override
    public PostResponseDto updatePost(long id, PostRequestDto postRequestDto) {
        return null;
    }

    @Override
    public void deletePost(long id) {

    }

    @Override
    public int incrementLikes(long id) {
        return 0;
    }

    @Override
    public void updateImage(long id, MultipartFile image) {

    }

    @Override
    public byte[] getImage(long id) {
        return new byte[0];
    }
}
