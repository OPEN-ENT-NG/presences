CREATE OR REPLACE FUNCTION incidents.increment_incident_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        structureId character varying;
    BEGIN
        -- Retrieve structure identifier based on incident identifier
        SELECT structure_id FROM incidents.incident WHERE id = NEW.incident_id INTO structureId;

        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
        EXECUTE presences.create_alert(NEW.user_id, structureId, 'INCIDENT');

        -- Increment student incident alert
        UPDATE presences.alerts SET count = (count + 1)
        WHERE student_id = NEW.user_id AND structure_id = structureId AND type = 'INCIDENT';

        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

-- Create a new function that manage alert and delete incident. To delete an incident, use the function as: SELECT incidents.delete_incident(42);
CREATE OR REPLACE FUNCTION incidents.delete_incident(incidentId bigint) RETURNS void AS
    $BODY$
    DECLARE
    countAlert bigint;
        incident incidents.incident%rowtype;
        protagonist incidents.protagonist%rowtype;
    BEGIN
        SELECT * FROM incidents.incident WHERE id = incidentId INTO incident;
        FOR protagonist IN SELECT * FROM incidents.protagonist WHERE incident_id = incidentId
        -- Before deleting incident, for each protagonist
        LOOP
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
        EXECUTE presences.create_alert(protagonist.user_id, incident.structure_id, 'INCIDENT');

         -- Get alert count. We need it because we need to check if it is > 0
        SELECT count FROM presences.alerts WHERE student_id = protagonist.user_id AND structure_id = incident.structure_id AND type = 'INCIDENT' INTO countAlert;

        -- If count > 0 then update student alert for current type
        IF countAlert  > 0 THEN
            UPDATE presences.alerts SET count = (count - 1)
            WHERE student_id = protagonist.user_id AND structure_id = incident.structure_id AND type = 'INCIDENT';
        END IF;
    END LOOP;

    DELETE FROM incidents.incident WHERE id = incidentId;
    END;
    $BODY$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS increment_incident_alert ON incidents.protagonist;

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE TRIGGER increment_incident_alert AFTER INSERT ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.increment_incident_alert();