CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    motorcycle_id BIGINT REFERENCES motorcycles(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_posts_page ON posts(created_at DESC, id DESC);
CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_posts_motorcycle ON posts(motorcycle_id);
CREATE TABLE post_comments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_page ON post_comments(post_id, created_at DESC, id DESC);
CREATE INDEX idx_comments_user ON post_comments(user_id);
CREATE TABLE reactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE,
    comment_id BIGINT REFERENCES post_comments(id) ON DELETE CASCADE,
    review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('LIKE','LOVE','HELPFUL')),
    CHECK (num_nonnulls(post_id,comment_id,review_id)=1),
    UNIQUE(user_id,post_id), UNIQUE(user_id,comment_id), UNIQUE(user_id,review_id)
);
CREATE INDEX idx_reactions_post ON reactions(post_id) WHERE post_id IS NOT NULL;
CREATE INDEX idx_reactions_comment ON reactions(comment_id) WHERE comment_id IS NOT NULL;
CREATE INDEX idx_reactions_review ON reactions(review_id) WHERE review_id IS NOT NULL;
