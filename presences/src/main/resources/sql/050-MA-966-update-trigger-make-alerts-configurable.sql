-- Manages whether to create or delete an alert following the modification of an absence event
CREATE OR REPLACE FUNCTION presences.update_absence_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    isNewEventExclude boolean;
    structureId character varying;
    oldAlertID bigint;
    eventIdToReplace bigint;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO structureId;

    -- Check if the new event must be exclude
    SELECT presences.absence_exclude_alert(NEW, structureId) INTO isNewEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'ABSENCE' AND student_id = OLD.student_id AND structure_id = structureId INTO oldAlertID;

    IF oldAlertID IS NOT NULL AND isNewEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        SELECT presences.get_id_absence_event_siblings(OLD, structureId, false) INTO eventIdToReplace;
        EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, structureId);
        IF eventIdToReplace IS NOT NULL THEN -- If we have a no other absence in the same time with no alert, we need to create a alert for other absence
            EXECUTE presences.create_alert(eventIdToReplace, 'ABSENCE', NEW.student_id, structureId);
        END IF;
    ELSIF oldAlertID IS NULL AND NOT isNewEventExclude THEN -- If we have not previously a alert and now the event is not exclude we must create alert
        SELECT presences.get_id_absence_event_siblings(OLD, structureId, true) INTO eventIdToReplace;
        IF eventIdToReplace IS NULL THEN -- If we have a other absence in the same time with alert, we DONT need create a alert
            EXECUTE presences.create_alert(NEW.id, 'ABSENCE', NEW.student_id, structureId);
        END IF;
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
-- For each update we need to update associated alert
CREATE TRIGGER update_absence_alert BEFORE UPDATE OF reason_id, start_date, end_date, counsellor_regularisation ON presences.event
    FOR EACH ROW WHEN (NEW.type_id = 1) EXECUTE PROCEDURE presences.update_absence_alert();

-- Manages whether to create or delete an alert following the modification of an lateness event
CREATE FUNCTION presences.update_lateness_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    newEventExclude boolean;
    structureId character varying;
    oldAlertId bigint;
BEGIN
    -- Select the structure associate with the new event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO structureId;

    -- Check if the new event must be exclude
    SELECT presences.lateness_exclude_alert(NEW, structureId) INTO newEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'LATENESS' AND student_id = OLD.student_id AND structure_id = structureId INTO oldAlertId;


    IF oldAlertId IS NOT NULL AND newEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, structureId);
    ELSIF oldAlertId IS NULL AND NOT newEventExclude THEN  -- If we have not previously a alert and now the event is not exclude we must create alert
        EXECUTE presences.create_alert(NEW.id, 'LATENESS', NEW.student_id, structureId);
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- For each update we need to update associated alert
CREATE TRIGGER update_lateness_alert BEFORE UPDATE OF reason_id ON presences.event
    FOR EACH ROW WHEN (NEW.type_id = 2) EXECUTE PROCEDURE presences.update_lateness_alert();