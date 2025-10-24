package ru.practicum.blog.model.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;
import ru.practicum.blog.model.dto.PostResponseDto;
import ru.practicum.blog.model.dto.PostsResponseDto;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PostMapper {
    private static final int PREVIEW_LIMIT = 128;
    private static final String ELLIPSIS = "…";

    public static PostsResponseDto toPostsResponseDto(
            List<Post> posts,
            boolean hasPrev,
            boolean hasNext,
            int lastPage
    ) {
        List<PostResponseDto> postDtos = new ArrayList<>();
        for (Post post : posts) {
            String truncateText = truncateWithEllipsis(post.getText());
            postDtos.add(toPostResponseDto(post, truncateText));
        }

        return new PostsResponseDto(
                postDtos,
                hasPrev,
                hasNext,
                lastPage
        );
    }

    public static PostResponseDto toPostResponseDto(Post post, String text) {
        List<String> tagNames = post.getTags().stream().map(Tag::getName).toList();

        return new PostResponseDto(
                post.getId(),
                post.getTitle(),
                text,
                tagNames,
                post.getLikesCount(),
                post.getCommentsCount());
    }

    private static String truncateWithEllipsis(String text) {
        if (text.length() <= PREVIEW_LIMIT) {
            return text;
        } else {
            return text.substring(0, PREVIEW_LIMIT) + ELLIPSIS;
        }
    }
}
