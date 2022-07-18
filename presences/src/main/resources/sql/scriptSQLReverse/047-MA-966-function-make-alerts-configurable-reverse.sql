DROP FUNCTION presences.absence_exclude_alert(presences.event, varchar);
DROP FUNCTION presences.lateness_exclude_alert(presences.event, varchar);
DROP FUNCTION incidents.incident_exclude_alert(incidents.incident);
DROP FUNCTION incidents.incident_protagonist_exclude_alert(incidents.protagonist, varchar);
DROP FUNCTION presences.notebook_exclude_alert(varchar);

DROP FUNCTION presences.get_id_absence_event_siblings_in_same_day(presences.event, time, time, varchar, boolean);
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
CREATE or replace FUNCTION presences.get_id_absence_event_siblings(event presences.event, structureId varchar, needAlert boolean) RETURNS bigint AS
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