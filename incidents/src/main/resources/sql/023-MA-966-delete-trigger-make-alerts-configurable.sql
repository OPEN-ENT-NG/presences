-- Drop old trigger and function
DROP TRIGGER decrement_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.decrement_incident_alert();

-- Delete alert for each protagonist of the incident if the event having one
CREATE OR REPLACE FUNCTION incidents.delete_incident(incidentId bigint) RETURNS void AS
$BODY$
DECLARE
    incident incidents.incident;
    protagonist incidents.protagonist;
BEGIN
    SELECT * FROM incidents.incident WHERE id = incidentId INTO incident;
    FOR protagonist IN SELECT * FROM incidents.protagonist WHERE incident_id = incidentId
        -- Before deleting incident, for each protagonist
        LOOP
            -- Delete alert for each protagonist
            EXECUTE presences.delete_alert(incidentId, 'INCIDENT', protagonist.user_id, incident.structure_id);
    END LOOP;
    DELETE FROM incidents.incident WHERE id = incidentId;
END;
$BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION incidents.remove_incident_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    incident incidents.incident;
BEGIN
    SELECT * FROM incidents.incident WHERE id = OLD.incident_id LIMIT 1 INTO incident;
    EXECUTE presences.delete_alert(OLD.incident_id, 'INCIDENT', OLD.user_id, incident.structure_id);
    RETURN OLD;
END;
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER remove_incident_alert before DELETE ON incidents.protagonist
    FOR EACH ROW EXECUTE PROCEDURE incidents.remove_incident_alert();