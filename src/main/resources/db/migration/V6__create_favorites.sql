-- Избранные мотоциклы
CREATE TABLE favorites (
                           id            BIGSERIAL PRIMARY KEY,
                           motorcycle_id BIGINT NOT NULL REFERENCES motorcycles (id) ON DELETE CASCADE,
                           created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                           UNIQUE (motorcycle_id)
);