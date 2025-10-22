package ru.practicum.blog.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;
import ru.practicum.blog.model.dto.PostRequestDto;
import ru.practicum.blog.model.dto.PostResponseDto;
import ru.practicum.blog.model.dto.PostsResponseDto;
import ru.practicum.blog.model.mapper.PostMapper;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.service.PostService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        long offset = (pageNumber - 1) * pageSize;

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
        Map<Long, List<Tag>> tagsPost = postRepository.findTagsByPostIds(postIds);

        return PostMapper.toPostsResponseDto(posts, tagsPost, hasPrev, hasNext, lastPage);
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
