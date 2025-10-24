package ru.practicum.blog.repository;

import jakarta.validation.constraints.NotBlank;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostRepository {

    List<Long> findPostIds(
            Set<String> tags,
            String titleSubstring,
            int limit,
            long offset
    );

    Optional<Post> findPostById(long id);

    List<Post> findPostsByIds(List<Long> postIds);

    long countPosts(Set<String> tags, String titleSubstring);

    Post createPost(String title, String text, List<String> tags);

    Post updatePost(long id,String title, String text, List<String> updatedTagNames);

    boolean existsById(Long id);
}
