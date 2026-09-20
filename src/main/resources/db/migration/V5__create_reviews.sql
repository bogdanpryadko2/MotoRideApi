-- Отзывы о мотоциклах
CREATE TABLE reviews (
                         id            BIGSERIAL PRIMARY KEY,
                         motorcycle_id BIGINT NOT NULL REFERENCES motorcycles (id) ON DELETE CASCADE,
                         rating        INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         text          TEXT NOT NULL,
                         created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_motorcycle ON reviews (motorcycle_id);