CREATE OR REPLACE FUNCTION presences.save_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    IF OLD.exceed_date IS NOT NULL THEN
        INSERT INTO presences.alert_history(student_id, structure_id, type, count, modified, exceed_date)
        VALUES (OLD.student_id, OLD.structure_id, OLD.type, OLD.count, OLD.modified, OLD.exceed_date);
    END IF;

    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;