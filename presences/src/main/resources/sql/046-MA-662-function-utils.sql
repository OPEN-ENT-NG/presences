-- Return true if event is exclude
CREATE FUNCTION presences.absence_exclude(event presences.event, structureIdentifier varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
    noReasonAbsenceExclude boolean;
    regularizedExclude boolean;
    noRegularizedExclude boolean;
BEGIN
    SELECT exclude_reason FROM presences.reason WHERE id = event.reason_id INTO eventExclude; -- Check if the reason is exclude
    SELECT exclude_absence_no_reason FROM presences.settings WHERE structure_id = structureIdentifier INTO noReasonAbsenceExclude;
    SELECT exclude_absence_regularized FROM presences.settings WHERE structure_id = structureIdentifier INTO regularizedExclude;
    SELECT exclude_absence_no_regularized FROM presences.settings WHERE structure_id = structureIdentifier INTO noRegularizedExclude;
    RETURN ((eventExclude AND event.reason_id IS NOT NULL) OR (event.reason_id IS NULL AND noReasonAbsenceExclude) OR
            ((event.counsellor_regularisation AND regularizedExclude OR NOT event.counsellor_regularisation AND noRegularizedExclude) AND event.reason_id IS NOT NULL));
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if event is exclude
CREATE FUNCTION presences.lateness_exclude(event presences.event, structureIdentifier varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
    noReasonLatenessExclude boolean;
BEGIN
    SELECT exclude_reason FROM presences.reason WHERE id = event.reason_id INTO eventExclude; -- Check if the reason is exclude
    SELECT exclude_lateness_no_reason FROM presences.settings WHERE structure_id = structureIdentifier INTO noReasonLatenessExclude;

    RETURN (eventExclude AND event.reason_id IS NOT NULL) OR (event.reason_id IS NULL AND noReasonLatenessExclude);
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if incident is exclude
CREATE FUNCTION incidents.incident_exclude(incident incidents.incident) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_seriousness FROM incidents.seriousness as s WHERE s.id = incident.seriousness_id INTO eventExclude;
    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if incident of the protagonist is exclude
CREATE FUNCTION incidents.incident_protagonist_exclude(protagonist incidents.protagonist, structureIdentifier varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_seriousness FROM incidents.seriousness as s INNER JOIN incidents.incident i on s.id = i.seriousness_id
    WHERE protagonist.incident_id = i.id AND s.structure_id = structureIdentifier LIMIT 1 INTO eventExclude;

    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if notebook is exclude
CREATE FUNCTION presences.notebook_exclude(structure_identifier varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_forgotten_notebook FROM presences.settings WHERE structure_id = structure_identifier INTO eventExclude; -- Check if we exclude forgotten notebook

    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;

DROP FUNCTION presences.exists_absence_in_half_day(date, time, time, character varying, character varying);
DROP FUNCTION presences.exists_absence_in_day(date, character varying, character varying);
-- Return id of the event included in the provided date. The event in parma is exclude.
-- If no event is found then we return null
CREATE or replace FUNCTION presences.get_one_other_absence_in_same_date_which_no_alert(absenceEvent presences.event, startTime time, endTime time, structureIdentifier character varying, needAlert boolean) RETURNS bigint AS
$BODY$
DECLARE
    eventId bigint;
    noReasonAbsenceExclude boolean;
    regularizedExclude boolean;
    noRegularizedExclude boolean;
BEGIN
    SELECT exclude_absence_no_reason FROM presences.settings WHERE structure_id = structureIdentifier INTO noReasonAbsenceExclude;
    SELECT exclude_absence_regularized FROM presences.settings WHERE structure_id = structureIdentifier INTO regularizedExclude;
    SELECT exclude_absence_no_regularized FROM presences.settings WHERE structure_id = structureIdentifier INTO noRegularizedExclude;

    SELECT event.id
    FROM presences.event
             INNER JOIN presences.register ON (register.id = event.register_id)
             LEFT JOIN presences.alerts as a ON (a.event_id = event.id)
    WHERE event.start_date::date = absenceEvent.start_date::date
        AND event.start_date::time >= startTime
        AND event.start_date::time <= endTime
        AND event.student_id = absenceEvent.student_id
        AND type_id = 1
        -- Check if event is exclude
        AND ((reason_id IS NULL AND NOT noReasonAbsenceExclude) OR
             reason_id IN (SELECT id FROM presences.reason WHERE reason_type_id = 1 AND exclude_reason = false))
        AND ((NOT regularizedExclude AND counsellor_regularisation = true) OR (NOT noRegularizedExclude AND counsellor_regularisation = false))
        AND event.id != absenceEvent.id
        AND ((needAlert AND a IS NOT NULL) OR (NOT needAlert AND a IS NULL))
        LIMIT 1 -- Only take one
    INTO eventId;

    RETURN eventId;
END;
$BODY$
    LANGUAGE plpgsql;

-- Return id of the event included in the provided date. The event in parma is exclude. The returned event is associated with no alert and must need one alert.
-- If no event is found then we return null
CREATE FUNCTION presences.get_one_other_absence_in_same_date_which_no_alert(event presences.event ,structureIdentifier varchar, needAlert boolean) RETURNS bigint AS
$BODY$
DECLARE
    recoveryMethod character varying;
    eventIdToReplace bigint = NULL;
    endOfHalfDay time without time zone;
BEGIN
    SELECT event_recovery_method FROM presences.settings WHERE structure_id = structureIdentifier INTO recoveryMethod; -- Get recovery method
    CASE recoveryMethod
        WHEN 'HALF_DAY' THEN
            -- First retrieve half day hour
            SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structureIdentifier INTO endOfHalfDay;
            if event.start_date::time < endOfHalfDay THEN -- If we are in morning
                SELECT presences.get_one_other_absence_in_same_date_which_no_alert(event, '00:00:00'::time, endOfHalfDay, structureIdentifier, needAlert) INTO eventIdToReplace;
            ELSE -- If we are afternoon
                SELECT presences.get_one_other_absence_in_same_date_which_no_alert(event, endOfHalfDay,'23:59:59'::time, structureIdentifier, needAlert) INTO eventIdToReplace;
            END IF;
        WHEN 'DAY' THEN
            -- Check if student already have absence for the day based on provided structure identifier
            SELECT presences.get_one_other_absence_in_same_date_which_no_alert(event, '00:00:00'::time, '23:59:59'::time, structureIdentifier, needAlert) INTO eventIdToReplace;
        -- 'HOUR' is a classic case. No other absence can be on the same date.
        ELSE
    END CASE;
    RETURN eventIdToReplace;
END;
$BODY$
    LANGUAGE plpgsql;

DROP TRIGGER update_modified_and_compute_exceed_date ON presences.alerts;
DROP FUNCTION presences.update_modified_and_compute_exceed_date();
ALTER TABLE presences.alerts DROP COLUMN count; -- This column this not used anymore