ALTER TABLE presences.reason
DROP CONSTRAINT fk_reason_type_id;

ALTER TABLE presences.reason
DROP COLUMN type_id;