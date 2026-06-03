ALTER TABLE card ADD COLUMN catalog_source VARCHAR(80) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE card ADD COLUMN verification_status VARCHAR(80) NOT NULL DEFAULT 'UNVERIFIED';
ALTER TABLE card ADD COLUMN external_card_id VARCHAR(120);
ALTER TABLE card ADD COLUMN external_image_url VARCHAR(1000);
ALTER TABLE card ADD COLUMN external_card_url VARCHAR(1000);
ALTER TABLE card ADD COLUMN rarity VARCHAR(120);
ALTER TABLE card ADD COLUMN last_synced_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE card ADD CONSTRAINT chk_card_catalog_source CHECK (catalog_source IN ('MANUAL', 'POKEMON_TCG_API'));
ALTER TABLE card ADD CONSTRAINT chk_card_verification_status CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED'));

CREATE UNIQUE INDEX uq_card_catalog_source_external_id ON card (catalog_source, external_card_id);
