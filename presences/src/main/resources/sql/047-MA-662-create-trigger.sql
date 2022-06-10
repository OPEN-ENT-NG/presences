DROP FUNCTION presences.create_alert(varchar, varchar, varchar);
-- Create a new function that create an alert for given student.
CREATE OR REPLACE FUNCTION presences.create_alert(eventId bigint, alertType varchar, studentId varchar, structureId varchar) RETURNS void AS
$BODY$
DECLARE
    alertExist boolean;
BEGIN
    -- Check if student alert exists
    SELECT exists(SELECT * FROM presences.alerts
                           WHERE event_id = eventId AND type = alertType AND student_id = studentId AND structure_id = structureId)
    INTO alertExist;

    -- IF student alert does not exists, creates one.
    IF alertExist IS FALSE THEN
        INSERT INTO presences.alerts(event_id, type, student_id, structure_id, modified) VALUES (eventId, alertType, studentId, structureId, now());
    END IF;
    RETURN;
END;
$BODY$
    LANGUAGE plpgsql;

DROP TRIGGER increment_event_alert_after_unjustifying ON presences.event;
DROP TRIGGER increment_event_alert ON presences.event;
DROP FUNCTION presences.increment_event_alert();

-- Use for each insert in presences.event
CREATE or replace FUNCTION presences.add_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureIdentifier character varying;
    existingAbsenceWithAlert bigint = NULL;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id INTO structureIdentifier;
    CASE NEW.type_id
        WHEN 1 THEN -- Absence creation
            SELECT presences.get_one_other_absence_in_same_date_which_no_alert(NEW, structureIdentifier, true) INTO existingAbsenceWithAlert;
            -- If we don't have have other absence with alert and the event is not exclude
            IF existingAbsenceWithAlert IS NULL AND NOT presences.absence_exclude(NEW, structureIdentifier) THEN
                -- Create alert
                EXECUTE presences.create_alert(NEW.id, 'ABSENCE', NEW.student_id, structureIdentifier);
            END IF;
        WHEN 2 THEN -- Lateness creation
            IF NOT presences.lateness_exclude(NEW, structureIdentifier) THEN -- If we have no exclude condition
                -- Create alert
                EXECUTE presences.create_alert(NEW.id, 'LATENESS', NEW.student_id, structureIdentifier);
            END IF;
        END CASE;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER add_event_alert BEFORE INSERT ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.add_event_alert();



DROP TRIGGER increment_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.increment_incident_alert();

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE FUNCTION incidents.add_incident_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureIdentifier character varying;
BEGIN
    -- Select the structure associate with the new protagonist
    SELECT structure_id FROM incidents.incident WHERE id = NEW.incident_id INTO structureIdentifier;

    IF incidents.incident_protagonist_exclude(NEW, structureIdentifier) IS FALSE THEN -- If we have no exclude condition
        -- Create alert
        EXECUTE presences.create_alert(NEW.incident_id, 'INCIDENT', NEW.user_id, structureIdentifier);
    END IF;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- Because an incident may be in relationship with many protagonists, we trigger alert on protagonist insert
CREATE TRIGGER add_incident_alert AFTER INSERT ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.add_incident_alert();





DROP TRIGGER increment_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.increment_notebook_alert();
CREATE FUNCTION presences.add_notebook_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureIdentifier character varying;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.forgotten_notebook WHERE id = NEW.id INTO structureIdentifier;

    IF presences.notebook_exclude(structureIdentifier) IS FALSE THEN -- If we have no exclude condition
        -- Create alert
        EXECUTE presences.create_alert(NEW.id, 'FORGOTTEN_NOTEBOOK', NEW.student_id, structureIdentifier);
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER add_notebook_alert AFTER INSERT ON presences.forgotten_notebook
    FOR EACH ROW EXECUTE PROCEDURE presences.add_notebook_alert();