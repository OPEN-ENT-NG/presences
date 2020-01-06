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

CREATE OR REPLACE FUNCTION incidents.decrement_incident_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        countAlert bigint;
        user incidents.protagonist%rowtype;
    BEGIN
        -- For each protagonists
        FOR user IN SELECT user_id FROM incidents.protagonist WHERE incident_id = OLD.id LOOP
            -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(user, OLD.structure_id, 'INCIDENT');

            -- Get alert count. We need it because we need to check if it is > 0
            SELECT count FROM presences.alerts WHERE student_id = user AND structure_id = OLD.structure_id AND type = 'INCIDENT' INTO countAlert ;

            -- If count > 0 then update student alert for current type
            IF countAlert  > 0 THEN
                UPDATE presences.alerts SET count = (count - 1)
                WHERE student_id = user AND structure_id = OLD.structure_id AND type = 'INCIDENT';
            END IF;
        END LOOP;

        RETURN OLD;
    END
    $BODY$
LANGUAGE plpgsql;

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE TRIGGER increment_incident_alert AFTER INSERT ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.increment_incident_alert();
-- Because protagonists are in relationship with incident and incident deletion deletes protagonists, we trigger alert BEFORE incident deletion
CREATE TRIGGER decrement_incident_alert BEFORE DELETE ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.decrement_incident_alert();