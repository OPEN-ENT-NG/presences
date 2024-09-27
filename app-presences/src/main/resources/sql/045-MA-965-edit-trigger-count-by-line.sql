DROP TRIGGER save_alert_on_reset ON presences.alerts;
DROP FUNCTION presences.exists_absence_in_half_day(date, time, time, character varying, character varying);
DROP FUNCTION presences.exists_absence_in_day(date, character varying, character varying);

-- Return the id of the absence event siblings of the event in params, included in the provided time.
-- If no event is found then we return null.
-- absenceEvent     The event with absence type of which we must find the siblings
-- startTime        Siblings must be after start time
-- endTime          Siblings must be before end time
-- needAlert        If true, the siblings must be have an associated alert. If false, the siblings must be have no associated alert.
CREATE FUNCTION presences.get_id_absence_event_siblings_in_same_day(absenceEvent presences.event, startTime time, endTime time, needAlert boolean) RETURNS bigint AS
$BODY$
DECLARE
    eventId bigint;
BEGIN
    SELECT event.id
    FROM presences.event
             INNER JOIN presences.register ON (register.id = event.register_id)
             LEFT JOIN presences.alerts as alert ON (alert.event_id = event.id)
    WHERE event.start_date::date = absenceEvent.start_date::date
      AND event.start_date::time >= startTime
      AND event.start_date::time <= endTime
      AND event.student_id = absenceEvent.student_id
      AND type_id = 1
      AND event.id != absenceEvent.id
      AND ((needAlert AND alert.event_id IS NOT NULL) OR (NOT needAlert AND alert.event_id IS NULL))
    LIMIT 1 -- Only take one
    INTO eventId;

    RETURN eventId;
END;
$BODY$
    LANGUAGE plpgsql;

-- Return the id of the absence event siblings of the event in params.
-- If no event is found then we return null.
-- absenceEvent     The event with absence type of which we must find the siblings
-- structureId      The structure of the event
-- needAlert        If true, the siblings must be have an associated alert. If false, the siblings must be have no associated alert.
CREATE FUNCTION presences.get_id_absence_event_siblings(event presences.event, structureId varchar, needAlert boolean) RETURNS bigint AS
$BODY$
DECLARE
    recoveryMethod character varying;
    absenceOtherId bigint = NULL;
    endOfHalfDay time without time zone;
BEGIN
    SELECT event_recovery_method FROM presences.settings WHERE structure_id = structureId INTO recoveryMethod; -- Get recovery method
    CASE recoveryMethod
        WHEN 'HALF_DAY' THEN
            -- First retrieve half day hour
            SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structureId INTO endOfHalfDay;
            if event.start_date::time < endOfHalfDay THEN -- If we are in morning
                SELECT presences.get_id_absence_event_siblings_in_same_day(event, '00:00:00'::time, endOfHalfDay, needAlert) INTO absenceOtherId;
            ELSE -- If we are afternoon
                SELECT presences.get_id_absence_event_siblings_in_same_day(event, endOfHalfDay,'23:59:59'::time, needAlert) INTO absenceOtherId;
            END IF;
        WHEN 'DAY' THEN
            -- Check if student already have absence for the day based on provided structure identifier
            SELECT presences.get_id_absence_event_siblings_in_same_day(event, '00:00:00'::time, '23:59:59'::time, needAlert) INTO absenceOtherId;
        -- 'HOUR' is a classic case. No other absence can be on the same date.
        ELSE
        END CASE;
    RETURN absenceOtherId;
END;
$BODY$
    LANGUAGE plpgsql;

DROP TRIGGER update_modified_and_compute_exceed_date ON presences.alerts;
DROP FUNCTION presences.update_modified_and_compute_exceed_date();
ALTER TABLE presences.alerts DROP COLUMN count; -- This column this not used anymore

-- Used before deleting alert to save alert into history
CREATE OR REPLACE FUNCTION presences.delete_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
BEGIN
    IF (OLD.delete_at IS NOT NULL) THEN
        RETURN OLD;
    end if;
    UPDATE presences.alerts SET delete_at = now() WHERE id = OLD.id;
    -- Return null cancel delete
    RETURN NULL;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER before_each_line_on_delete BEFORE DELETE ON presences.alerts FOR EACH ROW EXECUTE PROCEDURE presences.delete_alert();

CREATE OR REPLACE FUNCTION presences.save_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    thresholder_field_name character varying;
    thresholder bigint;
    item record;
    alert presences.alerts;
BEGIN
    -- At the level of this code we have already executed delete_alert for each line to be deleted
    FOR item IN SELECT type, structure_id, count(*) as count FROM presences.alerts WHERE delete_at IS NOT NULL GROUP BY type, structure_id LOOP
            SELECT presences.get_thresholder_fieldname(item.type) INTO thresholder_field_name;
            EXECUTE 'SELECT ' || thresholder_field_name || ' FROM presences.settings WHERE structure_id = $1' INTO thresholder USING item.structure_id;
            -- If we have more alerts than the threshold then we save them
            IF (item.count >= thresholder) THEN
                FOR alert IN SELECT * FROM presences.alerts WHERE delete_at IS NOT NULL LOOP
                        INSERT INTO presences.alert_history(student_id, structure_id, type, created, delete_at)
                        VALUES (alert.student_id, alert.structure_id, alert.type, alert.created, now());
                    end loop;
            END IF;
            -- This delete while recall delete_alert
            -- Now that delete_at is defined, the rows will actually be deleted
            DELETE FROM presences.alerts WHERE alerts.delete_at IS NOT NULL;
        end loop;
    RETURN NULL;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER after_each_statement_on_delete AFTER DELETE ON presences.alerts FOR EACH STATEMENT EXECUTE PROCEDURE presences.save_alert();

-- In order we do:
-- 1 delete
-- 2 for each row delete_at is false so we don't delete the line and we set delete_at to now (save_alert_on_delete)
-- 3 after the delete statement we count the number of lines we have delete_at defined and we save the line only if we have exceeded a thresholder (save_alert_on_reset)
-- 4 we delete a other time the same line
-- 5 for each row delete_at is define so we delete the line (save_alert_on_delete)
-- 6 after the delete statement all line where delete_at is define so we do nothing (save_alert_on_reset)