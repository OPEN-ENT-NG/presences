DROP FUNCTION presences.get_id_absence_event_siblings_in_same_day(presences.event, time, time, boolean);
DROP FUNCTION presences.get_id_absence_event_siblings(presences.event, character varying, boolean);
CREATE FUNCTION presences.exists_absence_in_half_day(date date, start_time time, end_time time, student character varying, structure_id character varying) RETURNS boolean AS
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
      AND reason_id IS NULL
      AND type_id = 1
    INTO count_event;

    -- Because our trigger is an AFTER trigger, start count is 1
    RETURN count_event > 0;
END
$BODY$
    LANGUAGE plpgsql;

CREATE FUNCTION presences.exists_absence_in_day(date date, student character varying, structure_identifier character varying) RETURNS boolean AS
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
      AND reason_id IS NULL
      AND type_id = 1
    INTO count_event;

    -- Because our trigger is an AFTER trigger, start count is 1
    RETURN count_event > 0;
END
$BODY$
    LANGUAGE plpgsql;


ALTER TABLE presences.alerts ADD COLUMN count bigint NOT NULL DEFAULT 0;
CREATE FUNCTION presences.update_modified_and_compute_exceed_date() RETURNS TRIGGER AS
$BODY$
DECLARE
    thresholder_field_name character varying;
    thresholder bigint;
BEGIN
    -- Update modified date
    UPDATE presences.alerts SET modified = now() WHERE id = NEW.id;

    SELECT presences.get_thresholder_fieldname(NEW.type) INTO thresholder_field_name;

    -- Retrieve thresholder
    EXECUTE 'SELECT ' || thresholder_field_name || ' FROM presences.settings WHERE structure_id = $1' INTO thresholder USING NEW.structure_id;

    -- If count < thresholder then remove exceed date
    IF NEW.count < thresholder THEN
        UPDATE presences.alerts SET exceed_date = NULL WHERE id = NEW.id;
    END IF;

    -- If count = thresholder then set exceed date
    IF NEW.count = thresholder THEN
        UPDATE presences.alerts SET exceed_date = now() WHERE id = NEW.id;
    END IF;

    -- If count > thresholder do nothing
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_modified_and_compute_exceed_date AFTER UPDATE OF count ON presences.alerts FOR EACH ROW EXECUTE PROCEDURE presences.update_modified_and_compute_exceed_date();


DROP TRIGGER before_each_line_on_delete ON presences.alerts;
DROP TRIGGER after_each_statement_on_delete ON presences.alerts;
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
CREATE TRIGGER save_alert_on_reset AFTER DELETE ON presences.alerts FOR EACH ROW EXECUTE PROCEDURE presences.save_alert();

DROP FUNCTION presences.delete_alert();