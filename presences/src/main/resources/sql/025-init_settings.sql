CREATE OR REPLACE FUNCTION presences.init_settings() RETURNS TRIGGER AS
$BODY$
DECLARE
    countEtab bigint;
BEGIN
    SELECT count(structure_id) FROM presences.settings WHERE structure_id = NEW.id_etablissement INTO countEtab;
    IF countEtab = 0 THEN
        INSERT INTO presences.settings (structure_id, alert_absence_threshold, alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold)
        VALUES (NEW.id_etablissement, 5, 3, 3, 3);
    END IF;

    RETURN NEW;
END
$BODY$
LANGUAGE plpgsql;