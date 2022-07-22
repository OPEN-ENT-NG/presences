-- Gets the alert threshold for a structure and data type
CREATE OR REPLACE FUNCTION presences.get_alert_thresholder(type character varying, structureId varchar) RETURNS BIGINT AS
$BODY$
DECLARE
    result bigint;
BEGIN
    CASE type
        WHEN 'ABSENCE' THEN result = (SELECT alert_absence_threshold FROM presences.settings WHERE structure_id = structureId LIMIT 1);
        WHEN 'LATENESS' THEN result = (SELECT alert_lateness_threshold FROM presences.settings WHERE structure_id = structureId LIMIT 1);
        WHEN 'INCIDENT' THEN result = (SELECT alert_incident_threshold FROM presences.settings WHERE structure_id = structureId LIMIT 1);
        WHEN 'FORGOTTEN_NOTEBOOK' THEN result = (SELECT alert_forgotten_notebook_threshold FROM presences.settings WHERE structure_id = structureId LIMIT 1); END CASE;
    RETURN result;
END
$BODY$
    LANGUAGE plpgsql;