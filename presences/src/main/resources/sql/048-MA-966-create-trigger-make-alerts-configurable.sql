-- Drop old function
DROP FUNCTION presences.create_alert(varchar, varchar, varchar);
-- Create a new function that create an alert for given student.
CREATE OR REPLACE FUNCTION presences.create_alert(eventId bigint, alertType varchar, studentId varchar, structureId varchar, date timestamp without time zone) RETURNS void AS
$BODY$
DECLARE
    alertExist boolean;
BEGIN
    -- Check if we have already a alert
    SELECT exists(SELECT * FROM presences.alerts
                           WHERE event_id = eventId AND type = alertType AND student_id = studentId AND structure_id = structureId)
    INTO alertExist;

    -- If not we create one
    IF alertExist IS FALSE THEN
        INSERT INTO presences.alerts(event_id, type, student_id, structure_id, date) VALUES (eventId, alertType, studentId, structureId, date);
    END IF;
    RETURN;
END;
$BODY$
    LANGUAGE plpgsql;

-- Drop old trigger and function
DROP TRIGGER increment_event_alert_after_unjustifying ON presences.event;
DROP TRIGGER increment_event_alert ON presences.event;
DROP FUNCTION presences.increment_event_alert();

-- Use for each insert in presences.event
CREATE or replace FUNCTION presences.add_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    register presences.register;
    existingAbsenceWithAlert presences.event;
BEGIN
    -- Select the structure associate with the new event
    SELECT * FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO register;
    CASE NEW.type_id
        WHEN 1 THEN -- Absence creation
            SELECT * FROM presences.get_id_absence_event_siblings(NEW, register.structure_id, true) INTO existingAbsenceWithAlert;
            -- If we don't have have other absence with alert and the event is not exclude
            IF existingAbsenceWithAlert.id IS NULL AND NOT presences.absence_exclude_alert(NEW, register.structure_id) THEN
                -- Create alert
                EXECUTE presences.create_alert(NEW.id, 'ABSENCE', NEW.student_id, register.structure_id, register.start_date);
            END IF;
        WHEN 2 THEN -- Lateness creation
            IF NOT presences.lateness_exclude_alert(NEW, register.structure_id) THEN -- If we have no exclude condition
                -- Create alert
                EXECUTE presences.create_alert(NEW.id, 'LATENESS', NEW.student_id, register.structure_id, register.start_date);
            END IF;
        END CASE;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- For each creation of a event we need to create a alert
CREATE TRIGGER add_event_alert BEFORE INSERT ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.add_event_alert();

-- Drop old trigger and function
DROP TRIGGER increment_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.increment_notebook_alert();

CREATE FUNCTION presences.add_notebook_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureId character varying;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.forgotten_notebook WHERE id = NEW.id INTO structureId;

    IF presences.notebook_exclude_alert(structureId) IS FALSE THEN -- If we have no exclude condition
        -- Create alert
        EXECUTE presences.create_alert(NEW.id, 'FORGOTTEN_NOTEBOOK', NEW.student_id, structureId, NEW.date);
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- For each creation of a forgotten notebook we need to create a alert
CREATE TRIGGER add_notebook_alert AFTER INSERT ON presences.forgotten_notebook
    FOR EACH ROW EXECUTE PROCEDURE presences.add_notebook_alert();