CREATE TABLE auth_rate_limit_locks (id INTEGER PRIMARY KEY);
INSERT INTO auth_rate_limit_locks (id) SELECT generate_series(0, 63);
ALTER TABLE auth_rate_limits ADD COLUMN lock_id INTEGER;
UPDATE auth_rate_limits SET lock_id = get_byte(decode(bucket_key, 'hex'), 0) % 64;
ALTER TABLE auth_rate_limits ALTER COLUMN lock_id SET NOT NULL;
ALTER TABLE auth_rate_limits ADD CONSTRAINT fk_rate_limit_lock FOREIGN KEY (lock_id) REFERENCES auth_rate_limit_locks(id);
CREATE INDEX idx_rate_limit_lock_window ON auth_rate_limits(lock_id, window_start);
