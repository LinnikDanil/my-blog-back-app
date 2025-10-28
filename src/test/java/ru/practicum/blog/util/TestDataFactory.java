package ru.practicum.blog.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.blog.domain.model.Comment;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.domain.model.Tag;
import ru.practicum.blog.web.dto.CommentRequestDto;
import ru.practicum.blog.web.dto.PostRequestDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Post createPost(
            long id,
            String title,
            String text,
            List<String> tagNames,
            int likes,
            int comments
    ) {
        List<Tag> tags = tagNames.stream()
                .map(name -> Tag.builder().id((long) name.hashCode()).name(name).build())
                .collect(Collectors.toList());

        return Post.builder()
                .id(id)
                .title(title)
                .text(text)
                .likesCount(likes)
                .commentsCount(comments)
                .tags(tags)
                .build();
    }

    public static PostRequestDto createPostRequestDto(Long id, String title, String text, List<String> tags) {
        return new PostRequestDto(id, title, text, tags);
    }

    public static Comment createComment(long id, String text, long postId) {
        return Comment.builder()
                .id(id)
                .text(text)
                .postId(postId)
                .build();
    }

    public static CommentRequestDto createCommentRequestDto(Long id, String text, Long postId) {
        return new CommentRequestDto(id, text, postId);
    }

    public static MultipartFile createMultipartFile(String name, byte[] content) {
        return new MockMultipartFile(name, name, "image/jpeg", content);
    }

    public static MultipartFile createEmptyMultipartFile(String name) {
        return new MockMultipartFile(name, name, "image/jpeg", new byte[0]);
    }

    public static byte[] stringAsBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
