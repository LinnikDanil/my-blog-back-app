package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public PostsResponseDto getPosts(String search, int pageNumber, int pageSize) {
        log.debug("Searching posts with query='{}', pageNumber={}, pageSize={}", search, pageNumber, pageSize);
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
            throw new PostBadRequestException("Requested page exceeds the total number of pages.");
        }

        if (posts.isEmpty()) {
            log.debug("No posts found for query='{}'", search);
            return new PostsResponseDto(Collections.emptyList(), hasPrev, hasNext, lastPage);
        }

        log.debug("Found {} posts for query='{}'", posts.size(), search);
        return PostMapper.toPostsResponseDto(posts, hasPrev, hasNext, lastPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDto getPost(long id) {
        log.debug("Fetching post with id={}", id);
        Post post = postRepository.findPostById(id)
                .orElseThrow(() -> new PostNotFoundException("Post with id = %d was not found.".formatted(id)));

        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto) {
        log.info("Creating new post with title='{}'", postRequestDto.title());
        Post post = postRepository.createPost(
                postRequestDto.title(),
                postRequestDto.text(),
                getNormalizedTags(postRequestDto.tags())
        );
        log.debug("Post with id={} created", post.getId());
        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public PostResponseDto updatePost(long id, PostRequestDto postRequestDto) {
        if (postRequestDto.id() != null && id != postRequestDto.id()) {
            throw new PostBadRequestException("Post id in the path and request body must match.");
        }

        log.info("Updating post with id={}", id);
        checkExistencePost(id);
        List<String> updatedTagNames = getNormalizedTags(postRequestDto.tags());

        Post post = postRepository.updatePost(
                id,
                postRequestDto.title(),
                postRequestDto.text(),
                updatedTagNames
        );
        log.debug("Post with id={} successfully updated", id);
        return PostMapper.toPostResponseDto(post, post.getText());
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        log.info("Deleting post with id={}", id);
        postRepository.deletePost(id);
    }

    @Override
    @Transactional
    public int incrementLikes(long id) {
        log.info("Incrementing likes for post with id={}", id);
        return postRepository.incrementLikes(id);
    }

    @Override
    @Transactional
    public void updateImage(long id, MultipartFile image) {
        log.info("Updating image for post with id={}", id);
        checkExistencePost(id);

        if (image.isEmpty()) {
            throw new PostImageException("Image cannot be empty.");
        }

        try {
            boolean isUpdated = postRepository.updateImage(id, image.getBytes());
            if (!isUpdated) {
                log.warn("Image for post with id={} was not updated because repository returned empty result", id);
                throw new PostImageException("Failed to update image.");
            }
            log.debug("Image for post with id={} updated", id);
        } catch (IOException ex) {
            log.error("Failed to update image for post with id={}", id, ex);
            throw new PostImageException("Failed to update image: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImage(long id) {
        log.debug("Loading image for post with id={}", id);
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
            throw new PostNotFoundException("Post with id = %d was not found.".formatted(postId));
        }
    }
}
