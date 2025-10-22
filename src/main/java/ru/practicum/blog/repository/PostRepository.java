package ru.practicum.blog.repository;

import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PostRepository {

    List<Long> findPostIds(
            Set<String> tags,
            String titleSubstring,
            int limit,
            long offset
    );

    List<Post> findPostsByIds(List<Long> postIds);

    Map<Long, List<Tag>> findTagsByPostIds(List<Long> postIds);

    long countPosts(Set<String> tags, String titleSubstring);
}
