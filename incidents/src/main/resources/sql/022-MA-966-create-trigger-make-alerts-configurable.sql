-- Drop old trigger and function
DROP TRIGGER increment_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.increment_incident_alert();

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE FUNCTION incidents.add_incident_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    incident incidents.incident;
BEGIN
    -- Select the structure associate with the new protagonist
    SELECT * FROM incidents.incident WHERE id = NEW.incident_id INTO incident;

    IF incidents.incident_protagonist_exclude_alert(NEW, incident.structure_id) IS FALSE THEN -- If we have no exclude condition
        -- Create alert
        EXECUTE presences.create_alert(NEW.incident_id, 'INCIDENT', NEW.user_id, incident.structure_id, incident.date);
    END IF;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE TRIGGER add_incident_alert AFTER INSERT ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.add_incident_alert();