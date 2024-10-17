ALTER TABLE presences.event
    ADD COLUMN followed boolean NOT NULL DEFAULT false;

ALTER TABLE presences.absence
    ADD COLUMN followed boolean NOT NULL DEFAULT false;