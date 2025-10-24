package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.exception.PostNotFoundException;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.dto.PostRequestDto;
import ru.practicum.blog.model.dto.PostResponseDto;
import ru.practicum.blog.model.dto.PostsResponseDto;
import ru.practicum.blog.model.mapper.PostMapper;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.PostService;

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

        List<Long> postIds = postRepository.findPostIds(
                tags,
                titleSubstring,
                pageSize,
                offset
        );

        long countPosts = postRepository.countPosts(tags, titleSubstring);
        int lastPage = Math.toIntExact(Math.ceilDiv(countPosts, pageSize));
        boolean hasPrev = pageNumber > 1;
        boolean hasNext = pageNumber < lastPage;

        if (postIds.isEmpty()) {
            return new PostsResponseDto(Collections.emptyList(), hasPrev, hasNext, lastPage);
        }

        List<Post> posts = postRepository.findPostsByIds(postIds);

        return PostMapper.toPostsResponseDto(posts, hasPrev, hasNext, lastPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDto getPost(long id) {
        Post post = findPostById(id);

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
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException("Post with id %d not found".formatted(id));
        }
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

    private Post findPostById(Long postId) {
        return postRepository.findPostById(postId)
                .orElseThrow(() -> new PostNotFoundException("Пост с id = %d не найден".formatted(postId)));
    }

    private static List<String> getNormalizedTags(List<String> tags) {
        return tags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }
}
