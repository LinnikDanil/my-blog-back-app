package ru.practicum.blog.repository.sql;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqlConstants {

    // === POST ===
    public static final String FIND_POSTS_BY_IDS = """
            SELECT id, title, text, likes_count, comments_count
            FROM post
            WHERE id IN (:postIds)
            ORDER BY created_at DESC, id DESC
            """;

    public static final String FIND_POST_BY_ID = """
            SELECT id, title, text, likes_count, comments_count
            FROM post
            WHERE id = :postId
            """;

    public static final String CREATE_POST =
            "INSERT INTO post (title, text) VALUES(:title, :text) RETURNING id";

    public static final String UPDATE_POST = """
            UPDATE post
            SET title = :title,
                text = :text,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :postId
            """;

    public static final String DELETE_POST =
            "DELETE FROM post WHERE id = :id";

    public static final String EXISTS_BY_ID =
            "SELECT EXISTS(SELECT 1 FROM post WHERE id = :id)";

    public static final String INCREMENT_LIKES = """
            UPDATE post
            SET likes_count = (likes_count + 1),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :postId
            RETURNING likes_count
            """;

    public static final String UPDATE_IMAGE =
            "UPDATE post SET image = :image WHERE id = :id";

    public static final String GET_IMAGE =
            "SELECT image FROM post WHERE id = :id";

    public static final String INCREMENT_COMMENTS = """
            UPDATE post
            SET comments_count = (comments_count + 1),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :postId
            """;

    public static final String DECREMENT_COMMENTS = """
            UPDATE post
            SET comments_count = (comments_count - 1),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :postId
            """;

    // === TAG ===
    public static final String INSERT_TAG =
            "INSERT INTO tag (name) VALUES(:name) ON CONFLICT (name) DO NOTHING";

    public static final String FIND_TAG_IDS_BY_NAMES =
            "SELECT id FROM tag WHERE name IN (:names)";

    public static final String DELETE_UNUSED_TAGS =
            "DELETE FROM post_tag WHERE post_id = :postId AND tag_id NOT IN (:tagIds)";

    public static final String CLEANUP_UNUSED_TAGS = """
            DELETE FROM tag t
            WHERE NOT EXISTS (
              SELECT 1 FROM post_tag pt
              WHERE pt.tag_id = t.id
            )
            """;

    // === POST_TAG ===
    public static final String INSERT_POST_TAG =
            "INSERT INTO post_tag (post_id, tag_id) VALUES(:postId, :tagId) ON CONFLICT DO NOTHING";

    public static final String DELETE_POST_TAGS =
            "DELETE FROM post_tag WHERE post_id = :postId";

    public static final String FIND_TAGS_BY_POST_ID = """
            SELECT pt.post_id, t.id, t.name
            FROM tag t
            JOIN post_tag pt ON t.id = pt.tag_id
            WHERE pt.post_id = :postId
            """;

    public static final String FIND_TAGS_BY_POST_IDS = """
            SELECT pt.post_id, t.id, t.name
            FROM tag t
            JOIN post_tag pt ON t.id = pt.tag_id
            WHERE pt.post_id IN (:postIds)
            """;

    // === FILTERING & COUNT ===
    public static final String COUNT_POSTS_NO_TAGS = """
            SELECT COUNT(*)
            FROM post
            WHERE :title = '' OR LOWER(title) LIKE CONCAT('%', :title, '%')
            """;

    public static final String COUNT_POSTS_WITH_TAGS = """
            SELECT COUNT(*)
            FROM post
            WHERE (:title = '' OR LOWER(title) LIKE CONCAT('%', :title, '%'))
              AND id IN (
                  SELECT pt.post_id
                  FROM post_tag pt
                  JOIN tag t ON t.id = pt.tag_id
                  WHERE t.name IN (:tags)
                  GROUP BY pt.post_id
                  HAVING COUNT(t.name) = :tagsCount
              )
            """;

    public static final String FIND_POST_IDS_NO_TAGS = """
            SELECT id
            FROM post
            WHERE :title = '' OR LOWER(title) LIKE CONCAT('%', :title, '%')
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    public static final String FIND_POST_IDS_WITH_TAGS = """
            SELECT id
            FROM post
            WHERE (:title = '' OR LOWER(title) LIKE CONCAT('%', :title, '%'))
              AND id IN (
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

    // === COMMENTS ===
    public static final String FIND_COMMENTS_BY_POST_ID =
            "SELECT id, text, post_id FROM comment WHERE post_id = :postId ORDER BY created_at DESC, id DESC";

    public static final String FIND_COMMENT_BY_ID =
            "SELECT id, text, post_id FROM comment WHERE id = :commentId AND post_id = :postId";

    public static final String CREATE_COMMENT =
            "INSERT INTO comment (text, post_id) VALUES(:text, :postId) RETURNING id, text, post_id";

    public static final String UPDATE_COMMENT = """
            UPDATE comment
            SET text = :text,
            updated_at = CURRENT_TIMESTAMP
            WHERE id = :commentId
                AND post_id = :postId
            RETURNING id, text, post_id
            """;

    public static final String DELETE_COMMENT =
            "DELETE FROM comment WHERE post_id = :postId AND id = :commentId";

    public static final String EXISTS_COMMENT =
            "SELECT EXISTS(SELECT 1 FROM comment WHERE id = :commentId AND post_id = :postId)";
}
