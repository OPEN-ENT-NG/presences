-- Return true if absence event is exclude to create alert
CREATE OR REPLACE FUNCTION presences.absence_exclude_alert(event presences.event, structureId varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    alertRulesTypeId bigint;
    noReasonAbsenceExclude boolean;
BEGIN
    SELECT reason_alert_exclude_rules_type_id FROM presences.reason_alert WHERE structure_id = structureId
                                                          AND reason_id = event.reason_id
                                                          AND deleted_at IS NULL
                                                          AND ((event.counsellor_regularisation AND reason_alert_exclude_rules_type_id = 1)
                                                                   OR (NOT event.counsellor_regularisation AND reason_alert_exclude_rules_type_id = 2))
                                                        LIMIT 1 INTO alertRulesTypeId;
    SELECT exclude_alert_absence_no_reason FROM presences.settings WHERE structure_id = structureId INTO noReasonAbsenceExclude;

    RETURN ((event.reason_id IS NULL AND noReasonAbsenceExclude) OR
            (alertRulesTypeId IS NOT NULL AND event.reason_id IS NOT NULL));
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if lateness event is exclude to create alert
CREATE or replace FUNCTION presences.lateness_exclude_alert(event presences.event, structureId varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    alertRulesTypeId bigint;
    noReasonLatenessExclude boolean;
BEGIN
    SELECT reason_alert_exclude_rules_type_id FROM presences.reason_alert WHERE structure_id = structureId
                                                                            AND reason_id = event.reason_id
                                                                            AND deleted_at IS NULL
                                                                            AND reason_alert_exclude_rules_type_id = 3 LIMIT 1 INTO alertRulesTypeId;
    SELECT exclude_alert_lateness_no_reason FROM presences.settings WHERE structure_id = structureId INTO noReasonLatenessExclude;

    RETURN (alertRulesTypeId IS NOT NULL AND event.reason_id IS NOT NULL) OR (event.reason_id IS NULL AND noReasonLatenessExclude);
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if notebook is exclude to create alert
CREATE or replace FUNCTION presences.notebook_exclude_alert(structure_identifier varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_alert_forgotten_notebook FROM presences.settings WHERE structure_id = structure_identifier INTO eventExclude; -- Check if we exclude forgotten notebook

    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;

DROP FUNCTION presences.get_id_absence_event_siblings_in_same_day(presences.event, time, time, boolean);
-- Return the absence event siblings of the event in params, included in the provided time.
-- If no event is found then we return null.
-- absenceEvent     The event with absence type of which we must find the siblings
-- startTime        Siblings must be after start time
-- endTime          Siblings must be before end time
-- structureId      Structure id of the absenceEvent
-- hasAlert        If true, the siblings must be have an associated alert. If false, the siblings must be have no associated alert.
CREATE OR REPLACE FUNCTION presences.get_id_absence_event_siblings_in_same_day(absenceEvent presences.event, startTime time, endTime time, structureId varchar, hasAlert boolean) RETURNS presences.event AS
$BODY$
DECLARE
    eventResult presences.event;
    noReasonAbsenceExclude boolean;
BEGIN
    SELECT exclude_alert_absence_no_reason FROM presences.settings WHERE structure_id = structureId INTO noReasonAbsenceExclude;
    SELECT *
    FROM presences.event
             INNER JOIN presences.register ON (register.id = event.register_id)
             LEFT JOIN presences.alerts as a ON (a.event_id = event.id)
    WHERE event.start_date::date = absenceEvent.start_date::date
      AND event.start_date::time >= startTime
      AND event.start_date::time <= endTime
      AND event.student_id = absenceEvent.student_id
      AND type_id = 1
      AND event.id != absenceEvent.id
      AND ((hasAlert AND a.event_id IS NOT NULL) OR (NOT hasAlert AND a.event_id IS NULL))
    LIMIT 1 -- Only take one
    INTO eventResult;
    RETURN eventResult;
END
$BODY$
    LANGUAGE plpgsql;

DROP FUNCTION presences.get_id_absence_event_siblings(presences.event,character varying,boolean);
-- Return the id of the absence event siblings of the event in params.
-- If no event is found then we return null.
-- absenceEvent     The event with absence type of which we must find the siblings
-- structureId      The structure of the event
-- hasAlert        If true, the siblings must be have an associated alert. If false, the siblings must be have no associated alert.
CREATE or replace FUNCTION presences.get_id_absence_event_siblings(event presences.event, structureId varchar, hasAlert boolean) RETURNS presences.event AS
$BODY$
DECLARE
    recoveryMethod character varying;
    otherAbsence presences.event;
    endOfHalfDay time without time zone;
BEGIN
    SELECT event_recovery_method FROM presences.settings WHERE structure_id = structureId INTO recoveryMethod; -- Get recovery method
    CASE recoveryMethod
        WHEN 'HALF_DAY' THEN
            -- First retrieve half day hour
            SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structureId INTO endOfHalfDay;
            if event.start_date::time < endOfHalfDay THEN -- If we are in morning
                SELECT * FROM presences.get_id_absence_event_siblings_in_same_day(event, '00:00:00'::time, endOfHalfDay, structureId, hasAlert) INTO otherAbsence;
            ELSE -- If we are afternoon
                SELECT * FROM presences.get_id_absence_event_siblings_in_same_day(event, endOfHalfDay,'23:59:59'::time, structureId, hasAlert) INTO otherAbsence;
            END IF;
        WHEN 'DAY' THEN
            -- Check if student already have absence for the day based on provided structure identifier
            SELECT * FROM presences.get_id_absence_event_siblings_in_same_day(event, '00:00:00'::time, '23:59:59'::time, structureId, hasAlert) INTO otherAbsence;
        -- 'HOUR' is a classic case. No other absence can be on the same date.
        ELSE
        END CASE;
    RETURN otherAbsence;
END;
$BODY$
    LANGUAGE plpgsql;