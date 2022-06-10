CREATE FUNCTION presences.delete_alert(eventId bigint, alertType varchar, studentId varchar, structureId varchar) RETURNS void AS
$BODY$
BEGIN
    DELETE FROM presences.alerts WHERE event_id = eventId AND type = alertType AND student_id = studentId AND structure_id = structureId;
    RETURN;
END;
$BODY$
    LANGUAGE plpgsql;


DROP TRIGGER decrement_event_alert ON presences.event;
DROP TRIGGER decrement_event_alert_after_justifying ON presences.event;
DROP FUNCTION presences.decrement_event_alert();
CREATE FUNCTION presences.remove_event_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    structureIdentifier character varying;
    eventIdToReplace bigint;
BEGIN
    CASE OLD.type_id
        WHEN 1 THEN -- Absence creation
            -- Retrieve event structure identifier based on new event register identifier
            SELECT structure_id FROM presences.register WHERE id = OLD.register_id INTO structureIdentifier;

            SELECT presences.get_one_other_absence_in_same_date_which_no_alert(OLD, structureIdentifier, false) INTO eventIdToReplace;
            EXECUTE presences.delete_alert(OLD.id, 'ABSENCE', OLD.student_id, structureIdentifier);
            IF eventIdToReplace IS NOT NULL THEN -- If we have other absence in the same time with no alert, we need a alert for this absence
                EXECUTE presences.create_alert(eventIdToReplace, 'ABSENCE', OLD.student_id, structureIdentifier);
            END IF;
        WHEN 2 THEN -- Lateness creation
            EXECUTE presences.delete_alert(OLD.id, 'LATENESS', OLD.student_id, structureIdentifier);
        END CASE;
        RETURN OLD;
    END
    $BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER remove_event_alert AFTER DELETE ON presences.event
    FOR EACH ROW EXECUTE PROCEDURE presences.remove_event_alert();





DROP TRIGGER decrement_incident_alert ON incidents.protagonist;
DROP FUNCTION incidents.decrement_incident_alert();
CREATE OR REPLACE FUNCTION incidents.delete_incident(incidentId bigint) RETURNS void AS
$BODY$
DECLARE
    incident incidents.incident%rowtype;
    protagonist incidents.protagonist%rowtype;
BEGIN
    SELECT * FROM incidents.incident WHERE id = incidentId INTO incident;
    FOR protagonist IN SELECT * FROM incidents.protagonist WHERE incident_id = incidentId
        -- Before deleting incident, for each protagonist
        LOOP
            -- Delete alert for each protagonist
            EXECUTE presences.delete_alert(incidentId, 'INCIDENT', protagonist.user_id, incident.structure_id);
    END LOOP;
    DELETE FROM incidents.incident WHERE id = incidentId;
END;
$BODY$
    LANGUAGE plpgsql;







DROP TRIGGER decrement_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION presences.decrement_notebook_alert();
CREATE FUNCTION presences.remove_notebook_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    EXECUTE presences.delete_alert(OLD.id, 'FORGOTTEN_NOTEBOOK', OLD.student_id, OLD.structure_id);
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;
CREATE TRIGGER remove_notebook_alert AFTER DELETE ON presences.forgotten_notebook
    FOR EACH ROW EXECUTE PROCEDURE presences.remove_notebook_alert();