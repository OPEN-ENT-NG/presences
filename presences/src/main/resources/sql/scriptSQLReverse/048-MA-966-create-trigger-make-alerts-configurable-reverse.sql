DROP FUNCTION presences.create_alert(bigint, varchar, varchar, varchar);
-- Create a new function that initialize an alert for given student.
CREATE OR REPLACE FUNCTION presences.create_alert(studentId varchar, structureId varchar, alertType varchar) RETURNS void AS
$BODY$
DECLARE
    studentExist boolean;
BEGIN
    -- Check if student alert exists
    SELECT exists(SELECT * FROM presences.alerts WHERE student_id = studentId AND structure_id = structureId AND type = alertType) INTO studentExist;

    -- IF student alert does not exists, creates one.
    IF studentExist IS FALSE THEN
        INSERT INTO presences.alerts (student_id, structure_id, type) VALUES (studentId, structureId, alertType);
    END IF;
    RETURN;
END;
$BODY$
    LANGUAGE plpgsql;

DROP TRIGGER add_event_alert ON presences.event;
DROP FUNCTION presences.add_event_alert();
CREATE FUNCTION presences.increment_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structure_identifier character varying;
    recovery_method character varying;
    should_increment boolean;
    end_of_half_day time without time zone;
BEGIN
    -- Retrieve event structure identifier based on new event register identifier
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id INTO structure_identifier;

    CASE NEW.type_id
        WHEN 1 THEN -- Absence creation
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            IF NEW.reason_id IS NULL THEN
                EXECUTE presences.create_alert(NEW.student_id, structure_identifier, 'ABSENCE');

                SELECT event_recovery_method FROM presences.settings WHERE structure_id = structure_identifier INTO recovery_method;

                CASE recovery_method
                    WHEN 'HALF_DAY' THEN
                        -- First retrieve half day hour
                        SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structure_identifier INTO end_of_half_day;
                        if NEW.start_date::time < end_of_half_day THEN
                            SELECT NOT presences.exists_absence_in_half_day(NEW.start_date::date, '00:00:00'::time, end_of_half_day, NEW.student_id, structure_identifier) INTO should_increment;
                        ELSE
                            SELECT NOT presences.exists_absence_in_half_day(NEW.start_date::date, end_of_half_day,'23:59:59'::time, NEW.student_id, structure_identifier) INTO should_increment;
                        END IF;
                    WHEN 'DAY' THEN
                        -- Check if student already have absence for the day based on provided structure identifier
                        SELECT NOT presences.exists_absence_in_day(NEW.start_date::date, NEW.student_id, structure_identifier) INTO should_increment;
                    ELSE
                        -- 'HOUR' is a classic case. Just increment count.
                        should_increment = true;
                    END CASE;
            ELSE
                should_increment = false;
            END IF;

            IF should_increment THEN
                UPDATE presences.alerts SET count = (count + 1)
                WHERE student_id = NEW.student_id AND structure_id = structure_identifier AND type = 'ABSENCE';
            END IF;
        WHEN 2 THEN -- Lateness creation
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(NEW.student_id, structure_identifier, 'LATENESS');

            -- Increment event alert number
            UPDATE presences.alerts SET count = (count + 1)
            WHERE student_id = NEW.student_id AND structure_id = structure_identifier AND type = 'LATENESS';
        ELSE
            RETURN NEW;
        END CASE;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER increment_event_alert_after_unjustifying BEFORE UPDATE OF reason_id ON presences.event
    FOR EACH ROW WHEN (NEW.reason_id IS NULL) EXECUTE PROCEDURE presences.increment_event_alert();

CREATE TRIGGER increment_event_alert BEFORE INSERT ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.increment_event_alert();




DROP TRIGGER add_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.add_incident_alert();
CREATE FUNCTION incidents.increment_incident_alert() RETURNS TRIGGER AS
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
CREATE TRIGGER increment_incident_alert AFTER INSERT ON incidents.protagonist FOR EACH ROW EXECUTE PROCEDURE incidents.increment_incident_alert();




DROP TRIGGER add_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.add_notebook_alert();
CREATE FUNCTION presences.increment_notebook_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
    EXECUTE presences.create_alert(NEW.student_id, NEW.structure_id, 'FORGOTTEN_NOTEBOOK');

    -- Increment student forgotten_notebook alert
    UPDATE presences.alerts SET count = (count + 1)
    WHERE student_id = NEW.student_id AND structure_id = NEW.structure_id AND type = 'FORGOTTEN_NOTEBOOK';

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER increment_notebook_alert AFTER INSERT ON presences.forgotten_notebook FOR EACH ROW EXECUTE PROCEDURE presences.increment_notebook_alert();