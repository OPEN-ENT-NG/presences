DROP TRIGGER updateReasonToNoReason ON presences.presence_student;
DROP TRIGGER updateReasonToNoReason ON presences.presence;
DROP TRIGGER updateReasonInStructure ON presences.presence_student;
DROP FUNCTION presences.eventHasOtherPresence(bigint, character varying, timestamp without time zone, timestamp without time zone);
DROP FUNCTION presences.updateReasonToNoReasonWhenPresenceStudentDelete();
DROP FUNCTION presences.updateReasonToNoReasonWhenPresenceDelete();
DROP FUNCTION presences.updateReasonToInStructureWhenPresenceStudentCreate();

-- Returns true if the student has another presence for the period provided in the parameter
-- presenceId      id of the presence to ignore
-- studentId       student id
-- startDate       period start date
-- endDate         period end date
CREATE OR REPLACE FUNCTION presences.student_has_other_presence(presenceId bigint, studentId character varying,
    startDate timestamp without time zone, endDate timestamp without time zone) RETURNS BOOLEAN AS
$BODY$
DECLARE
    result presences.presence;
BEGIN
    SELECT * FROM presences.presence AS p INNER JOIN presences.presence_student ps on p.id = ps.presence_id
    WHERE p.id != presenceId AND p.start_date <= endDate AND p.end_date >= startDate AND ps.student_id = studentId
    LIMIT 1 INTO result;
    RETURN result IS NOT NULL;
END
$BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.update_reason_to_no_reason_when_presence_student_delete() RETURNS TRIGGER AS
$BODY$
DECLARE
    presenceResult presences.presence;
BEGIN
    SELECT * FROM presences.presence WHERE id = OLD.presence_id INTO presenceResult;
    -- Update event
    UPDATE presences.event as e SET reason_id = NULL WHERE e.id IN (SELECT id FROM presences.event WHERE student_id = OLD.student_id
        AND start_date <= presenceResult.end_date AND end_date >= presenceResult.start_date AND reason_id = -2)
        AND presences.student_has_other_presence(presenceResult.id, OLD.student_id, e.start_date,
            e.end_date) IS FALSE;
    -- Update absences
    UPDATE presences.absence as a SET reason_id = NULL WHERE a.id IN (SELECT id FROM presences.absence WHERE student_id = OLD.student_id
        AND start_date <= presenceResult.end_date AND end_date >= presenceResult.start_date AND reason_id = -2)
        AND presences.student_has_other_presence(presenceResult.id, OLD.student_id, a.start_date,
            a.end_date) IS FALSE;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_reason_to_no_reason BEFORE DELETE ON presences.presence_student
    FOR EACH ROW EXECUTE PROCEDURE presences.update_reason_to_no_reason_when_presence_student_delete();

CREATE OR REPLACE FUNCTION presences.update_reason_to_no_reason_when_presence_delete() RETURNS TRIGGER AS
$BODY$
DECLARE
    presenceStudentResult presences.presence_student;
BEGIN
    FOR presenceStudentResult IN SELECT * FROM presences.presence_student WHERE presence_id = OLD.id LOOP
            -- Update event
            UPDATE presences.event as e SET reason_id = NULL WHERE e.id IN (SELECT id FROM presences.event WHERE student_id = presenceStudentResult.student_id
                AND start_date <= OLD.end_date AND end_date >= OLD.start_date AND reason_id = -2)
                AND presences.student_has_other_presence(OLD.id, presenceStudentResult.student_id, e.start_date,
                    e.end_date) IS FALSE;
            -- Update absences
            UPDATE presences.absence as a SET reason_id = NULL WHERE a.id IN (SELECT id FROM presences.absence WHERE student_id = presenceStudentResult.student_id
                AND start_date <= OLD.end_date AND end_date >= OLD.start_date AND reason_id = -2)
                AND presences.student_has_other_presence(OLD.id, presenceStudentResult.student_id, a.start_date,
                    a.end_date) IS FALSE;
    END LOOP;
    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_reason_to_no_reason BEFORE DELETE ON presences.presence
    FOR EACH ROW EXECUTE PROCEDURE presences.update_reason_to_no_reason_when_presence_delete();

CREATE OR REPLACE FUNCTION presences.update_reason_to_in_structure_when_presence_student_create() RETURNS TRIGGER AS
$BODY$
DECLARE
    presenceResult presences.presence;
BEGIN
    SELECT * FROM presences.presence WHERE id = NEW.presence_id INTO presenceResult;
    -- Update event
    UPDATE presences.event SET reason_id = -2 WHERE event.id IN (SELECT id FROM presences.event WHERE student_id = NEW.student_id
        AND start_date <= presenceResult.end_date AND end_date >= presenceResult.start_date AND reason_id IS NULL);
    -- Update absences
    UPDATE presences.absence SET reason_id = -2 WHERE absence.id IN (SELECT id FROM presences.absence WHERE student_id = NEW.student_id
        AND start_date <= presenceResult.end_date AND end_date >= presenceResult.start_date AND reason_id IS NULL);
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_reason_in_structure BEFORE INSERT ON presences.presence_student
    FOR EACH ROW EXECUTE PROCEDURE presences.update_reason_to_in_structure_when_presence_student_create();