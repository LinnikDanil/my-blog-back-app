package ru.practicum.blog.repository.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.domain.exception.PostDbException;
import ru.practicum.blog.domain.exception.PostImageException;
import ru.practicum.blog.domain.exception.PostNotFoundException;
import ru.practicum.blog.domain.model.Post;
import ru.practicum.blog.domain.model.Tag;
import ru.practicum.blog.repository.PostRepository;
import ru.practicum.blog.repository.util.SqlConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JdbcPostRepositoryImpl implements PostRepository {

    private static final Logger log = LogManager.getLogger(JdbcPostRepositoryImpl.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Post> findPosts(Set<String> tags, String titleSubstring, int pageSize, long offset) {
        List<Long> postIds = findPostIds(tags, titleSubstring, pageSize, offset);

        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Post> posts = jdbcTemplate.query(
                SqlConstants.FIND_POSTS_BY_IDS,
                Map.of("postIds", postIds),
                (resultSet, rowNum) -> Post.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .text(resultSet.getString("text"))
                        .likesCount(resultSet.getInt("likes_count"))
                        .commentsCount(resultSet.getInt("comments_count"))
                        .build()
        );

        Map<Long, List<Tag>> tagsPost = findTagsByPostIds(postIds);

        for (Post post : posts) {
            List<Tag> tagsForPosts = Optional.ofNullable(tagsPost.get(post.getId())).orElse(Collections.emptyList());
            post.setTags(tagsForPosts);
        }

        return posts;
    }

    @Override
    public Optional<Post> findPostById(long id) {
        Post post = jdbcTemplate.query(
                SqlConstants.FIND_POST_BY_ID,
                Map.of("postId", id),
                (resultSet, rowNum) -> Post.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .text(resultSet.getString("text"))
                        .likesCount(resultSet.getInt("likes_count"))
                        .commentsCount(resultSet.getInt("comments_count"))
                        .build()
        ).stream().findFirst().orElse(null);

        if (post == null) {
            return Optional.empty();
        }

        List<Tag> tags = findTagsByPostId(id);
        post.setTags(tags);

        return Optional.of(post);
    }

    @Override
    public Post createPost(String title, String text, List<String> tagNames) {
        // Сохраняем новый пост
        Long postId = jdbcTemplate.queryForObject(
                SqlConstants.CREATE_POST,
                Map.of("title", title, "text", text),
                Long.class
        );

        if (postId == null) {
            throw new PostDbException("Failed to create post.");
        }

        if (!tagNames.isEmpty()) {
            // Сохраняем новые теги
            insertBatchTags(tagNames);

            // Получаем id всех тегов нового поста
            List<Long> tagIds = findTagIdsByNames(tagNames);

            // Сохраняем все связи пост тег
            insertPostIdTagIds(tagIds, postId);
        }

        return findPostById(postId).orElseThrow(() -> new PostDbException("Failed to create post."));
    }

    @Override
    public Post updatePost(long postId, String title, String text, List<String> updatedTagNames) {
        MapSqlParameterSource parameterSourceForUpdatePost = new MapSqlParameterSource(
                Map.of("title", title, "text", text, "postId", postId));
        //Обновляем сам пост
        jdbcTemplate.update(SqlConstants.UPDATE_POST, parameterSourceForUpdatePost);

        // Если в обновлённом посте тегов нет, то очищаем все теги поста
        if (updatedTagNames.isEmpty()) {
            deleteTagsForPost(postId);
        } else {
            // Если в обновлённом посте есть теги, то сохраняем новые теги (старые просто проигнорируются)
            insertBatchTags(updatedTagNames);

            // Получаем tagIds всех новых тегов
            List<Long> updatedTagIds = findTagIdsByNames(updatedTagNames);

            // Удаляем неиспользуемые теги
            jdbcTemplate.update(
                    SqlConstants.DELETE_UNUSED_TAGS,
                    Map.of("postId", postId, "tagIds", updatedTagIds)
            );

            // Добавляем связи для новых тегов в обновлённом посте
            insertPostIdTagIds(updatedTagIds, postId);
        }

        return findPostById(postId).orElseThrow(() -> new PostDbException("Failed to update post."));
    }

    @Override
    public void deletePost(long id) {
        int deleted = jdbcTemplate.update(SqlConstants.DELETE_POST, Map.of("id", id));
        if (deleted == 0) {
            throw new PostNotFoundException("Пост с id = %d не существует.".formatted(id));
        }
        deleteTagsForPost(id);
    }

    @Override
    public long countPosts(Set<String> tags, String titleSubstring) {
        int tagsCount = tags.size();
        var params = new MapSqlParameterSource()
                .addValue("title", titleSubstring);
        String sql;
        if (tagsCount == 0) {
            sql = SqlConstants.COUNT_POSTS_NO_TAGS;
        } else {
            sql = SqlConstants.COUNT_POSTS_WITH_TAGS;
            params.addValue("tags", tags);
            params.addValue("tagsCount", tagsCount);
        }

        Long countPosts = jdbcTemplate.queryForObject(sql, params, Long.class);
        return countPosts != null && countPosts > 0 ? countPosts : 0;
    }

    @Override
    public boolean existsById(long id) {
        Boolean postExists = jdbcTemplate.queryForObject(SqlConstants.EXISTS_BY_ID, Map.of("id", id), Boolean.class);
        return Boolean.TRUE.equals(postExists);
    }

    @Override
    public int incrementLikes(long id) {
        return jdbcTemplate.query(
                        SqlConstants.INCREMENT_LIKES,
                        Map.of("postId", id),
                        (rs, rn) -> rs.getInt("likes_count")
                ).stream()
                .findFirst()
                .orElseThrow(() -> new PostNotFoundException("Post with id = %d does not exist.".formatted(id)));
    }

    @Override
    public boolean updateImage(long id, byte[] image) {
        int updated = jdbcTemplate.update(SqlConstants.UPDATE_IMAGE, Map.of("image", image, "id", id));
        return updated > 0;
    }

    @Override
    public byte[] getImage(long id) {
        return jdbcTemplate.query(
                        SqlConstants.GET_IMAGE,
                        Map.of("id", id),
                        (rs, rn) -> rs.getBytes("image")
                ).stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new PostImageException("Image for post with id = %d is not available.".formatted(id)));
    }

    @Override
    public void incrementComments(long postId) {
        jdbcTemplate.update(SqlConstants.INCREMENT_COMMENTS, Map.of("postId", postId));
    }

    @Override
    public void decrementComments(long postId) {
        jdbcTemplate.update(SqlConstants.DECREMENT_COMMENTS, Map.of("postId", postId));
    }

    private List<Long> findPostIds(
            Set<String> tags,
            String titleSubstring,
            int limit,
            long offset
    ) {
        int tagsCount = tags.size();
        var params = new MapSqlParameterSource()
                .addValue("title", titleSubstring)
                .addValue("limit", limit)
                .addValue("offset", offset);
        String sql;
        if (tagsCount == 0) {
            sql = SqlConstants.FIND_POST_IDS_NO_TAGS;
        } else {
            sql = SqlConstants.FIND_POST_IDS_WITH_TAGS;
            params.addValue("tags", tags);
            params.addValue("tagsCount", tagsCount);
        }

        return jdbcTemplate.query(
                sql,
                params,
                (resultSet, rowNum) -> resultSet.getLong("id")
        );
    }

    private List<Tag> findTagsByPostId(long id) {
        List<Tag> tags = jdbcTemplate.query(
                SqlConstants.FIND_TAGS_BY_POST_ID,
                Map.of("postId", id),
                (resultSet, rowNum) -> Tag.builder()
                        .id(resultSet.getLong("id"))
                        .name(resultSet.getString("name"))
                        .build()
        );

        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags;
    }

    private Map<Long, List<Tag>> findTagsByPostIds(List<Long> postIds) {
        return jdbcTemplate.query(
                SqlConstants.FIND_TAGS_BY_POST_IDS,
                Map.of("postIds", postIds),
                resultSet -> {
                    Map<Long, List<Tag>> result = new HashMap<>();

                    while (resultSet.next()) {
                        long postId = resultSet.getLong("post_id");
                        Tag tag = Tag.builder()
                                .id(resultSet.getLong("id"))
                                .name(resultSet.getString("name"))
                                .build();

                        result.computeIfAbsent(postId, k -> new ArrayList<>()).add(tag);
                    }
                    return result;
                }
        );
    }

    private void insertBatchTags(List<String> tagNames) {
        SqlParameterSource[] tagsForBatch = SqlParameterSourceUtils.createBatch(
                tagNames.stream()
                        .map(tag -> Map.<String, Object>of("name", tag))
                        .map(Map.class::cast)
                        .toArray()
        );
        // Сохраняем все новые теги в таблицу тегов
        jdbcTemplate.batchUpdate(SqlConstants.INSERT_TAG, tagsForBatch);
    }

    private List<Long> findTagIdsByNames(List<String> tagNames) {
        return jdbcTemplate.query(
                SqlConstants.FIND_TAG_IDS_BY_NAMES,
                Map.of("names", tagNames),
                (resultSet, rowNum) -> resultSet.getLong("id")
        );
    }

    private void insertPostIdTagIds(List<Long> tagIds, long postId) {
        SqlParameterSource[] postTagForBatch = SqlParameterSourceUtils.createBatch(
                tagIds.stream()
                        .map(tagId -> Map.<String, Object>of("postId", postId, "tagId", tagId))
                        .map(Map.class::cast)
                        .toArray()
        );
        jdbcTemplate.batchUpdate(SqlConstants.INSERT_POST_TAG, postTagForBatch);
    }

    private void deleteTagsForPost(long postId) {
        jdbcTemplate.update(
                SqlConstants.DELETE_POST_TAGS,
                Map.of("postId", postId)
        );
    }

    @Scheduled(cron = "0 0 0 * * 1") // каждый понедельник в 00:00
    private void cleanupUnusedTags() {
        log.info("Starting scheduled cleanup of unused tags");
        int deletedTags = jdbcTemplate.update(SqlConstants.CLEANUP_UNUSED_TAGS, Map.of());

        if (deletedTags > 0) {
            log.info("Cleanup finished, removed {} unused tags", deletedTags);
        } else {
            log.debug("Cleanup finished, no unused tags were removed");
        }
    }
}
