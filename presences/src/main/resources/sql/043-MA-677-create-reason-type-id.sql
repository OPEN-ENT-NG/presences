ALTER TABLE presences.reason
    ADD COLUMN reason_type_id bigint default 1;

UPDATE presences.reason SET reason_type_id = 1
WHERE reason_type_id IS NULL