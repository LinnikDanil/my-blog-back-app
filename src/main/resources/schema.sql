CREATE SCHEMA IF NOT EXISTS blog_app;

CREATE TABLE IF NOT EXISTS tag
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL UNIQUE CHECK (name = lower(name))
);

CREATE TABLE IF NOT EXISTS post
(
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(256) NOT NULL,
    text           TEXT         NOT NULL,
    likes_count    INTEGER      NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
    comments_count INTEGER      NOT NULL DEFAULT 0 CHECK (comments_count >= 0),
    image          BYTEA,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment
(
    id         BIGSERIAL PRIMARY KEY,
    text       TEXT      NOT NULL,
    post_id    BIGINT    NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_tag
(
    post_id BIGINT NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_comment_post_id ON comment (post_id);
