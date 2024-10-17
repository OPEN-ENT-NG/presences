-- Manages whether to create or delete an alert following the modification of an absence event
CREATE OR REPLACE FUNCTION presences.update_absence_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    isNewEventExclude boolean;
    register presences.register;
    oldAlertID bigint;
    eventToReplace presences.event;
BEGIN
    -- Select the structure associate with the new event
    SELECT * FROM presences.register WHERE id = NEW.register_id INTO register;

    -- Check if the new event must be exclude
    SELECT presences.absence_exclude_alert(NEW, register.structure_id) INTO isNewEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'ABSENCE' AND student_id = OLD.student_id AND structure_id = register.structure_id INTO oldAlertID;
    IF oldAlertID IS NOT NULL AND isNewEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        SELECT * FROM presences.get_id_absence_event_siblings(OLD, register.structure_id, false) INTO eventToReplace;
        EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, register.structure_id);
        -- The 2 events are necessarily on the same structure, no need to recalculate the register
        -- If we have a no other absence in the same time with no alert, we need to create a alert for other absence
        IF eventToReplace.id IS NOT NULL AND NOT presences.absence_exclude_alert(eventToReplace, register.structure_id) THEN
            SELECT * FROM presences.register WHERE id = eventToReplace.register_id LIMIT 1 INTO register;
            EXECUTE presences.create_alert(eventToReplace.id, 'ABSENCE', NEW.student_id, register.structure_id, register.start_date);
        END IF;
    ELSIF oldAlertID IS NULL AND NOT isNewEventExclude THEN -- If we have not previously a alert and now the event is not exclude we must create alert
        SELECT * FROM presences.get_id_absence_event_siblings(OLD, register.structure_id, true) INTO eventToReplace;
        IF eventToReplace.id IS NULL THEN -- If we have a other absence in the same time with alert, we DONT need create a alert
            EXECUTE presences.create_alert(NEW.id, 'ABSENCE', NEW.student_id, register.structure_id, register.start_date);
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
    register presences.register;
    oldAlertId bigint;
BEGIN
    -- Select the structure associate with the new event
    SELECT * FROM presences.register WHERE id = NEW.register_id LIMIT 1 INTO register;

    -- Check if the new event must be exclude
    SELECT presences.lateness_exclude_alert(NEW, register.structure_id) INTO newEventExclude;
    -- Check if the old data have alert
    SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'LATENESS' AND student_id = OLD.student_id AND structure_id = register.structure_id INTO oldAlertId;


    IF oldAlertId IS NOT NULL AND newEventExclude THEN -- If we have previously a alert and now the event is exclude we must delete alert
        EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, register.structure_id);
    ELSIF oldAlertId IS NULL AND NOT newEventExclude THEN  -- If we have not previously a alert and now the event is not exclude we must create alert
        EXECUTE presences.create_alert(NEW.id, 'LATENESS', NEW.student_id, register.structure_id, register.start_date);
    END IF;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

-- For each update we need to update associated alert
CREATE TRIGGER update_lateness_alert BEFORE UPDATE OF reason_id ON presences.event
    FOR EACH ROW WHEN (NEW.type_id = 2) EXECUTE PROCEDURE presences.update_lateness_alert();



-- Update alert date when forgotten notebook date change
CREATE FUNCTION presences.update_forgotten_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
BEGIN
    UPDATE presences.alerts SET date = NEW.date WHERE event_id = NEW.id AND type = 'FORGOTTEN_NOTEBOOK';
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
-- For each update we need to update associated alert
CREATE TRIGGER update_forgotten_alert BEFORE UPDATE OF date ON presences.forgotten_notebook
    FOR EACH ROW EXECUTE PROCEDURE presences.update_forgotten_alert();