CREATE or replace FUNCTION presences.update_absence_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    isNewEventExclude boolean;
    structureIdentifier character varying;
    oldAlertID bigint;
    eventIdToReplace bigint;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO structureIdentifier;

    -- Check if the new event must be exclude
    SELECT presences.absence_exclude(NEW, structureIdentifier) INTO isNewEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'ABSENCE' AND student_id = OLD.student_id AND structure_id = structureIdentifier INTO oldAlertID;

    IF oldAlertID IS NOT NULL AND isNewEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        SELECT presences.get_one_other_absence_in_same_date_which_no_alert(OLD, structureIdentifier, false) INTO eventIdToReplace;
        EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, structureIdentifier);
        IF eventIdToReplace IS NOT NULL THEN -- If we have a no other alert in the same time with no alert
            EXECUTE presences.create_alert(eventIdToReplace, 'ABSENCE', NEW.student_id, structureIdentifier);
        END IF;
    ELSIF oldAlertID IS NULL AND NOT isNewEventExclude THEN -- If we have not previously a alert and now the event is not exclude we must create alert
        SELECT presences.get_one_other_absence_in_same_date_which_no_alert(OLD, structureIdentifier, true) INTO eventIdToReplace;
        IF eventIdToReplace IS NULL THEN -- If we have a other alert in the same time with alert, w
            EXECUTE presences.create_alert(NEW.id, 'ABSENCE', NEW.student_id, structureIdentifier);
        END IF;
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER update_absence_alert BEFORE UPDATE OF reason_id, start_date, end_date, counsellor_regularisation ON presences.event
    FOR EACH ROW WHEN (NEW.type_id = 1) EXECUTE PROCEDURE presences.update_absence_alert();





CREATE FUNCTION presences.update_lateness_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    newEventExclude boolean;
    structureIdentifier character varying;
    oldAlertId bigint;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO structureIdentifier;

    -- Check if the new event must be exclude
    SELECT presences.lateness_exclude(NEW, structureIdentifier) INTO newEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'LATENESS' AND student_id = OLD.student_id AND structure_id = structureIdentifier INTO oldAlertId;


    IF oldAlertId IS NOT NULL AND newEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, structureIdentifier);
    ELSIF oldAlertId IS NULL AND NOT newEventExclude THEN  -- If we have not previously a alert and now the event is not exclude we must create alert
        EXECUTE presences.create_alert(NEW.id, 'LATENESS', NEW.student_id, structureIdentifier);
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_lateness_alert BEFORE UPDATE OF reason_id ON presences.event
    FOR EACH ROW WHEN (NEW.type_id = 2) EXECUTE PROCEDURE presences.update_lateness_alert();







CREATE FUNCTION incidents.update_incident_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    newEventExclude boolean;
    oldAlertId bigint = NULL;
    protagonist incidents.protagonist;
BEGIN
    -- Check if the new event must be exclude
    SELECT incidents.incident_exclude(NEW) INTO newEventExclude;

    FOR protagonist IN SELECT * FROM incidents.protagonist WHERE incident_id = NEW.id
        LOOP
            -- Check if the old data have alert
            SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'INCIDENT' AND student_id = protagonist.user_id AND structure_id = OLD.structure_id INTO oldAlertId;
            IF oldAlertId IS NOT NULL AND newEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
                EXECUTE presences.delete_alert(OLD.id, 'INCIDENT', protagonist.user_id, NEW.structure_id);
            ELSIF oldAlertId IS NULL AND NOT newEventExclude THEN -- If we have not previously a alert and now the event is not exclude we must create alert
                EXECUTE presences.create_alert(NEW.id, 'INCIDENT', protagonist.user_id, NEW.structure_id);
            END IF;
    END LOOP;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_incident_alert BEFORE UPDATE OF seriousness_id ON incidents.incident
    FOR EACH ROW EXECUTE PROCEDURE incidents.update_incident_alert();
