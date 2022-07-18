CREATE FUNCTION presences.delete_alert(eventId bigint, alertType varchar, studentId varchar, structureId varchar) RETURNS void AS
$BODY$
BEGIN
    DELETE FROM presences.alerts WHERE event_id = eventId AND type = alertType AND student_id = studentId AND structure_id = structureId;
    RETURN;
END;
$BODY$
    LANGUAGE plpgsql;

-- Drop old trigger and function
DROP TRIGGER decrement_event_alert ON presences.event;
DROP TRIGGER decrement_event_alert_after_justifying ON presences.event;
DROP FUNCTION presences.decrement_event_alert();

-- Delete alert if the event having one
CREATE FUNCTION presences.remove_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureId character varying;
    eventIdNeedAlert bigint;
BEGIN
    CASE OLD.type_id
        WHEN 1 THEN -- Absence creation
            -- Retrieve event structure identifier based on new event register identifier
            SELECT structure_id FROM presences.register WHERE id = OLD.register_id INTO structureId;

            SELECT presences.get_id_absence_event_siblings(OLD, structureId, false) INTO eventIdNeedAlert;
            EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, structureId);
            IF eventIdNeedAlert IS NOT NULL THEN -- If we have other absence in the same time with no alert, we need a alert for this absence
                EXECUTE presences.create_alert(eventIdNeedAlert, 'ABSENCE', OLD.student_id, structureId);
            END IF;
        WHEN 2 THEN -- Lateness creation
            EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, structureId);
        END CASE;
        RETURN OLD;
    END
    $BODY$
    LANGUAGE plpgsql;
-- For each delete we need to delete associated alert
CREATE TRIGGER remove_event_alert AFTER DELETE ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.remove_event_alert();

-- Delete old trigger and function
DROP TRIGGER decrement_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.decrement_notebook_alert();
-- Delete alert if the forgotten notebook having one
CREATE FUNCTION presences.remove_notebook_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    EXECUTE presences.delete_alert(OLD.id, 'FORGOTTEN_NOTEBOOK', OLD.student_id, OLD.structure_id);
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;
-- For each delete we need to delete associated alert
CREATE TRIGGER remove_notebook_alert AFTER DELETE ON presences.forgotten_notebook
    FOR EACH ROW EXECUTE PROCEDURE presences.remove_notebook_alert();

-- We need to delete associate reason_alert before deleting reason because of primary key
CREATE FUNCTION presences.remove_associate_reason_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    DELETE FROM presences.reason_alert WHERE structure_id = OLD.structure_id AND reason_id = OLD.id;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER remove_associate_reason_alert_before_delete_reason BEFORE DELETE ON presences.reason
    FOR EACH ROW EXECUTE PROCEDURE presences.remove_associate_reason_alert();