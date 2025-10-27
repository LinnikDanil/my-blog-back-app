package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.domain.exception.PostBadRequestException;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.PostService;
import ru.practicum.blog.web.dto.PostRequestDto;
import ru.practicum.blog.web.dto.PostResponseDto;
import ru.practicum.blog.web.dto.PostsResponseDto;
import ru.practicum.blog.web.mapper.PostMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final String TAG_PREFIX = "#";
    private static final String TITLE_DELIMITER = " ";

    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public PostsResponseDto getPosts(String search, int pageNumber, int pageSize) {
        List<String> wordsForSearch = List.of(search.trim().split("\\s+"));

        Set<String> tags = new HashSet<>();

        StringJoiner titleJoiner = new StringJoiner(TITLE_DELIMITER);
        for (String word : wordsForSearch) {
            if (word.startsWith(TAG_PREFIX) && word.length() > TAG_PREFIX.length()) {
                tags.add(word.substring(TAG_PREFIX.length()).toLowerCase());
            } else {
                titleJoiner.add(word.toLowerCase());
            }
        }
        String titleSubstring = titleJoiner.toString();
        long offset = (long) (pageNumber - 1) * pageSize;

        List<Post> posts = postRepository.findPosts(
                tags,
                titleSubstring,
                pageSize,
                offset
        );

        long countPosts = postRepository.countPosts(tags, titleSubstring);
        int lastPage;
        if (countPosts == 0) {
            lastPage = 1;
        } else {
            lastPage = Math.toIntExact(Math.ceilDiv(countPosts, pageSize));
        }
        boolean hasPrev = pageNumber > 1;
        boolean hasNext = pageNumber < lastPage;

        if (lastPage < pageNumber) {
            throw new PostBadRequestException("Запрашиваемая страница превышает количество возможных страниц.");
        }

        if (posts.isEmpty()) {
            return new PostsResponseDto(Collections.emptyList(), hasPrev, hasNext, lastPage);
        }

        return PostMapper.toPostsResponseDto(posts, hasPrev, hasNext, lastPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDto getPost(long id) {
        Post post = postRepository.findPostById(id)
                .orElseThrow(() -> new PostNotFoundException("Пост с id = %d не найден".formatted(id)));

        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto) {
        Post post = postRepository.createPost(
                postRequestDto.title(),
                postRequestDto.text(),
                getNormalizedTags(postRequestDto.tags())
        );
        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public PostResponseDto updatePost(long id, PostRequestDto postRequestDto) {
        if (postRequestDto.id() != null && id != postRequestDto.id()) {
            throw new PostBadRequestException("id поста в переменной пути и теле запроса должны совпадать.");
        }

        checkExistencePost(id);
        List<String> updatedTagNames = getNormalizedTags(postRequestDto.tags());

        Post post = postRepository.updatePost(
                id,
                postRequestDto.title(),
                postRequestDto.text(),
                updatedTagNames
        );
        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        postRepository.deletePost(id);
    }

    @Override
    public int incrementLikes(long id) {
        return postRepository.incrementLikes(id);
    }

    @Override
    public void updateImage(long id, MultipartFile image) {
        checkExistencePost(id);

        if (image.isEmpty()) {
            throw new PostImageException("Картинка не может быть пустой");
        }

        try {
            boolean isUpdated = postRepository.updateImage(id, image.getBytes());
            if (!isUpdated) {
                throw new PostImageException("Ошибка обновления картинки");
            }
        } catch (IOException ex) {
            throw new PostImageException("Ошибка обновления картинки: " + ex.getMessage());
        }
    }

    @Override
    public byte[] getImage(long id) {
        checkExistencePost(id);
        return postRepository.getImage(id);
    }

    private List<String> getNormalizedTags(List<String> tags) {
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }

    private void checkExistencePost(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Пост с id = %d не найден".formatted(postId));
        }
    }
}
