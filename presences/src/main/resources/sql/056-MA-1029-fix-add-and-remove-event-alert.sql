-- Use for each insert in presences.event
CREATE OR REPLACE FUNCTION presences.add_event_alert() RETURNS TRIGGER AS
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
        ELSE
        END CASE;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.remove_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    register presences.register;
    eventNeedAlert presences.event;
BEGIN
    SELECT * FROM presences.register WHERE id = OLD.register_id LIMIT 1 INTO register;
    CASE OLD.type_id
        WHEN 1 THEN -- Absence creation
        -- Retrieve event structure identifier based on new event register identifier

            SELECT * FROM presences.get_id_absence_event_siblings(OLD, register.structure_id, false) INTO eventNeedAlert;
            EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, register.structure_id);
            -- If we have other absence in the same time with no alert, we need a alert for this absence
            -- The 2 events are necessarily on the same structure, no need to recalculate the register
            IF eventNeedAlert.id IS NOT NULL AND NOT presences.absence_exclude_alert(eventNeedAlert, register.structure_id) THEN
                SELECT * FROM presences.register WHERE id = eventNeedAlert.register_id LIMIT 1 INTO register;
                EXECUTE presences.create_alert(eventNeedAlert.id, 'ABSENCE', OLD.student_id, register.structure_id, register.start_date);
            END IF;
        WHEN 2 THEN -- Lateness creation
        EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, register.structure_id);
        ELSE
        END CASE;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;