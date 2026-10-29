-- Keep unowned historic reviews publicly readable, editable only by ADMIN.
ALTER TABLE reviews ADD COLUMN user_id BIGINT REFERENCES users(id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
CREATE INDEX idx_reviews_page ON reviews(motorcycle_id, created_at DESC, id DESC);
-- Archive the original global list without assigning it to any real person.
ALTER TABLE favorites RENAME TO legacy_favorites;
COMMENT ON TABLE legacy_favorites IS 'Pre-user global favorites retained for manual administrative export; never exposed by personal API';
ALTER TABLE legacy_favorites RENAME CONSTRAINT favorites_pkey TO legacy_favorites_pkey;
ALTER SEQUENCE favorites_id_seq RENAME TO legacy_favorites_id_seq;
ALTER TABLE legacy_favorites ADD COLUMN motorcycle_snapshot JSONB;
UPDATE legacy_favorites f SET motorcycle_snapshot = to_jsonb(m) FROM motorcycles m WHERE m.id = f.motorcycle_id;
ALTER TABLE legacy_favorites DROP CONSTRAINT favorites_motorcycle_id_fkey;
CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    motorcycle_id BIGINT NOT NULL REFERENCES motorcycles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT favorites_user_motorcycle_key UNIQUE(user_id, motorcycle_id)
);
CREATE INDEX idx_favorites_page ON favorites(user_id, created_at DESC, id DESC);
