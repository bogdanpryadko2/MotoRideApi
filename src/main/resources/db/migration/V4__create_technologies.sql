-- Технологии мотоциклов и связь многие-ко-многим
CREATE TABLE technologies (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE motorcycle_technologies (
    motorcycle_id BIGINT NOT NULL REFERENCES motorcycles (id) ON DELETE CASCADE,
    technology_id BIGINT NOT NULL REFERENCES technologies (id) ON DELETE CASCADE,
    PRIMARY KEY (motorcycle_id, technology_id)
);