-- Used before deleting alert to save alert into history
CREATE OR REPLACE FUNCTION presences.save_alert() RETURNS TRIGGER AS
$BODY$
BEGIN
    INSERT INTO presences.alert_history(student_id, structure_id, type, modified)
    VALUES (OLD.student_id, OLD.structure_id, OLD.type, OLD.modified);

    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;