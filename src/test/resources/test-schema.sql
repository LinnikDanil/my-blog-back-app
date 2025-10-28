DROP TABLE IF EXISTS post_tag;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS tag;

CREATE TABLE tag
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL UNIQUE CHECK (name = LOWER(name))
);

CREATE TABLE post
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

CREATE TABLE comment
(
    id         BIGSERIAL PRIMARY KEY,
    text       TEXT      NOT NULL,
    post_id    BIGINT    NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE post_tag
(
    post_id BIGINT NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);
