CREATE OR REPLACE FUNCTION presences.function_delete_events_synchronously(studentId varchar,
                                                                          startdate timestamp without time zone,
                                                                          enddate timestamp without time zone) RETURNS VOID AS
$BODY$
DECLARE
    e presences.event%rowtype;
BEGIN
    FOR e IN SELECT *
             FROM presences.event
             WHERE student_id = studentId
               AND start_date < endDate
               AND end_date > startDate
               AND counsellor_input = true
               AND type_id = 1
        LOOP
            DELETE FROM presences.event WHERE id = e.id;
        END LOOP;

    RETURN;
END
$BODY$
    LANGUAGE plpgsql;