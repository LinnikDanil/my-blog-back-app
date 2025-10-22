package ru.practicum.blog.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.practicum.blog.model.Post;
import ru.practicum.blog.model.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                            WHERE LOWER(t.name) IN (:tags)
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
    public List<Post> findPostsByIds(List<Long> postIds) {
        String sql = """
                SELECT id, title, text, likes_count, comments_count
                FROM post p
                WHERE id IN (:postIds)
                ORDER BY created_at DESC, id DESC
                """;

        return jdbcTemplate.query(
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
    }

    @Override
    public Map<Long, List<Tag>> findTagsByPostIds(List<Long> postIds) {
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
                            WHERE LOWER(t.name) IN (:tags)
                            GROUP BY pt.post_id
                            HAVING COUNT(t.name) = :tagsCount
                        )
                    """;
            params.addValue("tags", tags);
            params.addValue("tagsCount", tagsCount);
        }

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }
}
