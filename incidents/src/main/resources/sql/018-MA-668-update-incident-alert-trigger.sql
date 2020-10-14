CREATE OR REPLACE FUNCTION incidents.decrement_incident_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        structureId character varying;
    BEGIN
        -- Retrieve structure identifier based on incident identifier
        SELECT structure_id FROM incidents.incident WHERE id = OLD.incident_id INTO structureId;

        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
        EXECUTE presences.create_alert(OLD.user_id, structureId, 'INCIDENT');

        -- Decrement student incident alert
        UPDATE presences.alerts SET count = (count - 1)
        WHERE student_id = OLD.user_id AND structure_id = structureId AND type = 'INCIDENT';

        RETURN OLD;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER decrement_incident_alert AFTER DELETE ON incidents.protagonist
    FOR EACH ROW EXECUTE PROCEDURE incidents.decrement_incident_alert();

