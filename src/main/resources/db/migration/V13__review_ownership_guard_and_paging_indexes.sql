-- Existing ownerless reviews are explicitly historic; new inserts default to non-legacy.
ALTER TABLE reviews ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE reviews SET legacy = TRUE WHERE user_id IS NULL;
ALTER TABLE reviews ADD CONSTRAINT reviews_owner_or_legacy CHECK (user_id IS NOT NULL OR legacy);
COMMENT ON COLUMN reviews.legacy IS 'Only pre-user historic rows may omit an owner; not writable through API';
CREATE INDEX idx_motorcycles_price_page ON motorcycles(price, id);
CREATE INDEX idx_motorcycles_created_page ON motorcycles(created_at DESC, id);
CREATE INDEX idx_news_page ON news(published_at DESC, id DESC);
