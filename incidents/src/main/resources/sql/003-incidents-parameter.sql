ALTER TABLE incidents.incident_type
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;

ALTER TABLE incidents.partner
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;

ALTER TABLE incidents.place
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;

ALTER TABLE incidents.protagonist_type
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;

ALTER TABLE incidents.seriousness
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;