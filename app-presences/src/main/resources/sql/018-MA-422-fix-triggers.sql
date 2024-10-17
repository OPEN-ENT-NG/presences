DROP FUNCTION presences.exists_absence_in_day(date date, student_id character varying, structure_identifier character varying);

CREATE OR REPLACE FUNCTION presences.exists_absence_in_day(date date, student character varying, structure_identifier character varying) RETURNS boolean AS
    $BODY$
    DECLARE
        count_event bigint;
    BEGIN
        SELECT count(event.id) as count
        FROM presences.event
        INNER JOIN presences.register ON (register.id = event.register_id)
        WHERE event.start_date::date = date
        AND register.structure_id = structure_identifier
		AND event.student_id = student
        AND type_id = 1
        INTO count_event;

        -- Because our trigger is an AFTER trigger, start count is 1
        RETURN count_event > 1;
    END
    $BODY$
LANGUAGE plpgsql;

DROP FUNCTION presences.exists_absence_in_half_day(date date, start_time time, end_time time, student_id character varying, structure_id character varying);

CREATE OR REPLACE FUNCTION presences.exists_absence_in_half_day(date date, start_time time, end_time time, student character varying, structure_id character varying) RETURNS boolean AS
    $BODY$
    DECLARE
        count_event bigint;
    BEGIN
        SELECT count(event.id) as count
        FROM presences.event
        INNER JOIN presences.register ON (register.id = event.register_id)
        WHERE event.start_date::date = date
        AND event.start_date::time >= start_time
        AND event.start_date::time <= end_time
		AND event.student_id = student
        AND type_id = 1
        INTO count_event;

        -- Because our trigger is an AFTER trigger, start count is 1
        RETURN count_event > 1;
    END
    $BODY$
LANGUAGE plpgsql;