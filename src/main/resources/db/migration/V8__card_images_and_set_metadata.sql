ALTER TABLE pokemon_set ADD COLUMN external_set_id VARCHAR(120);
ALTER TABLE pokemon_set ADD COLUMN series VARCHAR(120);
ALTER TABLE pokemon_set ADD COLUMN release_date DATE;
ALTER TABLE pokemon_set ADD COLUMN last_synced_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE card ADD COLUMN external_image_large_url VARCHAR(1000);
UPDATE card SET external_image_large_url = external_image_url WHERE external_image_url IS NOT NULL;

CREATE UNIQUE INDEX uq_pokemon_set_language_external_id ON pokemon_set (language_market, external_set_id);
