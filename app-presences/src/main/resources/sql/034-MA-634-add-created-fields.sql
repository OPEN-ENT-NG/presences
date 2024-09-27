ALTER TABLE presences.absence
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.actions
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.alert_history
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.alerts
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.discipline
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.exemption
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.exemption_recursive
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.forgotten_notebook
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.presence
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE presences.reason
ADD COLUMN created timestamp without time zone NOT NULL DEFAULT now();