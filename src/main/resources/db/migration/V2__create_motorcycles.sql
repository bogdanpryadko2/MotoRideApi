CREATE TABLE motorcycles (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT       REFERENCES categories (id) ON DELETE SET NULL,
    brand            VARCHAR(100) NOT NULL,
    model            VARCHAR(100) NOT NULL,
    engine_cc        INTEGER      NOT NULL,
    horsepower       INTEGER      NOT NULL,
    badge            VARCHAR(100),
    short_description VARCHAR(255),
    description      TEXT,
    image_url        VARCHAR(500),
    price            NUMERIC(12, 2),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_motorcycles_category ON motorcycles (category_id);
CREATE INDEX idx_motorcycles_horsepower ON motorcycles (horsepower DESC);