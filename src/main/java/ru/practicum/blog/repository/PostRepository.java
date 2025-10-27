package ru.practicum.blog.repository;

import ru.practicum.blog.domain.model.Post;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostRepository {

    List<Post> findPosts(Set<String> tags, String titleSubstring, int pageSize, long offset);

    Optional<Post> findPostById(long id);

    long countPosts(Set<String> tags, String titleSubstring);

    Post createPost(String title, String text, List<String> tags);

    Post updatePost(long id, String title, String text, List<String> updatedTagNames);

    void deletePost(long id);

    boolean existsById(long id);

    int incrementLikes(long id);

    boolean updateImage(long id, byte[] image);

    byte[] getImage(long id);

    void incrementComments(long postId);

    void decrementComments(long postId);
}
