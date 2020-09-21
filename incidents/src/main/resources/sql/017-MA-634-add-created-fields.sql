ALTER TABLE incidents.incident_type
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.partner
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.place
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.protagonist_type
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.punishment_category
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.punishment_type
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE incidents.seriousness
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();