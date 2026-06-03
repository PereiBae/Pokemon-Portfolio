ALTER TABLE card ADD COLUMN available_variant_codes VARCHAR(500) NOT NULL DEFAULT 'STANDARD';
UPDATE card SET available_variant_codes = variant WHERE available_variant_codes = 'STANDARD' AND variant IS NOT NULL;

ALTER TABLE owned_item ADD COLUMN owned_variant VARCHAR(80) NOT NULL DEFAULT 'STANDARD';
UPDATE owned_item
SET owned_variant = COALESCE((SELECT c.variant FROM card c WHERE c.id = owned_item.card_id), 'STANDARD');
