DROP FUNCTION presences.delete_alert(bigint, varchar, varchar, varchar);

DROP TRIGGER remove_associate_reason_alert_before_delete_reason ON presences.reason;
DROP FUNCTION presences.remove_associate_reason_alert();

DROP TRIGGER remove_event_alert ON presences.event;
DROP FUNCTION presences.remove_event_alert();
CREATE FUNCTION presences.decrement_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structure_identifier character varying;
    recovery_method character varying;
    should_decrement boolean;
    end_of_half_day time without time zone;
    count_alert bigint;
BEGIN
    -- Retrieve event structure identifier based on new event register identifier
    SELECT structure_id FROM presences.register WHERE id = OLD.register_id INTO structure_identifier;

    CASE OLD.type_id
        WHEN 1 THEN -- Absence creation
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(OLD.student_id, structure_identifier, 'ABSENCE');

            SELECT event_recovery_method FROM presences.settings WHERE structure_id = structure_identifier INTO recovery_method;

            CASE recovery_method
                WHEN 'HALF_DAY' THEN
                    -- First retrieve half day hour
                    SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structure_identifier INTO end_of_half_day;
                    if OLD.start_date::time < end_of_half_day THEN
                        SELECT NOT presences.exists_absence_in_half_day(OLD.start_date::date, '00:00:00'::time, end_of_half_day, OLD.student_id, structure_identifier) INTO should_decrement;
                    ELSE
                        SELECT NOT presences.exists_absence_in_half_day(OLD.start_date::date, end_of_half_day,'23:59:59'::time, OLD.student_id, structure_identifier) INTO should_decrement;
                    END IF;
                WHEN 'DAY' THEN
                    -- Check if student already have absence for the day based on provided structure identifier
                    SELECT NOT presences.exists_absence_in_day(OLD.start_date::date, OLD.student_id, structure_identifier) INTO should_decrement;
                ELSE
                    -- 'HOUR' is a classic case. Just increment count.
                    should_decrement = true;
                END CASE;

            IF should_decrement THEN
                SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'ABSENCE' INTO count_alert;

                IF count_alert > 0 THEN
                    UPDATE presences.alerts SET count = (count - 1)
                    WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'ABSENCE';
                END IF;
            END IF;
        WHEN 2 THEN -- Lateness creation
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(OLD.student_id, structure_identifier, 'LATENESS');

            SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'LATENESS' INTO count_alert;
            IF count_alert > 0 THEN
                UPDATE presences.alerts SET count = (count - 1)
                WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'LATENESS';
            END IF;
        ELSE
            RETURN OLD;
        END CASE;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;

DROP TRIGGER remove_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.remove_incident_alert();
CREATE TRIGGER decrement_event_alert_after_justifying AFTER UPDATE OF reason_id ON presences.event
    FOR EACH ROW WHEN (NEW.reason_id IS NOT NULL) EXECUTE PROCEDURE presences.decrement_event_alert();
CREATE TRIGGER decrement_event_alert AFTER DELETE ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.decrement_event_alert();

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





DROP TRIGGER remove_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.remove_notebook_alert();
CREATE FUNCTION presences.decrement_notebook_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    countAlert bigint;
BEGIN
    -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
    EXECUTE presences.create_alert(OLD.student_id, OLD.structure_id, 'FORGOTTEN_NOTEBOOK');

    -- Get alert count. We need it because we need to check if it is > 0
    SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = OLD.structure_id AND type = 'FORGOTTEN_NOTEBOOK' INTO countAlert ;

    -- If count > 0 then update student alert for current type
    IF countAlert > 0 THEN
        UPDATE presences.alerts SET count = (count - 1)
        WHERE student_id = OLD.student_id AND structure_id = OLD.structure_id AND type = 'FORGOTTEN_NOTEBOOK';
    END IF;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER decrement_notebook_alert AFTER DELETE ON presences.forgotten_notebook FOR EACH ROW EXECUTE PROCEDURE presences.decrement_notebook_alert();