ALTER TABLE presences.alerts_settings
RENAME TO settings;

ALTER TABLE presences.settings
RENAME CONSTRAINT alerts_settings_pkey TO settings_pkey;

ALTER TABLE presences.settings
RENAME COLUMN absence TO alert_absence_threshold;

ALTER TABLE presences.settings
RENAME COLUMN lateness TO alert_lateness_threshold;

ALTER TABLE presences.settings
RENAME COLUMN incident TO alert_incident_threshold;

ALTER TABLE presences.settings
RENAME COLUMN forgotten_notebook TO alert_forgotten_notebook_threshold;

ALTER TABLE presences.settings
ADD COLUMN event_recovery_method character varying (50) NOT NULL DEFAULT 'HALF_DAY';

ALTER TABLE presences.settings
ADD CONSTRAINT event_recovery_method_type_check CHECK (event_recovery_method = ANY (ARRAY['HOUR'::text, 'HALF_DAY'::text, 'DAY'::text]));

CREATE OR REPLACE FUNCTION presences.init_settings() RETURNS TRIGGER AS
$BODY$
DECLARE
    countEtab bigint;
BEGIN
    SELECT count(structure_id) FROM presences.settings WHERE structure_id = NEW.id_etablissement INTO countEtab;
    IF countEtab = 0 THEN
        INSERT INTO presences.settings (structure_id) VALUES (NEW.id_etablissement);
    END IF;

    RETURN NEW;
END
$BODY$
LANGUAGE plpgsql;

CREATE TRIGGER init_structure_settings AFTER INSERT ON presences.etablissements_actifs
FOR EACH ROW EXECUTE PROCEDURE presences.init_settings();

INSERT INTO presences.settings (structure_id)
SELECT id_etablissement
FROM presences.etablissements_actifs
WHERE etablissements_actifs.id_etablissement NOT IN (
    SELECT structure_id
    FROM presences.settings
);