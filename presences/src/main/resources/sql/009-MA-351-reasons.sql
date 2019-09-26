ALTER TABLE presences.reason
    ADD COLUMN hidden boolean NOT NULL DEFAULT false,
    ADD COLUMN absence_compliance boolean NOT NULL DEFAULT false;