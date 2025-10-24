package ru.practicum.blog.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.exception.PostDbException;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;

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

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Long> findPostIds(
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
            sql = """
                    SELECT p.id
                    FROM post p
                    WHERE :title = '' OR LOWER(p.title) LIKE CONCAT('%',:title,'%')
                    ORDER BY created_at DESC, id DESC 
                    LIMIT :limit OFFSET :offset
                    """;
        } else {
            sql = """
                    SELECT p.id
                    FROM post p
                    WHERE (:title = '' OR LOWER(p.title) LIKE CONCAT('%',:title,'%'))
                        AND p.id IN (
                            SELECT pt.post_id
                            FROM post_tag pt
                            JOIN tag t ON t.id = pt.tag_id
                            WHERE t.name IN (:tags)
                            GROUP BY pt.post_id
                            HAVING COUNT(t.name) = :tagsCount
                        )
                    ORDER BY created_at DESC, id DESC
                    LIMIT :limit OFFSET :offset
                    """;
            params.addValue("tags", tags);
            params.addValue("tagsCount", tagsCount);
        }

        return jdbcTemplate.query(
                sql,
                params,
                (resultSet, rowNum) -> resultSet.getLong("id")
        );
    }

    @Override
    public Optional<Post> findPostById(long id) {
        String sql = """
                SELECT id, title, text, likes_count, comments_count
                FROM post
                WHERE id = :postId
                """;

        Post post = jdbcTemplate.query(
                sql,
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
    public List<Post> findPostsByIds(List<Long> postIds) {
        String sql = """
                SELECT id, title, text, likes_count, comments_count
                FROM post p
                WHERE id IN (:postIds)
                ORDER BY created_at DESC, id DESC
                """;

        List<Post> posts = jdbcTemplate.query(
                sql,
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
            List<Tag> tags = Optional.ofNullable(tagsPost.get(post.getId())).orElse(Collections.emptyList());
            post.setTags(tags);
        }

        return posts;
    }

    @Override
    public Post createPost(String title, String text, List<String> tagNames) {
        String sqlInsertPost = "INSERT INTO post (title, text) VALUES(:title, :text) RETURNING id";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        // Сохраняем новый пост
        jdbcTemplate.update(
                sqlInsertPost,
                new MapSqlParameterSource(Map.of("title", title, "text", text)),
                keyHolder,
                new String[]{"id"}
        );
        long postId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        // Сохраняем новые тэги
        insertBatchTags(tagNames);

        // Получаем id всех тегов нового поста
        List<Long> tagIds = findTagIdsByNames(tagNames);

        // Сохраняем все связи пост тег
        insertPostIdTagIds(tagIds, postId);

        return findPostById(postId).orElseThrow(() -> new PostDbException("Ошибка при создании поста"));
    }

    @Override
    public Post updatePost(long postId, String title, String text, List<String> updatedTagNames) {
        String sqlUpdatePost = """
                UPDATE post
                SET title = :title,
                text = :text,
                updated_at = CURRENT_TIMESTAMP
                WHERE id = :postId
                """;

        MapSqlParameterSource parameterSourceForUpdatePost = new MapSqlParameterSource(
                Map.of("title", title, "text", text, "postId", postId));
        //Обновляем сам пост
        jdbcTemplate.update(sqlUpdatePost, parameterSourceForUpdatePost);

        // Если в обновлённом посте тегов нет, то очищаем все теги поста
        if (updatedTagNames.isEmpty()) {
            jdbcTemplate.update(
                    "DELETE FROM post_tag WHERE post_id = :postId",
                    Map.of("postId", postId)
            );
        } else {
            // Если в обновлённом посте есть теги, то сохраняем новые теги (старые просто проигнорируются)
            insertBatchTags(updatedTagNames);

            // Получаем tagIds всех новых тегов
            List<Long> updatedTagIds = findTagIdsByNames(updatedTagNames);

            // Удаляем неиспользуемые теги
            jdbcTemplate.update(
                    "DELETE FROM post_tag WHERE post_id = :postId AND tag_id NOT IN (:tagIds)",
                    Map.of("postId", postId, "tagIds", updatedTagIds)
            );

            // Добавляем связи для новых тегов в обновлённом посте
            insertPostIdTagIds(updatedTagIds, postId);
        }

        return findPostById(postId).orElseThrow(() -> new PostDbException("Ошибка при обновлении поста"));
    }

    @Override
    public long countPosts(Set<String> tags, String titleSubstring) {
        int tagsCount = tags.size();
        var params = new MapSqlParameterSource()
                .addValue("title", titleSubstring);
        String sql;
        if (tagsCount == 0) {
            sql = """
                    SELECT COUNT(*)
                    FROM post p
                    WHERE :title = '' OR LOWER(p.title) LIKE CONCAT('%',:title,'%')
                    """;
        } else {
            sql = """
                    SELECT COUNT(*)
                    FROM post p
                    WHERE (:title = '' OR LOWER(p.title) LIKE CONCAT('%',:title,'%'))
                        AND p.id IN (
                            SELECT pt.post_id
                            FROM post_tag pt
                            JOIN tag t ON t.id = pt.tag_id
                            WHERE t.name IN (:tags)
                            GROUP BY pt.post_id
                            HAVING COUNT(t.name) = :tagsCount
                        )
                    """;
            params.addValue("tags", tags);
            params.addValue("tagsCount", tagsCount);
        }

        Long countPosts = jdbcTemplate.queryForObject(sql, params, Long.class);
        return countPosts != null && countPosts > 0 ? countPosts : 0;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM post WHERE id = :id";
        Integer userCount = jdbcTemplate.queryForObject(sql, Map.of("id", id), Integer.class);
        return userCount != null && userCount > 0;
    }

    private List<Tag> findTagsByPostId(long id) {
        String sql = """
                SELECT pt.post_id, t.id, t.name
                FROM tag t
                JOIN post_tag pt ON t.id = pt.tag_id
                WHERE pt.post_id = :postIds
                """;

        List<Tag> tags = jdbcTemplate.query(
                sql,
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
        String sql = """
                SELECT pt.post_id, t.id, t.name
                FROM tag t
                JOIN post_tag pt ON t.id = pt.tag_id
                WHERE pt.post_id IN (:postIds)
                """;

        return jdbcTemplate.query(
                sql,
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
        String sqlInsertTag = "INSERT INTO tag (name) VALUES(:name) ON CONFLICT (name) DO NOTHING";

        SqlParameterSource[] tagsForBatch = SqlParameterSourceUtils.createBatch(
                tagNames.stream()
                        .map(tag -> Map.<String, Object>of("name", tag))
                        .map(Map.class::cast)
                        .toArray()
        );
        // Сохраняем все новые теги в таблицу тегов
        jdbcTemplate.batchUpdate(sqlInsertTag, tagsForBatch);
    }

    private List<Long> findTagIdsByNames(List<String> tagNames) {
        String sqlFindTagIds = "SELECT id FROM tag WHERE name IN (:names)";

        return jdbcTemplate.query(
                sqlFindTagIds,
                Map.of("names", tagNames),
                (resultSet, rowNum) -> resultSet.getLong("id")
        );
    }

    private void insertPostIdTagIds(List<Long> tagIds, long postId) {
        String sqlInsertToPostTag = "INSERT INTO post_tag (post_id, tag_id) VALUES(:postId, :tagId) ON CONFLICT DO NOTHING";

        SqlParameterSource[] postTagForBatch = SqlParameterSourceUtils.createBatch(
                tagIds.stream()
                        .map(tagId -> Map.<String, Object>of("postId", postId, "tagId", tagId))
                        .map(Map.class::cast)
                        .toArray()
        );
        jdbcTemplate.batchUpdate(sqlInsertToPostTag, postTagForBatch);
    }
}
